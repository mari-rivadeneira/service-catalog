package com.example.hexagonal.catalog.infrastructure.security;

import com.example.hexagonal.catalog.domain.port.out.PasswordHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordHasher implements PasswordHasher {

    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    private static final int MIN_MEMORY_KIB = 19_456;
    private static final int MIN_ITERATIONS = 2;

    private final Argon2PasswordEncoder encoder;

    public Argon2PasswordHasher(
            @Value("${password.argon2.memory-kib:19456}") int memoryKib,
            @Value("${password.argon2.iterations:2}") int iterations,
            @Value("${password.argon2.parallelism:1}") int parallelism) {
        if (memoryKib < MIN_MEMORY_KIB || iterations < MIN_ITERATIONS || parallelism < 1) {
            throw new IllegalStateException("Los parametros de Argon2 estan por debajo del minimo recomendado "
                    + "por OWASP (19456 KiB, 2 iteraciones, paralelismo 1)");
        }
        // Orden de los parametros: sal, largo del hash, paralelismo, memoria (KiB), iteraciones
        this.encoder = new Argon2PasswordEncoder(SALT_LENGTH, HASH_LENGTH, parallelism, memoryKib, iterations);
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}