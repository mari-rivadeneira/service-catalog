package com.example.hexagonal.catalog.domain.port.in;

import com.example.hexagonal.catalog.domain.model.CatedraSnapshot;

public interface ApplyCatalogSnapshotUseCase {
    void applySnapshot(CatedraSnapshot snapshot);
}