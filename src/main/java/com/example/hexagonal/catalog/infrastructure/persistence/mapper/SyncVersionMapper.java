package com.example.hexagonal.catalog.infrastructure.persistence.mapper;

import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.SyncVersionEntity;
import org.springframework.stereotype.Component;

@Component
public class SyncVersionMapper {

    public SyncVersion toDomain(SyncVersionEntity entity) {
        if (entity == null) return null;
        return new SyncVersion(
                entity.getEntityName(),
                entity.getVersion(),
                entity.getLastSyncedAt()
        );
    }

    public SyncVersionEntity toEntity(SyncVersion domain) {
        if (domain == null) return null;
        return new SyncVersionEntity(
                domain.getEntityName(),
                domain.getVersion(),
                domain.getLastSyncedAt()
        );
    }
}