package com.example.hexagonal.catalog.infrastructure.persistence.repository;

import com.example.hexagonal.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataWeeklyScheduleRepository extends JpaRepository<WeeklyScheduleEntity, UUID> {
    List<WeeklyScheduleEntity> findByProfessionalId(UUID professionalId);
    void deleteByProfessionalId(UUID professionalId);
}