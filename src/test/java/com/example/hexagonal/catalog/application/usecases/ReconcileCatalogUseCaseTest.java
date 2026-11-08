package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.CatalogHistoryUnavailableException;
import com.example.hexagonal.catalog.application.exception.CatedraRedisException;
import com.example.hexagonal.catalog.domain.model.SyncMode;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.port.in.SyncCatalogIncrementalUseCase;
import com.example.hexagonal.catalog.domain.port.in.SynchronizeCatalogUseCase;
import com.example.hexagonal.catalog.domain.port.out.CatedraRedisClient;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconcileCatalogUseCaseTest {

    @Mock
    private SyncVersionRepository syncVersionRepository;

    @Mock
    private CatedraRedisClient redisClient;

    @Mock
    private SyncCatalogIncrementalUseCase incrementalUseCase;

    @Mock
    private SynchronizeCatalogUseCase snapshotUseCase;

    private ReconcileCatalogUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReconcileCatalogUseCaseImpl(
                syncVersionRepository, redisClient, incrementalUseCase, snapshotUseCase);
    }

    private void localState(long version, SyncMode mode) {
        when(syncVersionRepository.findByEntityName(SyncVersion.CATALOG_KEY))
                .thenReturn(Optional.of(new SyncVersion(SyncVersion.CATALOG_KEY, version, Instant.now(), mode, null)));
    }

    @Test
    @DisplayName("Con la base vacia hace el snapshot inicial sin consultar Redis")
    void shouldTakeSnapshotWhenDatabaseIsEmpty() {
        localState(0L, null);

        useCase.reconcile();

        verify(snapshotUseCase).synchronize();
        verifyNoInteractions(redisClient, incrementalUseCase);
    }

    @Test
    @DisplayName("Sin atraso no hace nada")
    void shouldDoNothingWhenUpToDate() {
        localState(17L, SyncMode.INCREMENTAL);
        when(redisClient.getCurrentVersion()).thenReturn(17L);

        useCase.reconcile();

        verifyNoInteractions(snapshotUseCase, incrementalUseCase);
    }

    @Test
    @DisplayName("Con atraso dispara la sincronizacion incremental hasta la version publicada")
    void shouldSyncIncrementallyWhenBehind() {
        localState(15L, SyncMode.INCREMENTAL);
        when(redisClient.getCurrentVersion()).thenReturn(17L);

        useCase.reconcile();

        verify(incrementalUseCase).sync(17L);
        verifyNoInteractions(snapshotUseCase);
    }

    @Test
    @DisplayName("Si la version publicada es menor a la local, reconstruye con snapshot")
    void shouldTakeSnapshotWhenPublishedVersionIsLowerThanLocal() {
        localState(20L, SyncMode.INCREMENTAL);
        when(redisClient.getCurrentVersion()).thenReturn(17L);

        useCase.reconcile();

        verify(snapshotUseCase).synchronize();
        verifyNoInteractions(incrementalUseCase);
    }

    @Test
    @DisplayName("Si no se puede leer la version publicada, reconstruye con snapshot")
    void shouldTakeSnapshotWhenCurrentVersionCannotBeRead() {
        localState(15L, SyncMode.INCREMENTAL);
        when(redisClient.getCurrentVersion())
                .thenThrow(new CatalogHistoryUnavailableException("falta current-version"));

        useCase.reconcile();

        verify(snapshotUseCase).synchronize();
        verifyNoInteractions(incrementalUseCase);
    }

    @Test
    @DisplayName("Si Redis no responde NO hace snapshot: propaga el error para reintentar")
    void shouldPropagateWhenRedisIsDown() {
        localState(15L, SyncMode.INCREMENTAL);
        when(redisClient.getCurrentVersion())
                .thenThrow(new CatedraRedisException("Redis caido", new RuntimeException()));

        assertThrows(CatedraRedisException.class, () -> useCase.reconcile());

        verifyNoInteractions(snapshotUseCase, incrementalUseCase);
    }
}