package com.example.hexagonal.catalog.infrastructure.web.dto;

import com.example.hexagonal.catalog.domain.model.SyncVersion;

import java.time.Instant;
import java.util.Optional;

/**
 * Estado de la sincronizacion. lastError solo informa el tipo de error (el nombre de la excepcion): el texto
 * completo puede incluir direcciones internas y queda en la base y en el log.
 */
public record SyncStatusResponse(long version, Instant lastSyncedAt, String lastMode, String lastError) {

    public static SyncStatusResponse from(Optional<SyncVersion> status) {
        return status
                .map(state -> new SyncStatusResponse(state.getVersion(), state.getLastSyncedAt(),
                        state.getLastMode() == null ? null : state.getLastMode().name(),
                        summarize(state.getLastError())))
                .orElseGet(() -> new SyncStatusResponse(0L, null, null, null));
    }

    private static String summarize(String lastError) {
        if (lastError == null || lastError.isBlank()) {
            return null;
        }
        int colon = lastError.indexOf(':');
        return (colon > 0 ? lastError.substring(0, colon) : lastError).trim();
    }
}
