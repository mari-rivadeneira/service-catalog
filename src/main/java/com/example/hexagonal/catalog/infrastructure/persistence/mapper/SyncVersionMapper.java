package com.example.hexagonal.catalog.infrastructure.persistence.mapper;

import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.SyncStateEntity;
import org.springframework.stereotype.Component;

@Component
public class SyncStateMapper {

    public SyncVersion toDomain(SyncStateEntity entity) {
        if (entity == null) return null;
        return new SyncVersion(
                entity.getEntityName(),
                entity.getVersion(),
                entity.getLastSyncedAt()
        );
    }

    public SyncStateEntity toEntity(SyncVersion domain) {
        if (domain == null) return null;
        return new SyncStateEntity(
                domain.getEntityName(),
                domain.getVersion(),
                domain.getLastSyncedAt()
        );
    }
}