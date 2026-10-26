package com.example.hexagonal.catalog.infrastructure.persistence.adapter;

import com.example.hexagonal.catalog.domain.model.SyncVersion;
import com.example.hexagonal.catalog.domain.port.out.SyncVersionRepository;
import com.example.hexagonal.catalog.infrastructure.persistence.mapper.SyncVersionMapper;
import com.example.hexagonal.catalog.infrastructure.persistence.repository.SpringDataSyncVersionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SyncVersionJpaAdapter implements SyncVersionRepository {

    private final SpringDataSyncVersionRepository repository;
    private final SyncVersionMapper mapper;

    public SyncVersionJpaAdapter(SpringDataSyncVersionRepository repository, SyncVersionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public SyncVersion save(SyncVersion syncVersion) {
        return mapper.toDomain(repository.save(mapper.toEntity(syncVersion)));
    }

    @Override
    public Optional<SyncVersion> findByEntityName(String entityName) {
        return repository.findById(entityName).map(mapper::toDomain);
    }
}