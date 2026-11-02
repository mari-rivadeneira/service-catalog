package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.SyncMode;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.in.ApplyCatalogSnapshotUseCase;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ApplyCatalogSnapshotIntegrationTest {

    @Autowired
    private ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @Autowired
    private SyncVersionRepository syncVersionRepository;

    private CatedraSnapshot validSnapshot(long version) {
        Instant now = Instant.now();
        return new CatedraSnapshot(
                version,
                List.of(new ProfessionalCategory(1L, "Clinica medica", "Atencion clinica general", true, now, now)),
                List.of(new Professional(15L, 1L, "Ana", "Gomez", true, now, now)),
                List.of(new WeeklySchedule(21L, 15L, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(12, 0), 30, true, now, now))
        );
    }

    @Test
    @DisplayName("Un snapshot nuevo reemplaza por completo la copia local y registra su version")
    void shouldReplacePreviousCopy() {
        applyCatalogSnapshotUseCase.applySnapshot(validSnapshot(5L));

        Instant now = Instant.now();
        CatedraSnapshot other = new CatedraSnapshot(
                6L,
                List.of(new ProfessionalCategory(2L, "Pediatria", null, true, now, now)),
                List.of(new Professional(16L, 2L, "Luis", "Perez", false, now, now)),
                List.of()
        );
        applyCatalogSnapshotUseCase.applySnapshot(other);

        // Lo del primer snapshot ya no existe
        assertTrue(categoryRepository.findById(1L).isEmpty());
        assertTrue(professionalRepository.findById(15L).isEmpty());
        assertTrue(weeklyScheduleRepository.findByProfessionalId(15L).isEmpty());

        assertTrue(categoryRepository.findById(2L).isPresent());
        assertEquals(false, professionalRepository.findById(16L).orElseThrow().isEnabled());

        SyncVersion state = syncVersionRepository.findByEntityName(SyncVersion.CATALOG_KEY).orElseThrow();
        assertEquals(6L, state.getVersion());
        assertEquals(SyncMode.SNAPSHOT, state.getLastMode());
    }

    @Test
    @DisplayName("Si el guardado falla a mitad, se conservan los datos y la version anteriores (rollback)")
    void shouldRollbackWhenSnapshotFailsMidway() {
        applyCatalogSnapshotUseCase.applySnapshot(validSnapshot(5L));

        Instant now = Instant.now();
        CatedraSnapshot broken = new CatedraSnapshot(
                8L,
                List.of(new ProfessionalCategory(2L, "Pediatria", null, true, now, now)),
                // categoryId 999 no existe: la FK falla al confirmar la transaccion
                List.of(new Professional(16L, 999L, "Luis", "Perez", true, now, now)),
                List.of()
        );

        assertThrows(RuntimeException.class, () -> applyCatalogSnapshotUseCase.applySnapshot(broken));

        assertTrue(categoryRepository.findById(1L).isPresent());
        assertTrue(professionalRepository.findById(15L).isPresent());
        assertEquals(1, weeklyScheduleRepository.findByProfessionalId(15L).size());

        assertTrue(categoryRepository.findById(2L).isEmpty());
        assertTrue(professionalRepository.findById(16L).isEmpty());

        SyncVersion state = syncVersionRepository.findByEntityName(SyncVersion.CATALOG_KEY).orElseThrow();
        assertEquals(5L, state.getVersion());
    }
}