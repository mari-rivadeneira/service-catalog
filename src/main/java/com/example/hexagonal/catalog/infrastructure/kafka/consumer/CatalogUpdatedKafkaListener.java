package com.example.hexagonal.catalog.infrastructure.kafka.consumer;

import com.example.hexagonal.catalog.domain.port.in.ProcessCatalogUpdatedEventUseCase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class CatalogUpdatedKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogUpdatedKafkaListener.class);
    private static final String EVENT_TYPE = "CatalogUpdated";

    private final ProcessCatalogUpdatedEventUseCase processCatalogUpdatedEventUseCase;
    private final ObjectMapper objectMapper;

    public CatalogUpdatedKafkaListener(ProcessCatalogUpdatedEventUseCase processCatalogUpdatedEventUseCase,
                                       ObjectMapper objectMapper) {
        this.processCatalogUpdatedEventUseCase = processCatalogUpdatedEventUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${catedra.kafka.catalog-topic}")
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        CatalogUpdatedMessage message = parse(record);
        if (message == null) {
            // Un mensaje que nunca va a poder procesarse se descarta para no bloquear la particion
            acknowledgment.acknowledge();
            return;
        }

        // Si falla (Redis o base caidos) la excepcion sube: no se confirma el offset y el
        // manejador de errores reintenta de forma acotada.
        processCatalogUpdatedEventUseCase.process(message.eventId(), message.newVersion());
        acknowledgment.acknowledge();
    }

    private CatalogUpdatedMessage parse(ConsumerRecord<String, String> record) {
        if (record.value() == null) {
            log.warn("Mensaje sin contenido en el offset {}: se descarta", record.offset());
            return null;
        }

        CatalogUpdatedMessage message;
        try {
            message = objectMapper.readValue(record.value(), CatalogUpdatedMessage.class);
        } catch (JacksonException e) {
            log.warn("Mensaje ilegible en el offset {}: {}", record.offset(), e.getMessage());
            return null;
        }

        if (!EVENT_TYPE.equals(message.eventType())
                || message.eventId() == null || message.eventId().isBlank()
                || message.newVersion() == null) {
            log.warn("Mensaje incompleto o de otro tipo en el offset {}: se descarta", record.offset());
            return null;
        }
        return message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogUpdatedMessage(String eventId, String eventType, Long newVersion) {
    }
}