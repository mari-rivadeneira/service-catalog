package com.example.hexagonal.catalog.application.service;

import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.port.in.ReconcileCatalogUseCase;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


 //Ejecuta la reconciliacion con reintentos acotados y espera creciente, y registra el ultimo error.
 //Clase sin anotaciones de Spring: se crea en infrastructure/config/ReconciliationConfig.

public class CatalogReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(CatalogReconciliationService.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final ReconcileCatalogUseCase reconcileCatalogUseCase;
    private final SyncVersionRepository syncVersionRepository;
    private final int maxAttempts;
    private final long initialBackoffMs;
    private final long maxBackoffMs;

    public CatalogReconciliationService(ReconcileCatalogUseCase reconcileCatalogUseCase,
                                        SyncVersionRepository syncVersionRepository,
                                        int maxAttempts,
                                        long initialBackoffMs,
                                        long maxBackoffMs) {
        this.reconcileCatalogUseCase = reconcileCatalogUseCase;
        this.syncVersionRepository = syncVersionRepository;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialBackoffMs = initialBackoffMs;
        this.maxBackoffMs = maxBackoffMs;
    }

    /** @return true si la reconciliacion termino bien; false si se abandono tras agotar los intentos. */
    public boolean reconcileWithRetries() {
        long backoff = initialBackoffMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                reconcileCatalogUseCase.reconcile();
                recordError(null);
                return true;
            } catch (RuntimeException e) {
                log.warn("Reconciliacion fallida (intento {}/{}): {}", attempt, maxAttempts, e.getMessage());
                recordError(describe(e));

                if (attempt == maxAttempts) {
                    break;
                }
                if (!sleep(backoff)) {
                    return false;
                }
                backoff = Math.min(backoff * 2, maxBackoffMs);
            }
        }

        log.error("Reconciliacion abandonada tras {} intentos; se reintentara en la proxima ejecucion programada",
                maxAttempts);
        return false;
    }

    private void recordError(String error) {
        try {
            syncVersionRepository.updateLastError(SyncVersion.CATALOG_KEY, error);
        } catch (RuntimeException e) {
            // Registrar el error nunca debe ocultar el resultado de la reconciliacion
            log.warn("No se pudo registrar el estado de error de la sincronizacion: {}", e.getMessage());
        }
    }

    private String describe(RuntimeException e) {
        String text = e.getClass().getSimpleName() + ": " + e.getMessage();
        return text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) : text;
    }

    private boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}