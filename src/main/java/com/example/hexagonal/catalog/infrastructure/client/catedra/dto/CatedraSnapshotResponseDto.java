package com.example.hexagonal.catalog.infrastructure.client.catedra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatedraSnapshotResponseDto(
        Long snapshotVersion,
        Instant generatedAt,
        List<CategoryDto> professionalCategories,
        List<ProfessionalDto> professionals,
        List<WeeklyScheduleDto> weeklySchedules
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategoryDto(Long id, String name, String description, Boolean enabled,
                              Instant createdAt, Instant updatedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProfessionalDto(Long id, Long categoryId, String firstName, String lastName,
                                  Boolean enabled, Instant createdAt, Instant updatedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeeklyScheduleDto(Long id, Long professionalId, String dayOfWeek, LocalTime startTime,
                                    LocalTime endTime, Integer slotDurationMinutes, Boolean enabled,
                                    Instant createdAt, Instant updatedAt) {
    }
}