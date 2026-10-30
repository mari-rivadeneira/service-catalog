package com.example.hexagonal.catalog.domain.port.in;

import com.example.hexagonal.catalog.domain.model.SyncVersion;
import java.util.Optional;

public interface GetSyncStatusUseCase {
    Optional<SyncVersion> getStatus();
}