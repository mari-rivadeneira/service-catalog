package com.example.hexagonal.catalog.domain.port.in;

public interface ProcessCatalogUpdatedEventUseCase {

    void process(String eventId, long newVersion);
}
