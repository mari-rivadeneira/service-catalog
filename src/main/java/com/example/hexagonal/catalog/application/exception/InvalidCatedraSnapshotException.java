package com.example.hexagonal.catalog.application.exception;

// Excepción lanzada cuando el snapshot recibido de cátedra está incompleto o es inconsistente
public class InvalidCatedraSnapshotException extends CatalogException {

    public InvalidCatedraSnapshotException(String message) {
        super(message);
    }
}