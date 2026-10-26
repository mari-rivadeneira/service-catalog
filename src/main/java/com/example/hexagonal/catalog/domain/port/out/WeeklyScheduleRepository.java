package com.example.hexagonal.catalog.domain.port.out;

import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeeklyScheduleRepository {
    WeeklySchedule save(WeeklySchedule weeklySchedule);
    List<WeeklySchedule> saveAll(List<WeeklySchedule> schedules);
    Optional<WeeklySchedule> findById(UUID id);
    List<WeeklySchedule> findByProfessionalId(UUID professionalId);
    void deleteByProfessionalId(UUID professionalId);
}