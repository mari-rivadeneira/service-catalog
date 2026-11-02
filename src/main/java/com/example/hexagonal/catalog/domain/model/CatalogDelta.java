package com.example.hexagonal.catalog.domain.model;

import java.util.List;

public record CatalogDelta(
        long version,
        List<ProfessionalCategory> categories,
        List<Professional> professionals,
        List<WeeklySchedule> schedules
) {
    public CatalogDelta {
        categories = List.copyOf(categories);
        professionals = List.copyOf(professionals);
        schedules = List.copyOf(schedules);
    }
}