package com.example.hexagonal.catalog.domain.port.out;

public interface ProcessedEventRepository {

    boolean exists(String eventId);

    /** @return true si se registro ahora; false si ya estaba registrado. */
    boolean markProcessed(String eventId);
}