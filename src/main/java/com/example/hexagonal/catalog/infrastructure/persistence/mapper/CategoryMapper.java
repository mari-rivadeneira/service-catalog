package com.example.hexagonal.catalog.infrastructure.persistence.mapper;

import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public ProfessionalCategory toDomain(CategoryEntity entity) {
        if (entity == null) return null;
        return new ProfessionalCategory(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CategoryEntity toEntity(ProfessionalCategory domain) {
        if (domain == null) return null;
        return new CategoryEntity(
                domain.getId(),
                domain.getName(),
                domain.getDescription(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}