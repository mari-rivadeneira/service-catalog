package com.example.hexagonal.catalog.infrastructure.web.dto;

import com.example.hexagonal.catalog.domain.model.Professional;

import java.time.Instant;

public record ProfessionalResponse(Long id, Long categoryId, String firstName, String lastName,
                                   boolean enabled, Instant createdAt, Instant updatedAt) {

    public static ProfessionalResponse from(Professional professional) {
        return new ProfessionalResponse(professional.getId(), professional.getCategoryId(),
                professional.getFirstName(), professional.getLastName(), professional.isEnabled(),
                professional.getCreatedAt(), professional.getUpdatedAt());
    }
}
