package com.example.hexagonal.catalog.infrastructure.persistence.adapter;

import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import com.example.hexagonal.catalog.infrastructure.persistence.mapper.CategoryMapper;
import com.example.hexagonal.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
    public List<ProfessionalCategory> saveAll(List<ProfessionalCategory> categories) {
        return repository.saveAll(categories.stream().map(mapper::toEntity).toList())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<ProfessionalCategory> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ProfessionalCategory> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteAll() {
        // Borrado en batch: se ejecuta de inmediato, antes de las reinserciones del mismo snapshot
        repository.deleteAllInBatch();
    }
}