package com.example.hexagonal.catalog.infrastructure.persistence.repository;

import com.example.hexagonal.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataProfessionalRepository extends JpaRepository<ProfessionalEntity, UUID> {
    Optional<ProfessionalEntity> findByEmail(String email);
    List<ProfessionalEntity> findByCategoryId(UUID categoryId);
    boolean existsByEmail(String email);
}