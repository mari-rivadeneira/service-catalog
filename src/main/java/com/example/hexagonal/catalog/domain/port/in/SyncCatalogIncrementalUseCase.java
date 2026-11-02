package com.example.hexagonal.catalog.domain.port.in;

public interface SyncCatalogIncrementalUseCase {

    // Lleva la copia local hasta newVersion, de forma incremental o con snapshot si no puede continuar.
    void sync(long newVersion);
}
