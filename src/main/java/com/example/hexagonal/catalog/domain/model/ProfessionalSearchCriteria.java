package com.example.hexagonal.catalog.domain.model;

import java.time.DayOfWeek;

/**
 * Filtros de busqueda de profesionales. Los filtros en null no se aplican.
 * hasSchedules: tiene (true) o no tiene (false) al menos una agenda semanal habilitada.
 * dayOfWeek: exige una agenda habilitada ese dia de la semana (implica hasSchedules = true).
 */
public record ProfessionalSearchCriteria(
        Long categoryId,
        String name,
        Boolean enabled,
        Boolean hasSchedules,
        DayOfWeek dayOfWeek,
        int page,
        int size
) {
}