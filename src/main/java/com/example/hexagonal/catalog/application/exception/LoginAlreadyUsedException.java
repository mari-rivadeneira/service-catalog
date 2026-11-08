package com.example.hexagonal.catalog.application.exception;

public class LoginAlreadyUsedException extends CatalogException {

    public LoginAlreadyUsedException(String login) {
        super("El login ya esta en uso");
    }
}
