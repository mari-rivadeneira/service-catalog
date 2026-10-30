package com.example.hexagonal.catalog.application.service;

import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.port.in.GetSyncStatusUseCase;
import com.example.hexagonal.catalog.domain.port.in.SynchronizeCatalogUseCase;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CatalogSyncService {

    private final SynchronizeCatalogUseCase synchronizeCatalogUseCase;
    private final GetSyncStatusUseCase getSyncStatusUseCase;

    public CatalogSyncService(
            SynchronizeCatalogUseCase synchronizeCatalogUseCase,
            GetSyncStatusUseCase getSyncStatusUseCase) {
        this.synchronizeCatalogUseCase = synchronizeCatalogUseCase;
        this.getSyncStatusUseCase = getSyncStatusUseCase;
    }

    public void triggerSnapshotSync() {
        synchronizeCatalogUseCase.synchronize();
    }

    public Optional<SyncVersion> checkCurrentStatus() {
        return getSyncStatusUseCase.getStatus();
    }
}