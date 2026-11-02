package com.example.hexagonal.catalog.domain.model;

import java.util.List;

public record CatalogChangeSet(
        long version,
        List<Long> professionalCategoryIds,
        List<Long> professionalIds,
        List<Long> weeklyScheduleIds
) {
    public CatalogChangeSet {
        professionalCategoryIds = List.copyOf(professionalCategoryIds);
        professionalIds = List.copyOf(professionalIds);
        weeklyScheduleIds = List.copyOf(weeklyScheduleIds);
    }
}
