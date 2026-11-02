package com.example.hexagonal.catalog.domain.model;

import java.time.Instant;

public class Professional {
    private Long id;
    private Long categoryId;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public Professional() {
    }

    public Professional(Long id, Long categoryId, String firstName, String lastName,
                        boolean enabled, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}