package com.example.hexagonal.catalog.infrastructure.kafka.consumer;

import com.example.hexagonal.catalog.domain.port.in.ProcessCatalogUpdatedEventUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CatalogUpdatedKafkaListenerTest {

    @Mock
    private ProcessCatalogUpdatedEventUseCase useCase;

    @Mock
    private Acknowledgment acknowledgment;

    private CatalogUpdatedKafkaListener listener;

    @BeforeEach
    void setUp() {
        listener = new CatalogUpdatedKafkaListener(useCase, JsonMapper.builder().build());
    }

    private ConsumerRecord<String, String> record(String value) {
        return new ConsumerRecord<>("catedra.catalog.local", 0, 0L, "7", value);
    }

    @Test
    @DisplayName("Procesa un CatalogUpdated del contrato, ignora campos desconocidos y confirma el offset")
    void shouldProcessValidMessageIgnoringUnknownFields() {
        String json = """
                {
                  "eventId": "catalog-event-7-4f3b",
                  "eventType": "CatalogUpdated",
                  "newVersion": 7,
                  "occurredAt": "2026-09-10T13:44:59Z",
                  "source": "MANUAL",
                  "schemaVersion": 1,
                  "campoNuevoCompatible": "ignorado"
                }
                """;

        listener.onMessage(record(json), acknowledgment);

        verify(useCase).process("catalog-event-7-4f3b", 7L);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Un mensaje ilegible se descarta y se confirma el offset")
    void shouldDiscardUnreadableMessage() {
        listener.onMessage(record("esto no es json"), acknowledgment);

        verifyNoInteractions(useCase);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Un mensaje sin contenido se descarta y se confirma el offset")
    void shouldDiscardMessageWithoutValue() {
        listener.onMessage(record(null), acknowledgment);

        verifyNoInteractions(useCase);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Un mensaje sin eventId o sin newVersion se descarta y se confirma el offset")
    void shouldDiscardIncompleteMessage() {
        listener.onMessage(record("{\"eventType\": \"CatalogUpdated\", \"newVersion\": 7}"), acknowledgment);
        listener.onMessage(record("{\"eventId\": \"e1\", \"eventType\": \"CatalogUpdated\"}"), acknowledgment);

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Si el procesamiento falla NO se confirma el offset y se propaga el error")
    void shouldNotAcknowledgeWhenProcessingFails() {
        String json = "{\"eventId\": \"e1\", \"eventType\": \"CatalogUpdated\", \"newVersion\": 7}";
        doThrow(new IllegalStateException("Redis caido")).when(useCase).process("e1", 7L);

        assertThrows(IllegalStateException.class, () -> listener.onMessage(record(json), acknowledgment));

        verify(acknowledgment, never()).acknowledge();
    }
}