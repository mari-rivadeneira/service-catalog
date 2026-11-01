package com.example.hexagonal.catalog.infrastructure.persistence.adapter;

import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.infrastructure.persistence.mapper.ProfessionalMapper;
import com.example.hexagonal.catalog.infrastructure.persistence.repository.SpringDataProfessionalRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProfessionalJpaAdapter implements ProfessionalRepository {

    private final SpringDataProfessionalRepository repository;
    private final ProfessionalMapper mapper;

    public ProfessionalJpaAdapter(SpringDataProfessionalRepository repository, ProfessionalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Professional save(Professional professional) {
        return mapper.toDomain(repository.save(mapper.toEntity(professional)));
    }

    @Override
    public List<Professional> saveAll(List<Professional> professionals) {
        return repository.saveAll(professionals.stream().map(mapper::toEntity).toList())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Professional> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Professional> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Professional> findByCategoryId(Long categoryId) {
        return repository.findByCategoryId(categoryId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteAll() {
        repository.deleteAllInBatch();
    }
}