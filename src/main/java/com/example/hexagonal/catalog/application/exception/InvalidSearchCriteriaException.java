package com.example.hexagonal.catalog.application.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class InvalidSearchCriteriaException extends CatalogException {

    private final Map<String, String> fieldErrors;

    public InvalidSearchCriteriaException(Map<String, String> fieldErrors) {
        super("Criterios de busqueda invalidos: " + fieldErrors);
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
