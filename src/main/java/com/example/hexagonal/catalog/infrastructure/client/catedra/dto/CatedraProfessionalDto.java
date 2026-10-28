package com.example.hexagonal.catalog.infrastructure.client.catedra.dto;

import java.time.Instant;
import java.util.UUID;

public record CatedraProfessionalDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        UUID categoryId,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}