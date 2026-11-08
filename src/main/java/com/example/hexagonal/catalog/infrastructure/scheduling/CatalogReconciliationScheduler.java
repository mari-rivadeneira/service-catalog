package com.example.hexagonal.catalog.infrastructure.scheduling;

import com.example.hexagonal.catalog.application.service.CatalogReconciliationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "catalog.reconciliation.enabled", havingValue = "true")
public class CatalogReconciliationScheduler {

    private final CatalogReconciliationService reconciliationService;

    public CatalogReconciliationScheduler(CatalogReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    // Al arrancar: snapshot si la base esta vacia; si no, comparacion contra la version publicada.
    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnStartup() {
        reconciliationService.reconcileWithRetries();
    }

    /**
     * Periodica: cubre las notificaciones de Kafka perdidas. fixedDelay evita que dos ejecuciones se pisen;
     * el retardo inicial igual al intervalo evita solaparse con la del arranque.
     */
    @Scheduled(fixedDelayString = "${catalog.reconciliation.interval-ms:300000}",
            initialDelayString = "${catalog.reconciliation.interval-ms:300000}")
    public void reconcilePeriodically() {
        reconciliationService.reconcileWithRetries();
    }
}