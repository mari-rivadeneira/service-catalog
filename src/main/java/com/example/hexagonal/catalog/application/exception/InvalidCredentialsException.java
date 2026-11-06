package com.example.hexagonal.catalog.application.exception;

public class InvalidCredentialsException extends CatalogException {

    public InvalidCredentialsException() {
        super("Credenciales invalidas");
    }
}
