package com.example.hexagonal.catalog.infrastructure.persistence.repository;

import com.example.hexagonal.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataWeeklyScheduleRepository extends JpaRepository<WeeklyScheduleEntity, Long> {
    List<WeeklyScheduleEntity> findByProfessionalId(Long professionalId);
}