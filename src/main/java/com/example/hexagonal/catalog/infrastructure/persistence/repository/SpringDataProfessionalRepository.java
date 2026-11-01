package com.example.hexagonal.catalog.infrastructure.persistence.repository;

import com.example.hexagonal.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataProfessionalRepository extends JpaRepository<ProfessionalEntity, Long> {
    List<ProfessionalEntity> findByCategoryId(Long categoryId);
}