package com.example.hexagonal.catalog.infrastructure.persistence.mapper;

import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.SyncVersionEntity;
import org.springframework.stereotype.Component;

@Component
public class SyncVersionMapper {

    public SyncVersion toDomain(SyncVersionEntity entity) {
        return new SyncVersion(entity.getEntityName(), entity.getVersion(), entity.getLastSyncedAt(),
                entity.getLastMode(), entity.getLastError());
    }

    public SyncVersionEntity toEntity(SyncVersion syncVersion) {
        SyncVersionEntity entity = new SyncVersionEntity();
        entity.setEntityName(syncVersion.getEntityName());
        entity.setVersion(syncVersion.getVersion());
        entity.setLastSyncedAt(syncVersion.getLastSyncedAt());
        entity.setLastMode(syncVersion.getLastMode());
        entity.setLastError(syncVersion.getLastError());
        return entity;
    }
}