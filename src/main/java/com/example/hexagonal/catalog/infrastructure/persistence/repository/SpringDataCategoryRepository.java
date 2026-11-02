package com.example.hexagonal.catalog.infrastructure.persistence.repository;

import com.example.hexagonal.catalog.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCategoryRepository extends JpaRepository<CategoryEntity, Long> {
}