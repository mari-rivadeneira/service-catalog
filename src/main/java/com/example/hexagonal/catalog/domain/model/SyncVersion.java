package com.example.hexagonal.catalog.domain.model;

import java.time.Instant;

public class SyncVersion {
    private String entityName;
    private long version;
    private Instant lastSyncedAt;

    public SyncVersion() {
    }

    public SyncVersion(String entityName, long version, Instant lastSyncedAt) {
        this.entityName = entityName;
        this.version = version;
        this.lastSyncedAt = lastSyncedAt != null ? lastSyncedAt : Instant.now();
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}