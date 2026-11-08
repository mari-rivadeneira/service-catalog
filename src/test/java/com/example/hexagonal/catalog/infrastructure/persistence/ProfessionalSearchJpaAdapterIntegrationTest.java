package com.example.hexagonal.catalog.infrastructure.persistence;

import com.example.hexagonal.catalog.domain.model.PageResult;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.ProfessionalSearchCriteria;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalSearchRepository;
import com.example.hexagonal.catalog.domain.port.out.WeeklyScheduleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Requiere el compose activo. Todos los datos de prueba usan ids altos y las busquedas filtran por
 * categoria de prueba, asi que no se mezclan con un catalogo real que haya en la base local.
 * Cada test se deshace al terminar (@Transactional).
 */
@SpringBootTest
@Transactional
class ProfessionalSearchJpaAdapterIntegrationTest {

    private static final long C1 = 910_001L;
    private static final long C2 = 910_002L;
    private static final long P1 = 910_101L; // Ana Gomez: habilitada, agenda habilitada el lunes
    private static final long P2 = 910_102L; // Luis Perez: habilitado, solo una agenda DESHABILITADA el martes
    private static final long P3 = 910_103L; // Ana Lopez: deshabilitada, agenda habilitada el lunes
    private static final long P4 = 910_104L; // Marta Diaz: habilitada, categoria 2, sin agendas

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @Autowired
    private ProfessionalSearchRepository searchRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        Instant now = Instant.now();
        categoryRepository.saveAll(List.of(
                new ProfessionalCategory(C1, "Test busqueda 1", null, true, now, now),
                new ProfessionalCategory(C2, "Test busqueda 2", null, true, now, now)));
        professionalRepository.saveAll(List.of(
                new Professional(P1, C1, "Ana", "Gomez", true, now, now),
                new Professional(P2, C1, "Luis", "Perez", true, now, now),
                new Professional(P3, C1, "Ana", "Lopez", false, now, now),
                new Professional(P4, C2, "Marta", "Diaz", true, now, now)));
        weeklyScheduleRepository.saveAll(List.of(
                schedule(910_201L, P1, DayOfWeek.MONDAY, true),
                schedule(910_202L, P2, DayOfWeek.TUESDAY, false),
                schedule(910_203L, P3, DayOfWeek.MONDAY, true)));
        entityManager.flush();
        entityManager.clear();
    }

    private WeeklySchedule schedule(long id, long professionalId, DayOfWeek day, boolean enabled) {
        Instant now = Instant.now();
        return new WeeklySchedule(id, professionalId, day, LocalTime.of(9, 0), LocalTime.of(12, 0), 30,
                enabled, now, now);
    }

    private ProfessionalSearchCriteria criteria(long categoryId, String name, Boolean enabled,
                                                Boolean hasSchedules, DayOfWeek day, int page, int size) {
        return new ProfessionalSearchCriteria(categoryId, name, enabled, hasSchedules, day, page, size);
    }

    private List<Long> ids(PageResult<Professional> result) {
        return result.content().stream().map(Professional::getId).toList();
    }

    @Test
    @DisplayName("Solo con la categoria: ordena por apellido y nombre")
    void shouldListCategoryOrderedByLastNameThenFirstName() {
        PageResult<Professional> result = searchRepository.search(criteria(C1, null, null, null, null, 0, 20));

        assertEquals(List.of(P1, P3, P2), ids(result));
        assertEquals(3, result.totalElements());
    }

    @Test
    @DisplayName("Nombre: parcial y sin distinguir mayusculas")
    void shouldFilterByNameIgnoringCase() {
        PageResult<Professional> result = searchRepository.search(criteria(C1, "ANA", null, null, null, 0, 20));

        assertEquals(List.of(P1, P3), ids(result));
    }

    @Test
    @DisplayName("Nombre completo (nombre y apellido)")
    void shouldFilterByFullName() {
        PageResult<Professional> result = searchRepository.search(criteria(C1, "ana gomez", null, null, null, 0, 20));

        assertEquals(List.of(P1), ids(result));
    }

    @Test
    @DisplayName("El % y el _ se buscan como texto, no como comodines")
    void shouldTreatWildcardsAsLiteralText() {
        assertTrue(searchRepository.search(criteria(C1, "a_a", null, null, null, 0, 20)).content().isEmpty());
        assertTrue(searchRepository.search(criteria(C1, "%", null, null, null, 0, 20)).content().isEmpty());
    }

    @Test
    @DisplayName("Estado habilitado: true y false")
    void shouldFilterByEnabled() {
        assertEquals(List.of(P1, P2), ids(searchRepository.search(criteria(C1, null, true, null, null, 0, 20))));
        assertEquals(List.of(P3), ids(searchRepository.search(criteria(C1, null, false, null, null, 0, 20))));
    }

    @Test
    @DisplayName("Disponibilidad: con y sin agenda habilitada (una agenda deshabilitada no cuenta)")
    void shouldFilterByAvailability() {
        assertEquals(List.of(P1, P3), ids(searchRepository.search(criteria(C1, null, null, true, null, 0, 20))));
        assertEquals(List.of(P2), ids(searchRepository.search(criteria(C1, null, null, false, null, 0, 20))));
    }

    @Test
    @DisplayName("Dia de la semana: exige una agenda habilitada ese dia")
    void shouldFilterByDayOfWeek() {
        assertEquals(List.of(P1, P3),
                ids(searchRepository.search(criteria(C1, null, null, null, DayOfWeek.MONDAY, 0, 20))));
        assertTrue(searchRepository.search(criteria(C1, null, null, null, DayOfWeek.TUESDAY, 0, 20))
                .content().isEmpty());
    }

    @Test
    @DisplayName("Los filtros se combinan")
    void shouldCombineFilters() {
        assertEquals(List.of(P1), ids(searchRepository.search(criteria(C1, null, true, true, null, 0, 20))));
        assertEquals(List.of(P3),
                ids(searchRepository.search(criteria(C1, "ana", false, null, DayOfWeek.MONDAY, 0, 20))));
    }

    @Test
    @DisplayName("Paginacion: paginas, total y cantidad de paginas")
    void shouldPaginate() {
        PageResult<Professional> first = searchRepository.search(criteria(C1, null, null, null, null, 0, 2));
        PageResult<Professional> second = searchRepository.search(criteria(C1, null, null, null, null, 1, 2));
        PageResult<Professional> beyond = searchRepository.search(criteria(C1, null, null, null, null, 2, 2));

        assertEquals(List.of(P1, P3), ids(first));
        assertEquals(3, first.totalElements());
        assertEquals(2, first.totalPages());
        assertEquals(List.of(P2), ids(second));
        assertTrue(beyond.content().isEmpty());
    }

    @Test
    @DisplayName("Categoria: aisla los profesionales de cada una y combina con la disponibilidad")
    void shouldFilterByCategory() {
        assertEquals(List.of(P4), ids(searchRepository.search(criteria(C2, null, null, null, null, 0, 20))));
        assertEquals(List.of(P4), ids(searchRepository.search(criteria(C2, null, null, false, null, 0, 20))));
        assertTrue(searchRepository.search(criteria(C2, null, null, true, null, 0, 20)).content().isEmpty());
    }
}
