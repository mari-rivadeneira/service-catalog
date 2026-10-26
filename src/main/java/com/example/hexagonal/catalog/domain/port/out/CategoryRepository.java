package com.example.hexagonal.catalog.domain.port.out;

import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    ProfessionalCategory save(ProfessionalCategory category);
    Optional<ProfessionalCategory> findById(UUID id);
    Optional<ProfessionalCategory> findByName(String name);
    List<ProfessionalCategory> findAll();
    boolean existsByName(String name);
    void deleteById(UUID id);
}