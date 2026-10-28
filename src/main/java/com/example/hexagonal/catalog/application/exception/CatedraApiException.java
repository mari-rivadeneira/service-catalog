package com.example.hexagonal.catalog.application.exception;

// Excepción lanzada cuando la API de cátedra responde con un error HTTP
public class CatedraApiException extends RuntimeException {

    public CatedraApiException(String message) {
        super(message);
    }
}
