package com.example.hexagonal.catalog.infrastructure.persistence.adapter;

import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import com.example.hexagonal.catalog.infrastructure.persistence.mapper.CategoryMapper;
import com.example.hexagonal.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CategoryJpaAdapter implements CategoryRepository {

    private final SpringDataCategoryRepository repository;
    private final CategoryMapper mapper;

    public CategoryJpaAdapter(SpringDataCategoryRepository repository, CategoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ProfessionalCategory save(ProfessionalCategory category) {
        return mapper.toDomain(repository.save(mapper.toEntity(category)));
    }

    @Override
    public Optional<ProfessionalCategory> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ProfessionalCategory> findByName(String name) {
        return repository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public List<ProfessionalCategory> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByName(name);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}