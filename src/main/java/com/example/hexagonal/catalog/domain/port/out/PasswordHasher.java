package com.example.hexagonal.catalog.domain.port.out;

public interface PasswordHasher {

    String hash(String rawPassword);

    // Devuelve false (sin lanzar excepcion) si el hash tiene otro formato.
    boolean matches(String rawPassword, String hash);
}
