package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.CatalogHistoryUnavailableException;
import com.example.hexagonal.catalog.domain.model.CatalogDelta;
import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.SyncMode;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.in.ApplyCatalogDeltaUseCase;
import com.example.hexagonal.catalog.domain.port.in.ApplyCatalogSnapshotUseCase;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ATENCION: no es @Transactional (necesita las transacciones reales) y reemplaza el contenido de
 * las tablas de catalogo de la base local. Despues de correrlo, volve a sincronizar el snapshot real.
 */
@SpringBootTest
class ApplyCatalogDeltaIntegrationTest {

    private static final long BASE_VERSION = 5L;

    @Autowired
    private ApplyCatalogSnapshotUseCase snapshotUseCase;

    @Autowired
    private ApplyCatalogDeltaUseCase deltaUseCase;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @Autowired
    private SyncVersionRepository syncVersionRepository;

    @BeforeEach
    void baseState() {
        Instant now = Instant.now();
        snapshotUseCase.applySnapshot(new CatedraSnapshot(
                BASE_VERSION,
                List.of(new ProfessionalCategory(1L, "Clinica medica", "Atencion clinica general", true, now, now)),
                List.of(new Professional(15L, 1L, "Ana", "Gomez", true, now, now)),
                List.of(new WeeklySchedule(21L, 15L, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(12, 0), 30, true, now, now))
        ));
    }

    private Professional professional(long id, long categoryId, String lastName, boolean enabled) {
        Instant now = Instant.now();
        return new Professional(id, categoryId, "Ana", lastName, enabled, now, now);
    }

    private long localVersion() {
        return syncVersionRepository.findByEntityName(SyncVersion.CATALOG_KEY).orElseThrow().getVersion();
    }

    @Test
    @DisplayName("Aplica una version: upsert, baja logica, nuevas entidades relacionadas y avance de version")
    void shouldApplyDeltaAndAdvanceVersion() {
        Instant now = Instant.now();
        CatalogDelta delta = new CatalogDelta(
                6L,
                List.of(new ProfessionalCategory(2L, "Pediatria", null, true, now, now)),
                List.of(professional(15L, 1L, "Lopez", false),
                        new Professional(16L, 2L, "Luis", "Perez", true, now, now)),
                List.of()
        );

        deltaUseCase.applyDelta(delta);

        Professional updated = professionalRepository.findById(15L).orElseThrow();
        assertEquals("Lopez", updated.getLastName());
        assertFalse(updated.isEnabled());
        assertTrue(professionalRepository.findById(16L).isPresent());
        assertTrue(categoryRepository.findById(2L).isPresent());
        assertEquals(1, weeklyScheduleRepository.findByProfessionalId(15L).size());

        SyncVersion state = syncVersionRepository.findByEntityName(SyncVersion.CATALOG_KEY).orElseThrow();
        assertEquals(6L, state.getVersion());
        assertEquals(SyncMode.INCREMENTAL, state.getLastMode());
    }

    @Test
    @DisplayName("Aplicar dos veces la misma version no cambia el resultado (idempotencia)")
    void shouldBeIdempotentWhenVersionAlreadyApplied() {
        deltaUseCase.applyDelta(new CatalogDelta(6L, List.of(),
                List.of(professional(15L, 1L, "Lopez", true)), List.of()));
        deltaUseCase.applyDelta(new CatalogDelta(6L, List.of(),
                List.of(professional(15L, 1L, "Otro", true)), List.of()));

        assertEquals("Lopez", professionalRepository.findById(15L).orElseThrow().getLastName());
        assertEquals(6L, localVersion());
    }

    @Test
    @DisplayName("Un hueco de versiones se rechaza con CatalogHistoryUnavailableException")
    void shouldRejectVersionGap() {
        CatalogDelta gap = new CatalogDelta(8L, List.of(),
                List.of(professional(15L, 1L, "Lopez", true)), List.of());

        assertThrows(CatalogHistoryUnavailableException.class, () -> deltaUseCase.applyDelta(gap));

        assertEquals("Gomez", professionalRepository.findById(15L).orElseThrow().getLastName());
        assertEquals(BASE_VERSION, localVersion());
    }

    @Test
    @DisplayName("Una referencia a una categoria inexistente se rechaza y no se guarda nada")
    void shouldRejectMissingReference() {
        CatalogDelta delta = new CatalogDelta(6L, List.of(),
                List.of(professional(16L, 999L, "Perez", true)), List.of());

        assertThrows(CatalogHistoryUnavailableException.class, () -> deltaUseCase.applyDelta(delta));

        assertTrue(professionalRepository.findById(16L).isEmpty());
        assertEquals(BASE_VERSION, localVersion());
    }

    @Test
    @DisplayName("Si la base falla a mitad de la version, se deshace todo y la version no avanza")
    void shouldRollbackWhenDatabaseFailsMidway() {
        Instant now = Instant.now();
        // El profesional se actualiza bien, pero la agenda viola el CHECK start_time < end_time
        CatalogDelta delta = new CatalogDelta(
                6L,
                List.of(),
                List.of(professional(15L, 1L, "Lopez", true)),
                List.of(new WeeklySchedule(21L, 15L, DayOfWeek.MONDAY,
                        LocalTime.of(13, 0), LocalTime.of(9, 0), 30, true, now, now))
        );

        assertThrows(RuntimeException.class, () -> deltaUseCase.applyDelta(delta));

        assertEquals("Gomez", professionalRepository.findById(15L).orElseThrow().getLastName());
        assertEquals(LocalTime.of(9, 0), weeklyScheduleRepository.findById(21L).orElseThrow().getStartTime());
        assertEquals(BASE_VERSION, localVersion());
    }

    @Test
    @DisplayName("Dos eventos concurrentes por la misma version se serializan: una sola aplicacion, sin errores")
    void shouldSerializeConcurrentApplications() throws Exception {
        CatalogDelta delta = new CatalogDelta(6L, List.of(),
                List.of(professional(15L, 1L, "Lopez", true)), List.of());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Void> task = () -> {
            start.await();
            deltaUseCase.applyDelta(delta);
            return null;
        };

        Future<Void> first = pool.submit(task);
        Future<Void> second = pool.submit(task);
        start.countDown();

        first.get(15, TimeUnit.SECONDS);
        second.get(15, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(6L, localVersion());
        assertEquals("Lopez", professionalRepository.findById(15L).orElseThrow().getLastName());
    }
}
