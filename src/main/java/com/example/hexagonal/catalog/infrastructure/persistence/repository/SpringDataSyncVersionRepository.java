package com.example.hexagonal.catalog.infrastructure.persistence.repository;

import com.example.hexagonal.catalog.infrastructure.persistence.entity.SyncStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSyncStateRepository extends JpaRepository<SyncStateEntity, String> {
}