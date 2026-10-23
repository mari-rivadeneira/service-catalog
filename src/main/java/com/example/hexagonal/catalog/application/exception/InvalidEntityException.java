package com.example.hexagonal.catalog.application.exception;

public class InvalidEntityException extends CatalogException {
    public InvalidEntityException(String message) {
        super(message);
    }
}