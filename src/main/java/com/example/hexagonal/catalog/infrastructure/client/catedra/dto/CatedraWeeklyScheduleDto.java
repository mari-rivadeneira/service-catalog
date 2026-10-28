package com.example.hexagonal.catalog.infrastructure.client.catedra.dto;

import java.time.LocalTime;
import java.util.UUID;

public record CatedraWeeklyScheduleDto(
        UUID id,
        UUID professionalId,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes
) {}