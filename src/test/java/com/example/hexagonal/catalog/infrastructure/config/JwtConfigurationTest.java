package com.example.hexagonal.catalog.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtConfigurationTest {

    private final JwtConfiguration configuration = new JwtConfiguration();

    @Test
    @DisplayName("Sin secreto, o con uno en blanco, la aplicacion no arranca")
    void shouldRejectMissingSecret() {
        assertThrows(IllegalStateException.class, () -> configuration.jwtSecretKey(null));
        assertThrows(IllegalStateException.class, () -> configuration.jwtSecretKey("  "));
    }

    @Test
    @DisplayName("Un secreto que no es Base64 se rechaza")
    void shouldRejectNonBase64Secret() {
        assertThrows(IllegalStateException.class, () -> configuration.jwtSecretKey("esto no es base64 !!!"));
    }

    @Test
    @DisplayName("Un secreto de menos de 64 bytes se rechaza")
    void shouldRejectShortSecret() {
        String shortSecret = Base64.getEncoder().encodeToString(new byte[32]);

        assertThrows(IllegalStateException.class, () -> configuration.jwtSecretKey(shortSecret));
    }

    @Test
    @DisplayName("Un secreto de 64 bytes en Base64 se acepta")
    void shouldAcceptValidSecret() {
        String secret = Base64.getEncoder().encodeToString(new byte[64]);

        SecretKey key = configuration.jwtSecretKey(secret);

        assertEquals("HmacSHA512", key.getAlgorithm());
        assertEquals(64, key.getEncoded().length);
    }
}
