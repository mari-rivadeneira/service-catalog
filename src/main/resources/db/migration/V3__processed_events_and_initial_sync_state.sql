-- Eventos de Kafka ya procesados (idempotencia por eventId)
CREATE TABLE processed_events (
                                  event_id VARCHAR(200) PRIMARY KEY,
                                  processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Fila unica de estado de sincronizacion: necesaria para poder bloquearla con SELECT ... FOR UPDATE
INSERT INTO sync_state (entity_name, version, last_synced_at)
VALUES ('CATALOG', 0, NOW())
    ON CONFLICT (entity_name) DO NOTHING;