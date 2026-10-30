package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;
import com.example.hexagonal.catalog.domain.port.in.ApplyCatalogSnapshotUseCase;
import com.example.hexagonal.catalog.domain.port.in.SynchronizeCatalogUseCase;
import com.example.hexagonal.catalog.domain.port.out.CatedraSnapshotClient;
import org.springframework.stereotype.Service;

@Service
public class SynchronizeCatalogUseCaseImpl implements SynchronizeCatalogUseCase {

    private final CatedraSnapshotClient snapshotClient;
    private final ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase;

    public SynchronizeCatalogUseCaseImpl(
            CatedraSnapshotClient snapshotClient,
            ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase) {
        this.snapshotClient = snapshotClient;
        this.applyCatalogSnapshotUseCase = applyCatalogSnapshotUseCase;
    }

    @Override
    public void synchronize() {
        CatedraSnapshot snapshot = snapshotClient.fetchSnapshot();
        applyCatalogSnapshotUseCase.applySnapshot(snapshot);
    }
}