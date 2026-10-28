package com.example.hexagonal.catalog.application.exception;

public class EntityNotFoundException extends CatalogException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}