package com.example.hexagonal.catalog.infrastructure.config;

import com.example.hexagonal.catalog.application.service.CatalogReconciliationService;
import com.example.hexagonal.catalog.domain.port.in.ReconcileCatalogUseCase;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "catalog.reconciliation.enabled", havingValue = "true")
public class ReconciliationConfig {

    @Bean
    public CatalogReconciliationService catalogReconciliationService(
            ReconcileCatalogUseCase reconcileCatalogUseCase,
            SyncVersionRepository syncVersionRepository,
            @Value("${catalog.reconciliation.max-attempts:4}") int maxAttempts,
            @Value("${catalog.reconciliation.initial-backoff-ms:2000}") long initialBackoffMs,
            @Value("${catalog.reconciliation.max-backoff-ms:30000}") long maxBackoffMs) {
        return new CatalogReconciliationService(
                reconcileCatalogUseCase, syncVersionRepository, maxAttempts, initialBackoffMs, maxBackoffMs);
    }
}
