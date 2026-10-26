package com.example.hexagonal.catalog.domain.port.out;

import com.example.hexagonal.catalog.domain.model.Professional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalRepository {
    Professional save(Professional professional);
    Optional<Professional> findById(UUID id);
    Optional<Professional> findByEmail(String email);
    List<Professional> findAll();
    List<Professional> findByCategoryId(UUID categoryId);
    boolean existsByEmail(String email);
    void deleteById(UUID id);
}