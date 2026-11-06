package com.example.hexagonal.catalog.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Argon2PasswordHasherTest {

    private final Argon2PasswordHasher hasher = new Argon2PasswordHasher(19_456, 2, 1);

    @Test
    @DisplayName("El hash es Argon2id, entra en la columna de 255 y verifica la contrasena correcta")
    void shouldHashAndVerify() {
        String hash = hasher.hash("clave-segura");

        assertTrue(hash.startsWith("$argon2id$"));
        assertTrue(hash.length() <= 255);
        assertNotEquals("clave-segura", hash);
        assertTrue(hasher.matches("clave-segura", hash));
        assertFalse(hasher.matches("otra-clave", hash));
    }

    @Test
    @DisplayName("Cada hash lleva una sal distinta")
    void shouldUseDifferentSaltEachTime() {
        assertNotEquals(hasher.hash("clave-segura"), hasher.hash("clave-segura"));
    }

    @Test
    @DisplayName("Admite caracteres no ASCII y contrasenas de 100 caracteres")
    void shouldHandleUnicodeAndLongPasswords() {
        String unicode = "contraseña-ñandú-ü";
        String longPassword = "x".repeat(100);

        assertTrue(hasher.matches(unicode, hasher.hash(unicode)));
        assertTrue(hasher.matches(longPassword, hasher.hash(longPassword)));
    }

    @Test
    @DisplayName("Un hash de otro formato (BCrypt) no se verifica y no lanza excepcion")
    void shouldNotVerifyForeignHashFormat() {
        String bcryptHash = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5BWNHnn6.1LlG3E6uVyK3fOBx6Wu2";

        assertFalse(hasher.matches("clave-segura", bcryptHash));
    }

    @Test
    @DisplayName("Parametros por debajo del minimo de OWASP impiden construir el hasher")
    void shouldRejectWeakParameters() {
        assertThrows(IllegalStateException.class, () -> new Argon2PasswordHasher(1_024, 1, 1));
        assertThrows(IllegalStateException.class, () -> new Argon2PasswordHasher(19_456, 1, 1));
        assertThrows(IllegalStateException.class, () -> new Argon2PasswordHasher(19_456, 2, 0));
    }
}