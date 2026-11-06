package com.example.hexagonal.catalog.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.LinkedHashMap;
import java.util.Map;

// Cuerpo comun de todos los errores de la API (application/problem+json con un code estable). */
public final class ProblemBody {

    private ProblemBody() {
    }

    public static Map<String, Object> of(HttpStatusCode status, String code, String detail, String path) {
        return of(status, code, detail, path, null);
    }

    public static Map<String, Object> of(HttpStatusCode status, String code, String detail, String path,
                                         Map<String, String> fieldErrors) {
        HttpStatus resolved = HttpStatus.resolve(status.value());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", resolved != null ? resolved.getReasonPhrase() : "Error");
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("path", path);
        body.put("code", code);
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            body.put("fieldErrors", fieldErrors);
        }
        return body;
    }
}