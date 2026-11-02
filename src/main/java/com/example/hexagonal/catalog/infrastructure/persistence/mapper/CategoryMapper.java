package com.example.hexagonal.catalog.infrastructure.persistence.mapper;

import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public ProfessionalCategory toDomain(CategoryEntity entity) {
        return new ProfessionalCategory(entity.getId(), entity.getName(), entity.getDescription(),
                entity.isEnabled(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public CategoryEntity toEntity(ProfessionalCategory category) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(category.getId());
        entity.setName(category.getName());
        entity.setDescription(category.getDescription());
        entity.setEnabled(category.isEnabled());
        entity.setCreatedAt(category.getCreatedAt());
        entity.setUpdatedAt(category.getUpdatedAt());
        return entity;
    }
}