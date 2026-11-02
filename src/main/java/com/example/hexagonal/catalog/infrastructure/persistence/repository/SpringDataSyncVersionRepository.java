package com.example.hexagonal.catalog.infrastructure.persistence.repository;

import com.example.hexagonal.catalog.infrastructure.persistence.entity.SyncVersionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataSyncVersionRepository extends JpaRepository<SyncVersionEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SyncVersionEntity s where s.entityName = :entityName")
    Optional<SyncVersionEntity> findByEntityNameForUpdate(@Param("entityName") String entityName);
}