package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.CatalogHistoryUnavailableException;
import com.example.hexagonal.catalog.domain.model.CatalogChangeSet;
import com.example.hexagonal.catalog.domain.model.CatalogDelta;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.model.WeeklySchedule;
import com.example.hexagonal.catalog.domain.port.in.ApplyCatalogDeltaUseCase;
import com.example.hexagonal.catalog.domain.port.in.SyncCatalogIncrementalUseCase;
import com.example.hexagonal.catalog.domain.port.in.SynchronizeCatalogUseCase;
import com.example.hexagonal.catalog.domain.port.out.CatedraRedisClient;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquesta la sincronizacion incremental. NO es transaccional: lee Redis sin abrir transacciones
 * y delega cada version a ApplyCatalogDeltaUseCase, que abre la suya.
 */
@Service
public class SyncCatalogIncrementalUseCaseImpl implements SyncCatalogIncrementalUseCase {

    private static final Logger log = LoggerFactory.getLogger(SyncCatalogIncrementalUseCaseImpl.class);

    private final SyncVersionRepository syncVersionRepository;
    private final CatedraRedisClient redisClient;
    private final ApplyCatalogDeltaUseCase applyCatalogDeltaUseCase;
    private final SynchronizeCatalogUseCase snapshotUseCase;

    public SyncCatalogIncrementalUseCaseImpl(
            SyncVersionRepository syncVersionRepository,
            CatedraRedisClient redisClient,
            ApplyCatalogDeltaUseCase applyCatalogDeltaUseCase,
            SynchronizeCatalogUseCase snapshotUseCase) {
        this.syncVersionRepository = syncVersionRepository;
        this.redisClient = redisClient;
        this.applyCatalogDeltaUseCase = applyCatalogDeltaUseCase;
        this.snapshotUseCase = snapshotUseCase;
    }

    @Override
    public void sync(long newVersion) {
        SyncVersion state = syncVersionRepository.findByEntityName(SyncVersion.CATALOG_KEY)
                .orElseGet(SyncVersion::initial);

        // Nunca sincronizado: no hay base sobre la cual aplicar cambios
        if (state.getLastMode() == null) {
            rebuildFromSnapshot("no hay copia local");
            return;
        }

        if (newVersion <= state.getVersion()) {
            log.debug("Version {} ya aplicada (local {}): se ignora", newVersion, state.getVersion());
            return;
        }

        long oldest;
        try {
            oldest = redisClient.getOldestAvailableVersion();
        } catch (CatalogHistoryUnavailableException e) {
            rebuildFromSnapshot("no se puede verificar la ventana de historial: " + e.getMessage());
            return;
        }

        if (state.requiresSnapshot(oldest)) {
            rebuildFromSnapshot("la version local " + state.getVersion()
                    + " quedo fuera de la ventana (oldest=" + oldest + ")");
            return;
        }

        try {
            for (long version = state.getVersion() + 1; version <= newVersion; version++) {
                applyCatalogDeltaUseCase.applyDelta(readDelta(version));
            }
        } catch (CatalogHistoryUnavailableException e) {
            rebuildFromSnapshot("historial incremental no disponible: " + e.getMessage());
        }
    }

    private CatalogDelta readDelta(long version) {
        CatalogChangeSet changes = redisClient.getChanges(version);

        List<ProfessionalCategory> categories = changes.professionalCategoryIds().stream()
                .map(redisClient::getCategory).toList();
        List<Professional> professionals = changes.professionalIds().stream()
                .map(redisClient::getProfessional).toList();
        List<WeeklySchedule> schedules = changes.weeklyScheduleIds().stream()
                .map(redisClient::getWeeklySchedule).toList();

        return new CatalogDelta(version, categories, professionals, schedules);
    }

    private void rebuildFromSnapshot(String reason) {
        log.warn("Se reconstruye el catalogo con snapshot completo: {}", reason);
        snapshotUseCase.synchronize();
    }
}