package com.example.hexagonal.catalog.infrastructure.persistence.entity;

import com.example.hexagonal.catalog.domain.model.SyncMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sync_state")
public class SyncVersionEntity {

    @Id
    @Column(name = "entity_name", length = 100)
    private String entityName;

    @Column(nullable = false)
    private long version;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_mode", length = 20)
    private SyncMode lastMode;

    @Column(name = "last_error")
    private String lastError;

    public SyncVersionEntity() {
    }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public SyncMode getLastMode() { return lastMode; }
    public void setLastMode(SyncMode lastMode) { this.lastMode = lastMode; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}