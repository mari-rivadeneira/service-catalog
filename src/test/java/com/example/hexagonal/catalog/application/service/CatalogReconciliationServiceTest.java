package com.example.hexagonal.catalog.application.service;

import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.port.in.ReconcileCatalogUseCase;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CatalogReconciliationServiceTest {

    @Mock
    private ReconcileCatalogUseCase reconcileCatalogUseCase;

    @Mock
    private SyncVersionRepository syncVersionRepository;

    // Backoff de 1 ms para que los tests no esperen
    private CatalogReconciliationService service(int maxAttempts) {
        return new CatalogReconciliationService(reconcileCatalogUseCase, syncVersionRepository, maxAttempts, 1L, 2L);
    }

    @Test
    @DisplayName("Si funciona al primer intento no reintenta y limpia el ultimo error")
    void shouldSucceedOnFirstAttempt() {
        assertTrue(service(3).reconcileWithRetries());

        verify(reconcileCatalogUseCase, times(1)).reconcile();
        verify(syncVersionRepository).updateLastError(eq(SyncVersion.CATALOG_KEY), isNull());
    }

    @Test
    @DisplayName("Reintenta tras fallar, registra cada error y al final limpia el error")
    void shouldRetryAndRecoverAfterFailures() {
        doThrow(new RuntimeException("Redis caido"))
                .doThrow(new RuntimeException("Redis caido"))
                .doNothing()
                .when(reconcileCatalogUseCase).reconcile();

        assertTrue(service(4).reconcileWithRetries());

        verify(reconcileCatalogUseCase, times(3)).reconcile();
        verify(syncVersionRepository, times(2)).updateLastError(eq(SyncVersion.CATALOG_KEY), contains("Redis caido"));
        verify(syncVersionRepository).updateLastError(eq(SyncVersion.CATALOG_KEY), isNull());
    }

    @Test
    @DisplayName("Respeta el tope de intentos: no hay ciclos infinitos")
    void shouldGiveUpAfterMaxAttempts() {
        doThrow(new RuntimeException("catedra no responde")).when(reconcileCatalogUseCase).reconcile();

        assertFalse(service(3).reconcileWithRetries());

        verify(reconcileCatalogUseCase, times(3)).reconcile();
        verify(syncVersionRepository, times(3))
                .updateLastError(eq(SyncVersion.CATALOG_KEY), contains("catedra no responde"));
        verify(syncVersionRepository, never()).updateLastError(eq(SyncVersion.CATALOG_KEY), isNull());
    }

    @Test
    @DisplayName("Un fallo al registrar el error no oculta el resultado de la reconciliacion")
    void shouldNotFailWhenRecordingTheErrorFails() {
        doThrow(new IllegalStateException("base caida"))
                .when(syncVersionRepository).updateLastError(anyString(), any());

        assertTrue(service(2).reconcileWithRetries());
    }
}
