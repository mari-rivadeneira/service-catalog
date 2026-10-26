package com.example.hexagonal.catalog.infrastructure.persistence.repository;

import com.example.hexagonal.catalog.infrastructure.persistence.entity.SyncVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSyncVersionRepository extends JpaRepository<SyncVersionEntity, String> {
}