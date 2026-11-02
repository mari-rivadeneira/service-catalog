package com.example.hexagonal.catalog.infrastructure.persistence.repository;

import com.example.hexagonal.catalog.infrastructure.persistence.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}