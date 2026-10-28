package com.example.hexagonal.catalog.infrastructure.client.catedra.dto;

import java.util.List;

public record CatedraSnapshotResponseDto(
        long version,
        List<CatedraCategoryDto> categories,
        List<CatedraProfessionalDto> professionals,
        List<CatedraWeeklyScheduleDto> schedules
) {}