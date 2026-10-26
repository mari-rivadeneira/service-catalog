package com.example.hexagonal.catalog.infrastructure.persistence.adapter;

import com.example.hexagonal.catalog.domain.model.SyncState;
import com.example.hexagonal.catalog.domain.port.out.SyncStateRepository;
import com.example.hexagonal.catalog.infrastructure.persistence.mapper.SyncStateMapper;
import com.example.hexagonal.catalog.infrastructure.persistence.repository.SpringDataSyncStateRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SyncStateJpaAdapter implements SyncStateRepository {

    private final SpringDataSyncStateRepository repository;
    private final SyncStateMapper mapper;

    public SyncStateJpaAdapter(SpringDataSyncStateRepository repository, SyncStateMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public SyncState save(SyncState syncState) {
        return mapper.toDomain(repository.save(mapper.toEntity(syncState)));
    }

    @Override
    public Optional<SyncState> findByEntityName(String entityName) {
        return repository.findById(entityName).map(mapper::toDomain);
    }
}