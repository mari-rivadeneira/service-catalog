package com.example.hexagonal.catalog.application.exception;

// Redis de catedra no disponible (conexion, timeout, permisos). No implica perdida de historial.
public class CatedraRedisException extends CatalogException {

    public CatedraRedisException(String message, Throwable cause) {
        super(message, cause);
    }
}