package com.example.hexagonal.catalog.infrastructure.persistence;

import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.SyncMode;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CategoryAndProfessionalJpaAdapterTest {

    // IDs altos para no chocar con datos reales de un snapshot en la base local
    private static final long CATEGORY_ID = 900_001L;
    private static final long PROFESSIONAL_ID = 900_002L;
    private static final long SCHEDULE_ID = 900_003L;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @Autowired
    private SyncVersionRepository syncVersionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /** Fuerza el SQL real y vacia la memoria de Hibernate para que las lecturas vayan a la base. */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private ProfessionalCategory category(boolean enabled) {
        Instant now = Instant.now();
        return new ProfessionalCategory(CATEGORY_ID, "Cardiología", "Especialidad médica coronaria",
                enabled, now, now);
    }

    private Professional professional() {
        Instant now = Instant.now();
        return new Professional(PROFESSIONAL_ID, CATEGORY_ID, "Marina", "Rivadeneira", true, now, now);
    }

    private WeeklySchedule schedule() {
        Instant now = Instant.now();
        return new WeeklySchedule(SCHEDULE_ID, PROFESSIONAL_ID, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(13, 0), 30, true, now, now);
    }

    @Test
    @DisplayName("Debe persistir una categoria deshabilitada (baja logica) y recuperarla por ID")
    void shouldSaveAndFindCategory() {
        ProfessionalCategory saved = categoryRepository.save(category(false));
        assertNotNull(saved);
        flushAndClear();

        Optional<ProfessionalCategory> found = categoryRepository.findById(CATEGORY_ID);
        assertTrue(found.isPresent());
        assertEquals("Cardiología", found.get().getName());
        assertFalse(found.get().isEnabled());
    }

    @Test
    @DisplayName("Debe persistir un profesional vinculado a una categoria existente")
    void shouldSaveAndFindProfessional() {
        categoryRepository.save(category(true));
        Professional saved = professionalRepository.save(professional());
        assertNotNull(saved);
        flushAndClear();

        Optional<Professional> found = professionalRepository.findById(PROFESSIONAL_ID);
        assertTrue(found.isPresent());
        assertEquals("Marina", found.get().getFirstName());
        assertEquals(CATEGORY_ID, found.get().getCategoryId());
        assertTrue(found.get().isEnabled());

        List<Professional> byCategory = professionalRepository.findByCategoryId(CATEGORY_ID);
        assertEquals(1, byCategory.size());
    }

    @Test
    @DisplayName("Debe persistir y recuperar la agenda semanal de un profesional")
    void shouldSaveAndFindWeeklySchedule() {
        categoryRepository.save(category(true));
        professionalRepository.save(professional());
        WeeklySchedule saved = weeklyScheduleRepository.save(schedule());
        assertNotNull(saved);
        flushAndClear();

        Optional<WeeklySchedule> found = weeklyScheduleRepository.findById(SCHEDULE_ID);
        assertTrue(found.isPresent());
        assertEquals(DayOfWeek.MONDAY, found.get().getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), found.get().getStartTime());
        assertEquals(LocalTime.of(13, 0), found.get().getEndTime());
        assertEquals(30, found.get().getSlotDurationMinutes());
        assertTrue(found.get().isEnabled());

        List<WeeklySchedule> professionalSchedules = weeklyScheduleRepository.findByProfessionalId(PROFESSIONAL_ID);
        assertEquals(1, professionalSchedules.size());
    }

    @Test
    @DisplayName("Debe persistir y actualizar el estado de sincronizacion (SyncVersion)")
    void shouldSaveAndFindSyncVersion() {
        String entityName = "TEST_STATE";
        syncVersionRepository.save(new SyncVersion(entityName, 1L, Instant.now()));
        flushAndClear();

        Optional<SyncVersion> found = syncVersionRepository.findByEntityName(entityName);
        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getVersion());
        assertNull(found.get().getLastMode());

        found.get().applied(2L, SyncMode.INCREMENTAL);
        syncVersionRepository.save(found.get());
        flushAndClear();

        SyncVersion updated = syncVersionRepository.findByEntityName(entityName).orElseThrow();
        assertEquals(2L, updated.getVersion());
        assertEquals(SyncMode.INCREMENTAL, updated.getLastMode());
        assertNull(updated.getLastError());

        updated.failed("Redis no disponible");
        syncVersionRepository.save(updated);
        flushAndClear();

        SyncVersion failed = syncVersionRepository.findByEntityName(entityName).orElseThrow();
        assertEquals(2L, failed.getVersion());
        assertEquals("Redis no disponible", failed.getLastError());
    }

    @Test
    @DisplayName("Debe borrar todo en orden de FK y permitir reinsertar los mismos IDs")
    void shouldDeleteAllAndReinsertSameIds() {
        List<ProfessionalCategory> categories = List.of(category(true));
        List<Professional> professionals = List.of(professional());
        List<WeeklySchedule> schedules = List.of(schedule());

        categoryRepository.saveAll(categories);
        professionalRepository.saveAll(professionals);
        weeklyScheduleRepository.saveAll(schedules);
        flushAndClear();

        weeklyScheduleRepository.deleteAll();
        professionalRepository.deleteAll();
        categoryRepository.deleteAll();

        assertTrue(weeklyScheduleRepository.findByProfessionalId(PROFESSIONAL_ID).isEmpty());
        assertTrue(professionalRepository.findAll().isEmpty());
        assertTrue(categoryRepository.findAll().isEmpty());

        categoryRepository.saveAll(categories);
        professionalRepository.saveAll(professionals);
        weeklyScheduleRepository.saveAll(schedules);
        flushAndClear();

        assertTrue(categoryRepository.findById(CATEGORY_ID).isPresent());
        assertTrue(professionalRepository.findById(PROFESSIONAL_ID).isPresent());
        assertTrue(weeklyScheduleRepository.findById(SCHEDULE_ID).isPresent());
    }

    @Test
    @DisplayName("updateLastError registra y limpia el error sin tocar la version")
    void shouldUpdateLastErrorWithoutTouchingVersion() {
        String entityName = "TEST_STATE_ERROR";
        syncVersionRepository.save(new SyncVersion(entityName, 7L, Instant.now(), SyncMode.INCREMENTAL, null));
        flushAndClear();

        syncVersionRepository.updateLastError(entityName, "Redis no disponible");
        flushAndClear();

        SyncVersion withError = syncVersionRepository.findByEntityName(entityName).orElseThrow();
        assertEquals(7L, withError.getVersion());
        assertEquals(SyncMode.INCREMENTAL, withError.getLastMode());
        assertEquals("Redis no disponible", withError.getLastError());

        syncVersionRepository.updateLastError(entityName, null);
        flushAndClear();

        assertNull(syncVersionRepository.findByEntityName(entityName).orElseThrow().getLastError());
    }
}