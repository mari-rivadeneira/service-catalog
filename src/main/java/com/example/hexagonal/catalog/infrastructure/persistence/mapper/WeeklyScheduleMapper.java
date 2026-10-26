package com.example.hexagonal.catalog.infrastructure.persistence.mapper;

import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;

@Component
public class WeeklyScheduleMapper {

    public WeeklySchedule toDomain(WeeklyScheduleEntity entity) {
        if (entity == null) return null;
        return new WeeklySchedule(
                entity.getId(),
                entity.getProfessional() != null ? entity.getProfessional().getId() : null,
                entity.getDayOfWeek() != null ? DayOfWeek.valueOf(entity.getDayOfWeek()) : null,
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getSlotDurationMinutes()
        );
    }

    public WeeklyScheduleEntity toEntity(WeeklySchedule domain, ProfessionalEntity professionalEntity) {
        if (domain == null) return null;
        return new WeeklyScheduleEntity(
                domain.getId(),
                professionalEntity,
                domain.getDayOfWeek() != null ? domain.getDayOfWeek().name() : null,
                domain.getStartTime(),
                domain.getEndTime(),
                domain.getSlotDurationMinutes()
        );
    }
}