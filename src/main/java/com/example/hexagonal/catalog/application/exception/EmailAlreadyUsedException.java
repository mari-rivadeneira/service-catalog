package com.example.hexagonal.catalog.application.exception;

public class EmailAlreadyUsedException extends CatalogException {

    public EmailAlreadyUsedException(String email) {
        super("El email ya esta en uso");
    }
}
