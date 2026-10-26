package com.example.hexagonal.catalog.infrastructure.persistence.mapper;

import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalMapper {

    public Professional toDomain(ProfessionalEntity entity) {
        if (entity == null) return null;
        return new Professional(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getCategory() != null ? entity.getCategory().getId() : null,
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProfessionalEntity toEntity(Professional domain, CategoryEntity categoryEntity) {
        if (domain == null) return null;
        return new ProfessionalEntity(
                domain.getId(),
                domain.getFirstName(),
                domain.getLastName(),
                domain.getEmail(),
                domain.getPhone(),
                categoryEntity,
                domain.isActive(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}