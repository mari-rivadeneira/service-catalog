package com.example.hexagonal.catalog.domain.port.out;

import com.example.hexagonal.catalog.domain.model.WeeklySchedule;

import java.util.List;
import java.util.Optional;

public interface WeeklyScheduleRepository {
    WeeklySchedule save(WeeklySchedule weeklySchedule);
    List<WeeklySchedule> saveAll(List<WeeklySchedule> schedules);
    Optional<WeeklySchedule> findById(Long id);
    List<WeeklySchedule> findByProfessionalId(Long professionalId);
    void deleteAll();
}