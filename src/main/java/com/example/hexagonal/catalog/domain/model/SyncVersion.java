package com.example.hexagonal.catalog.domain.model;

import java.time.Instant;

public class SyncVersion {

    /** La catedra publica una unica version global: hay una sola fila de estado. */
    public static final String CATALOG_KEY = "CATALOG";

    private String entityName;
    private long version;
    private Instant lastSyncedAt;
    private SyncMode lastMode;
    private String lastError;

    public SyncVersion() {
    }

    public SyncVersion(String entityName, long version, Instant lastSyncedAt) {
        this(entityName, version, lastSyncedAt, null, null);
    }

    public SyncVersion(String entityName, long version, Instant lastSyncedAt,
                       SyncMode lastMode, String lastError) {
        if (version < 0) {
            throw new IllegalArgumentException("La version de sincronizacion no puede ser negativa");
        }
        this.entityName = entityName;
        this.version = version;
        this.lastSyncedAt = lastSyncedAt != null ? lastSyncedAt : Instant.now();
        this.lastMode = lastMode;
        this.lastError = lastError;
    }

    /** Estado inicial: base vacia, nunca sincronizada. */
    public static SyncVersion initial() {
        return new SyncVersion(CATALOG_KEY, 0L, Instant.now(), null, null);
    }

    /** Se aplico correctamente una version incremental: la version no puede retroceder. */
    public void applied(long newVersion, SyncMode mode) {
        if (newVersion < this.version) {
            throw new IllegalArgumentException(
                    "La version no puede retroceder: local=" + this.version + ", nueva=" + newVersion);
        }
        this.version = newVersion;
        this.lastMode = mode;
        this.lastSyncedAt = Instant.now();
        this.lastError = null;
    }

    /** Se reconstruyo la copia completa desde un snapshot: se adopta su version tal cual. */
    public void rebuiltFromSnapshot(long snapshotVersion) {
        if (snapshotVersion < 0) {
            throw new IllegalArgumentException("La version del snapshot no puede ser negativa");
        }
        this.version = snapshotVersion;
        this.lastMode = SyncMode.SNAPSHOT;
        this.lastSyncedAt = Instant.now();
        this.lastError = null;
    }

    /** Fallo un intento: la version NO avanza, solo se registra el error. */
    public void failed(String error) {
        this.lastError = error;
    }

    /** Hay que reconstruir con snapshot si nunca se sincronizo o quedo fuera de la ventana. */
    public boolean requiresSnapshot(long oldestAvailableVersion) {
        return lastMode == null || version < oldestAvailableVersion;
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