package com.example.hexagonal.catalog.infrastructure.persistence;

import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CategoryAndProfessionalJpaAdapterTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @Autowired
    private SyncVersionRepository syncVersionRepository;

    @Test
    @DisplayName("Debe persistir una categoria y recuperarla por ID")
    void shouldSaveAndFindCategory() {
        UUID categoryId = UUID.randomUUID();
        ProfessionalCategory category = new ProfessionalCategory(
                categoryId,
                "Cardiología",
                "Especialidad médica coronaria",
                Instant.now(),
                Instant.now()
        );

        ProfessionalCategory saved = categoryRepository.save(category);
        assertNotNull(saved);

        Optional<ProfessionalCategory> found = categoryRepository.findById(categoryId);
        assertTrue(found.isPresent());
        assertEquals("Cardiología", found.get().getName());
    }

    @Test
    @DisplayName("Debe persistir un profesional vinculado a una categoria existente")
    void shouldSaveAndFindProfessional() {
        // 1. Categoria obligatoria para FK
        UUID categoryId = UUID.randomUUID();
        ProfessionalCategory category = new ProfessionalCategory(
                categoryId,
                "Dermatología",
                "Cuidado de la piel",
                Instant.now(),
                Instant.now()
        );
        categoryRepository.save(category);

        // 2. Profesional
        UUID professionalId = UUID.randomUUID();
        Professional professional = new Professional(
                professionalId,
                "Marina",
                "Rivadeneira",
                "marina.test@example.com",
                "+542611234567",
                categoryId,
                true,
                Instant.now(),
                Instant.now()
        );

        Professional saved = professionalRepository.save(professional);
        assertNotNull(saved);

        // 3. Verificacion
        Optional<Professional> foundByEmail = professionalRepository.findByEmail("marina.test@example.com");
        assertTrue(foundByEmail.isPresent());
        assertEquals("Marina", foundByEmail.get().getFirstName());
        assertEquals(categoryId, foundByEmail.get().getCategoryId());
    }

    @Test
    @DisplayName("Debe persistir y recuperar la agenda semanal de un profesional")
    void shouldSaveAndFindWeeklySchedule() {
        // 1. Categoria y Profesional previos requeridos por la FK
        UUID categoryId = UUID.randomUUID();
        categoryRepository.save(new ProfessionalCategory(
                categoryId, "Odontología", "Salud bucal", Instant.now(), Instant.now()
        ));

        UUID professionalId = UUID.randomUUID();
        professionalRepository.save(new Professional(
                professionalId, "Carlos", "Gómez", "carlos.gomez@example.com",
                "+542619998888", categoryId, true, Instant.now(), Instant.now()
        ));

        // 2. Persistir bloque horario semanal
        UUID scheduleId = UUID.randomUUID();
        WeeklySchedule schedule = new WeeklySchedule(
                scheduleId,
                professionalId,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                30
        );

        WeeklySchedule saved = weeklyScheduleRepository.save(schedule);
        assertNotNull(saved);

        // 3. Recuperar por ID y por profesional
        Optional<WeeklySchedule> found = weeklyScheduleRepository.findById(scheduleId);
        assertTrue(found.isPresent());
        assertEquals(DayOfWeek.MONDAY, found.get().getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), found.get().getStartTime());

        var professionalSchedules = weeklyScheduleRepository.findByProfessionalId(professionalId);
        assertFalse(professionalSchedules.isEmpty());
        assertEquals(1, professionalSchedules.size());
    }

    @Test
    @DisplayName("Debe persistir y actualizar el estado de sincronizacion (SyncVersion)")
    void shouldSaveAndFindSyncVersion() {
        String entityName = "professionals";
        SyncVersion syncVersion = new SyncVersion(
                entityName,
                1L,
                Instant.now()
        );

        SyncVersion saved = syncVersionRepository.save(syncVersion);
        assertNotNull(saved);

        // Recuperar por entity_name
        Optional<SyncVersion> found = syncVersionRepository.findByEntityName(entityName);
        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getVersion());

        // Actualizar version
        found.get().setVersion(2L);
        syncVersionRepository.save(found.get());

        Optional<SyncVersion> updated = syncVersionRepository.findByEntityName(entityName);
        assertTrue(updated.isPresent());
        assertEquals(2L, updated.get().getVersion());
    }
}