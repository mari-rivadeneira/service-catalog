package com.example.hexagonal.catalog.infrastructure.web.dto;

import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;

import java.time.Instant;

public record CategoryResponse(Long id, String name, String description, boolean enabled,
                               Instant createdAt, Instant updatedAt) {

    public static CategoryResponse from(ProfessionalCategory category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription(),
                category.isEnabled(), category.getCreatedAt(), category.getUpdatedAt());
    }
}