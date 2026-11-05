package com.example.hexagonal.catalog.domain.port.out;

import com.example.hexagonal.catalog.domain.model.SyncVersion;
import java.util.Optional;

public interface SyncVersionRepository {
    SyncVersion save(SyncVersion syncState);
    Optional<SyncVersion> findByEntityName(String entityName);

    // Lee el estado con bloqueo de escritura (SELECT ... FOR UPDATE). Debe llamarse dentro de una transaccion.
    Optional<SyncVersion> findByEntityNameForUpdate(String entityName);

    // Registra (o limpia, con null) el ultimo error sin tocar la version ni el resto del estado.
    void updateLastError(String entityName, String lastError);
}