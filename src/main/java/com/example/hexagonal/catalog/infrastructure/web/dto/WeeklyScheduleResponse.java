package com.example.hexagonal.catalog.infrastructure.web.dto;

import com.example.hexagonal.catalog.domain.model.WeeklySchedule;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

public record WeeklyScheduleResponse(Long id, Long professionalId, DayOfWeek dayOfWeek, LocalTime startTime,
                                     LocalTime endTime, int slotDurationMinutes, boolean enabled,
                                     Instant createdAt, Instant updatedAt) {

    public static WeeklyScheduleResponse from(WeeklySchedule schedule) {
        return new WeeklyScheduleResponse(schedule.getId(), schedule.getProfessionalId(), schedule.getDayOfWeek(),
                schedule.getStartTime(), schedule.getEndTime(), schedule.getSlotDurationMinutes(),
                schedule.isEnabled(), schedule.getCreatedAt(), schedule.getUpdatedAt());
    }
}