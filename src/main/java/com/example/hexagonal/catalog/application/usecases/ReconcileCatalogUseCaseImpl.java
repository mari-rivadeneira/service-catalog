package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.CatalogHistoryUnavailableException;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.port.in.ReconcileCatalogUseCase;
import com.example.hexagonal.catalog.domain.port.in.SyncCatalogIncrementalUseCase;
import com.example.hexagonal.catalog.domain.port.in.SynchronizeCatalogUseCase;
import com.example.hexagonal.catalog.domain.port.out.CatedraRedisClient;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReconcileCatalogUseCaseImpl implements ReconcileCatalogUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReconcileCatalogUseCaseImpl.class);

    private final SyncVersionRepository syncVersionRepository;
    private final CatedraRedisClient redisClient;
    private final SyncCatalogIncrementalUseCase incrementalUseCase;
    private final SynchronizeCatalogUseCase snapshotUseCase;

    public ReconcileCatalogUseCaseImpl(
            SyncVersionRepository syncVersionRepository,
            CatedraRedisClient redisClient,
            SyncCatalogIncrementalUseCase incrementalUseCase,
            SynchronizeCatalogUseCase snapshotUseCase) {
        this.syncVersionRepository = syncVersionRepository;
        this.redisClient = redisClient;
        this.incrementalUseCase = incrementalUseCase;
        this.snapshotUseCase = snapshotUseCase;
    }

    @Override
    public void reconcile() {
        SyncVersion state = syncVersionRepository.findByEntityName(SyncVersion.CATALOG_KEY)
                .orElseGet(SyncVersion::initial);

        if (state.getLastMode() == null) {
            log.info("Reconciliacion: no hay copia local, se hace el snapshot inicial");
            snapshotUseCase.synchronize();
            return;
        }

        long published;
        try {
            published = redisClient.getCurrentVersion();
        } catch (CatalogHistoryUnavailableException e) {
            log.warn("Reconciliacion: no se puede verificar la version publicada ({}), se reconstruye con snapshot",
                    e.getMessage());
            snapshotUseCase.synchronize();
            return;
        }

        if (published < state.getVersion()) {
            log.warn("Reconciliacion: la version publicada ({}) es menor a la local ({}), se reconstruye con snapshot",
                    published, state.getVersion());
            snapshotUseCase.synchronize();
            return;
        }

        if (published == state.getVersion()) {
            log.debug("Reconciliacion: sin atraso (version {})", published);
            return;
        }

        log.info("Reconciliacion: atraso detectado (local {}, publicada {}), se sincroniza",
                state.getVersion(), published);
        incrementalUseCase.sync(published);
    }
}