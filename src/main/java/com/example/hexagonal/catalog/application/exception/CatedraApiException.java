package com.example.hexagonal.catalog.application.exception;

// Excepción lanzada cuando la API de cátedra responde con un error HTTP
public class CatedraApiException extends CatalogException {

    private final int status;
    private final String code;

    public CatedraApiException(int status, String code, String detail) {
        super("La catedra respondio con estado " + status
                + (code != null ? " (" + code + ")" : "")
                + (detail != null ? ": " + detail : ""));
        this.status = status;
        this.code = code;
    }

    public int getStatus() { return status; }

    /** Codigo funcional estable del contrato; puede ser null (401 y 403 de Spring Security, 500, 503). */
    public String getCode() { return code; }
}