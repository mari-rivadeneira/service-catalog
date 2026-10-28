package com.example.hexagonal.catalog.domain.port.out;

import com.example.hexagonal.catalog.domain.model.SyncVersion;
import java.util.Optional;

public interface SyncVersionRepository {
    SyncVersion save(SyncVersion syncState);
    Optional<SyncVersion> findByEntityName(String entityName);
}
