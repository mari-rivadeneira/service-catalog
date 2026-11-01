package com.example.hexagonal.catalog.domain.port.out;

import com.example.hexagonal.catalog.domain.model.Professional;

import java.util.List;
import java.util.Optional;

public interface ProfessionalRepository {
    Professional save(Professional professional);
    List<Professional> saveAll(List<Professional> professionals);
    Optional<Professional> findById(Long id);
    List<Professional> findAll();
    List<Professional> findByCategoryId(Long categoryId);
    void deleteAll();
}