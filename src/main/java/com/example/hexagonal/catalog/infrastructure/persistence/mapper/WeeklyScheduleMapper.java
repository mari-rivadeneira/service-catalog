package com.example.hexagonal.catalog.infrastructure.persistence.mapper;

import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import org.springframework.stereotype.Component;

@Component
public class WeeklyScheduleMapper {

    public WeeklySchedule toDomain(WeeklyScheduleEntity entity) {
        return new WeeklySchedule(entity.getId(), entity.getProfessionalId(), entity.getDayOfWeek(),
                entity.getStartTime(), entity.getEndTime(), entity.getSlotDurationMinutes(),
                entity.isEnabled(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public WeeklyScheduleEntity toEntity(WeeklySchedule schedule) {
        WeeklyScheduleEntity entity = new WeeklyScheduleEntity();
        entity.setId(schedule.getId());
        entity.setProfessionalId(schedule.getProfessionalId());
        entity.setDayOfWeek(schedule.getDayOfWeek());
        entity.setStartTime(schedule.getStartTime());
        entity.setEndTime(schedule.getEndTime());
        entity.setSlotDurationMinutes(schedule.getSlotDurationMinutes());
        entity.setEnabled(schedule.isEnabled());
        entity.setCreatedAt(schedule.getCreatedAt());
        entity.setUpdatedAt(schedule.getUpdatedAt());
        return entity;
    }
}