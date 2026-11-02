package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.CatalogHistoryUnavailableException;
import com.example.hexagonal.catalog.application.exception.CatedraRedisException;
import com.example.hexagonal.catalog.domain.model.CatalogChangeSet;
import com.example.hexagonal.catalog.domain.model.CatalogDelta;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.SyncMode;
import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.port.in.ApplyCatalogDeltaUseCase;
import com.example.hexagonal.catalog.domain.port.in.SynchronizeCatalogUseCase;
import com.example.hexagonal.catalog.domain.port.out.CatedraRedisClient;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncCatalogIncrementalUseCaseTest {

    @Mock
    private SyncVersionRepository syncVersionRepository;

    @Mock
    private CatedraRedisClient redisClient;

    @Mock
    private ApplyCatalogDeltaUseCase applyCatalogDeltaUseCase;

    @Mock
    private SynchronizeCatalogUseCase snapshotUseCase;

    private SyncCatalogIncrementalUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SyncCatalogIncrementalUseCaseImpl(
                syncVersionRepository, redisClient, applyCatalogDeltaUseCase, snapshotUseCase);
    }

    private void localState(long version, SyncMode mode) {
        when(syncVersionRepository.findByEntityName(SyncVersion.CATALOG_KEY))
                .thenReturn(Optional.of(new SyncVersion(SyncVersion.CATALOG_KEY, version, Instant.now(), mode, null)));
    }

    private CatalogChangeSet changes(long version, List<Long> professionalIds) {
        return new CatalogChangeSet(version, List.of(), professionalIds, List.of());
    }

    @Test
    @DisplayName("Ignora una version menor o igual a la local")
    void shouldIgnoreAlreadyAppliedVersion() {
        localState(7L, SyncMode.INCREMENTAL);

        useCase.sync(7L);
        useCase.sync(5L);

        verifyNoInteractions(redisClient, applyCatalogDeltaUseCase, snapshotUseCase);
    }

    @Test
    @DisplayName("Si nunca se sincronizo, reconstruye con snapshot sin consultar Redis")
    void shouldRebuildWhenNeverSynced() {
        localState(0L, null);

        useCase.sync(7L);

        verify(snapshotUseCase).synchronize();
        verifyNoInteractions(redisClient, applyCatalogDeltaUseCase);
    }

    @Test
    @DisplayName("Version perdida (local 3, oldest 4, current 7): reconstruye con snapshot")
    void shouldRebuildWhenLocalVersionIsOutsideWindow() {
        localState(3L, SyncMode.INCREMENTAL);
        when(redisClient.getOldestAvailableVersion()).thenReturn(4L);

        useCase.sync(7L);

        verify(snapshotUseCase).synchronize();
        verify(redisClient, never()).getChanges(any(Long.class));
        verifyNoInteractions(applyCatalogDeltaUseCase);
    }

    @Test
    @DisplayName("Si no se puede leer la ventana de historial, reconstruye con snapshot")
    void shouldRebuildWhenOldestVersionIsMissing() {
        localState(5L, SyncMode.INCREMENTAL);
        when(redisClient.getOldestAvailableVersion())
                .thenThrow(new CatalogHistoryUnavailableException("falta oldest-available-version"));

        useCase.sync(7L);

        verify(snapshotUseCase).synchronize();
        verifyNoInteractions(applyCatalogDeltaUseCase);
    }

    @Test
    @DisplayName("Rango continuo (local 5, new 7): aplica las versiones 6 y 7 en orden")
    void shouldApplyContinuousRangeInOrder() {
        localState(5L, SyncMode.INCREMENTAL);
        when(redisClient.getOldestAvailableVersion()).thenReturn(4L);
        Professional professional = new Professional(15L, 1L, "Ana", "Gomez", true, Instant.now(), Instant.now());
        when(redisClient.getChanges(6L)).thenReturn(changes(6L, List.of(15L)));
        when(redisClient.getChanges(7L)).thenReturn(changes(7L, List.of()));
        when(redisClient.getProfessional(15L)).thenReturn(professional);

        useCase.sync(7L);

        ArgumentCaptor<CatalogDelta> captor = ArgumentCaptor.forClass(CatalogDelta.class);
        verify(applyCatalogDeltaUseCase, times(2)).applyDelta(captor.capture());
        assertEquals(6L, captor.getAllValues().get(0).version());
        assertEquals(1, captor.getAllValues().get(0).professionals().size());
        assertEquals(7L, captor.getAllValues().get(1).version());
        verifyNoInteractions(snapshotUseCase);
    }

    @Test
    @DisplayName("Si falta el historial a mitad de camino, aplica lo que hay y reconstruye con snapshot")
    void shouldRebuildWhenHistoryIsMissingMidway() {
        localState(5L, SyncMode.INCREMENTAL);
        when(redisClient.getOldestAvailableVersion()).thenReturn(4L);
        when(redisClient.getChanges(6L)).thenReturn(changes(6L, List.of()));
        when(redisClient.getChanges(7L)).thenThrow(new CatalogHistoryUnavailableException("falta changes:7"));

        useCase.sync(7L);

        verify(applyCatalogDeltaUseCase, times(1)).applyDelta(any());
        verify(snapshotUseCase).synchronize();
    }

    @Test
    @DisplayName("Si al aplicar se detecta un hueco o referencia invalida, reconstruye con snapshot")
    void shouldRebuildWhenApplyDetectsInconsistency() {
        localState(5L, SyncMode.INCREMENTAL);
        when(redisClient.getOldestAvailableVersion()).thenReturn(4L);
        when(redisClient.getChanges(6L)).thenReturn(changes(6L, List.of()));
        doThrow(new CatalogHistoryUnavailableException("hueco")).when(applyCatalogDeltaUseCase).applyDelta(any());

        useCase.sync(6L);

        verify(snapshotUseCase).synchronize();
    }

    @Test
    @DisplayName("Si Redis no responde NO hace snapshot: propaga el error para reintentar")
    void shouldPropagateWhenRedisIsDown() {
        localState(5L, SyncMode.INCREMENTAL);
        when(redisClient.getOldestAvailableVersion())
                .thenThrow(new CatedraRedisException("Redis caido", new RuntimeException()));

        assertThrows(CatedraRedisException.class, () -> useCase.sync(7L));

        verifyNoInteractions(snapshotUseCase, applyCatalogDeltaUseCase);
    }

    @Test
    @DisplayName("Si falla la aplicacion de una version, no sigue con las siguientes ni hace snapshot")
    void shouldStopWhenApplyFails() {
        localState(5L, SyncMode.INCREMENTAL);
        when(redisClient.getOldestAvailableVersion()).thenReturn(4L);
        when(redisClient.getChanges(6L)).thenReturn(changes(6L, List.of()));
        doThrow(new IllegalStateException("fallo de base")).when(applyCatalogDeltaUseCase).applyDelta(any());

        assertThrows(IllegalStateException.class, () -> useCase.sync(7L));

        verify(redisClient, never()).getChanges(7L);
        verifyNoInteractions(snapshotUseCase);
    }
}