package com.example.hexagonal.catalog.infrastructure.web.exception;

import com.example.hexagonal.catalog.application.exception.EmailAlreadyUsedException;
import com.example.hexagonal.catalog.application.exception.EntityNotFoundException;
import com.example.hexagonal.catalog.application.exception.InvalidCredentialsException;
import com.example.hexagonal.catalog.application.exception.InvalidSearchCriteriaException;
import com.example.hexagonal.catalog.application.exception.InvalidUserDataException;
import com.example.hexagonal.catalog.application.exception.LoginAlreadyUsedException;
import com.example.hexagonal.catalog.infrastructure.web.ProblemBody;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidUserDataException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidUserData(InvalidUserDataException ex,
                                                                     HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Los datos enviados no son validos",
                request, ex.getFieldErrors());
    }

    @ExceptionHandler(InvalidSearchCriteriaException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidSearchCriteria(InvalidSearchCriteriaException ex,
                                                                           HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Los criterios de busqueda no son validos",
                request, ex.getFieldErrors());
    }

    @ExceptionHandler(LoginAlreadyUsedException.class)
    public ResponseEntity<Map<String, Object>> handleLoginAlreadyUsed(LoginAlreadyUsedException ex,
                                                                      HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "LOGIN_ALREADY_USED", "El login ya esta en uso", request, null);
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyUsed(EmailAlreadyUsedException ex,
                                                                      HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "EMAIL_ALREADY_USED", "El email ya esta en uso", request, null);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex,
                                                                        HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Credenciales invalidas", request, null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex,
                                                              HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                                    HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "El cuerpo de la peticion no es un JSON valido",
                request, null);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(TypeMismatchException ex,
                                                                  HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "El valor de un parametro no tiene el formato esperado", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                        HttpServletRequest request) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON);
        Set<HttpMethod> supported = ex.getSupportedHttpMethods();
        if (supported != null && !supported.isEmpty()) {
            builder.allow(supported.toArray(new HttpMethod[0]));
        }
        return builder.body(ProblemBody.of(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "El metodo HTTP no esta permitido para este recurso", request.getRequestURI()));
    }

    // Se lanzan dentro de un controller (seguridad por metodo): sin estos handlers el generico daria 500
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex,
                                                                  HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "FORBIDDEN", "No tiene permisos para realizar esta operacion",
                request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex,
                                                                    HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Se requiere autenticacion valida para acceder a este recurso", request, null);
    }

    /** Errores estandar de Spring (ruta inexistente, tipo de contenido, etc.) con su estado; el resto es un 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            return respond(status, codeFor(status.value()), detailFor(status.value()), request, null);
        }
        log.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocurrio un error inesperado",
                request, null);
    }

    private ResponseEntity<Map<String, Object>> respond(HttpStatusCode status, String code, String detail,
                                                        HttpServletRequest request,
                                                        Map<String, String> fieldErrors) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemBody.of(status, code, detail, request.getRequestURI(), fieldErrors));
    }

    private static String codeFor(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> status >= 500 ? "INTERNAL_ERROR" : "ERROR";
        };
    }

    private static String detailFor(int status) {
        return switch (status) {
            case 400 -> "La peticion es invalida";
            case 404 -> "El recurso solicitado no existe";
            case 405 -> "El metodo HTTP no esta permitido para este recurso";
            case 406 -> "El formato de respuesta solicitado no esta disponible";
            case 415 -> "El tipo de contenido no esta soportado";
            default -> status >= 500 ? "Ocurrio un error inesperado" : "No se pudo procesar la peticion";
        };
    }
}
