package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.port.in.GetSyncStatusUseCase;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetSyncStatusUseCaseImpl implements GetSyncStatusUseCase {

    private static final String CATALOG_ENTITY_NAME = "CATALOG";
    private final SyncVersionRepository syncVersionRepository;

    public GetSyncStatusUseCaseImpl(SyncVersionRepository syncVersionRepository) {
        this.syncVersionRepository = syncVersionRepository;
    }

    @Override
    public Optional<SyncVersion> getStatus() {
        return syncVersionRepository.findByEntityName(CATALOG_ENTITY_NAME);
    }
}