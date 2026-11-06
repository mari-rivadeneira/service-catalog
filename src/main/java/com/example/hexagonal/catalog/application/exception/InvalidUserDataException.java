package com.example.hexagonal.catalog.application.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class InvalidUserDataException extends CatalogException {

    private final Map<String, String> fieldErrors;

    public InvalidUserDataException(Map<String, String> fieldErrors) {
        super("Datos de usuario invalidos: " + fieldErrors);
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}