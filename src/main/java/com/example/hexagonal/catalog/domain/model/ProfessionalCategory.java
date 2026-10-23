package com.example.hexagonal.catalog.domain.model;

import java.time.Instant;
import java.util.UUID;

public class ProfessionalCategory {
    private UUID id;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public ProfessionalCategory() {
    }

    public ProfessionalCategory(UUID id, String name, String description, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
