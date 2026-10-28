package com.example.hexagonal.catalog.infrastructure.client.catedra.dto;

import java.time.Instant;
import java.util.UUID;

public record CatedraCategoryDto(
        UUID id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
