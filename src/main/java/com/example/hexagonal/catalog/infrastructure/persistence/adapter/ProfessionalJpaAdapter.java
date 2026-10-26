package com.example.hexagonal.catalog.infrastructure.persistence.adapter;

import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.example.hexagonal.catalog.infrastructure.persistence.mapper.ProfessionalMapper;
import com.example.hexagonal.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.example.hexagonal.catalog.infrastructure.persistence.repository.SpringDataProfessionalRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProfessionalJpaAdapter implements ProfessionalRepository {

    private final SpringDataProfessionalRepository professionalRepo;
    private final SpringDataCategoryRepository categoryRepo;
    private final ProfessionalMapper mapper;

    public ProfessionalJpaAdapter(SpringDataProfessionalRepository professionalRepo,
                                  SpringDataCategoryRepository categoryRepo,
                                  ProfessionalMapper mapper) {
        this.professionalRepo = professionalRepo;
        this.categoryRepo = categoryRepo;
        this.mapper = mapper;
    }

    @Override
    public Professional save(Professional professional) {
        CategoryEntity category = categoryRepo.findById(professional.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con id: " + professional.getCategoryId()));
        return mapper.toDomain(professionalRepo.save(mapper.toEntity(professional, category)));
    }

    @Override
    public Optional<Professional> findById(UUID id) {
        return professionalRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Professional> findByEmail(String email) {
        return professionalRepo.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public List<Professional> findAll() {
        return professionalRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Professional> findByCategoryId(UUID categoryId) {
        return professionalRepo.findByCategoryId(categoryId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return professionalRepo.existsByEmail(email);
    }

    @Override
    public void deleteById(UUID id) {
        professionalRepo.deleteById(id);
    }
}