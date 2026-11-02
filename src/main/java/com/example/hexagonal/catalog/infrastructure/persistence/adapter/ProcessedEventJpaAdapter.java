package com.example.hexagonal.catalog.infrastructure.persistence.adapter;

import com.example.hexagonal.catalog.domain.port.out.ProcessedEventRepository;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.ProcessedEventEntity;
import com.example.hexagonal.catalog.infrastructure.persistence.repository.SpringDataProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class ProcessedEventJpaAdapter implements ProcessedEventRepository {

    private final SpringDataProcessedEventRepository repository;

    public ProcessedEventJpaAdapter(SpringDataProcessedEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean exists(String eventId) {
        return repository.existsById(eventId);
    }

    // No llamar dentro de otra transaccion: un duplicado la dejaria marcada para rollback.
    @Override
    public boolean markProcessed(String eventId) {
        if (repository.existsById(eventId)) {
            return false;
        }
        try {
            repository.saveAndFlush(new ProcessedEventEntity(eventId, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException e) {

            return false;
        }
    }
}