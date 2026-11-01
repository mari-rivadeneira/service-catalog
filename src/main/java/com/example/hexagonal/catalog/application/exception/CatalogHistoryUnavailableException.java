package com.example.hexagonal.catalog.application.exception;

// Señal de discontinuidad: falta una clave o un id en Redis, o el contenido no se puede interpretar.
// La sincronizacion incremental debe recuperarse con un snapshot completo.
public class CatalogHistoryUnavailableException extends CatalogException {

    public CatalogHistoryUnavailableException(String message) {
        super(message);
    }

    public CatalogHistoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
