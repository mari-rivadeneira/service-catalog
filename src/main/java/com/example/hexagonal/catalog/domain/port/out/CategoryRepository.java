package com.example.hexagonal.catalog.domain.port.out;

import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    ProfessionalCategory save(ProfessionalCategory category);
    List<ProfessionalCategory> saveAll(List<ProfessionalCategory> categories);
    Optional<ProfessionalCategory> findById(Long id);
    List<ProfessionalCategory> findAll();
    void deleteAll();
}