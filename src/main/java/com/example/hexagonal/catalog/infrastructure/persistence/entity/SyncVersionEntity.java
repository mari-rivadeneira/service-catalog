package com.example.hexagonal.catalog.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sync_state")
public class SyncStateEntity {

    @Id
    @Column(name = "entity_name", length = 100)
    private String entityName;

    @Column(nullable = false)
    private Long version;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    public SyncStateEntity() {}

    public SyncStateEntity(String entityName, Long version, Instant lastSyncedAt) {
        this.entityName = entityName;
        this.version = version;
        this.lastSyncedAt = lastSyncedAt;
    }

    // Getters y Setters
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}