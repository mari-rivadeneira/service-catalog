package com.example.hexagonal.catalog.domain.port.in;

public interface ReconcileCatalogUseCase {

    /**
     * Un intento de reconciliacion: compara la copia local con la version publicada por la catedra
     * y sincroniza si hay atraso. Sin reintentos: de eso se ocupa CatalogReconciliationService.
     */
    void reconcile();
}