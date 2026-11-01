package com.example.hexagonal.catalog.infrastructure.persistence.mapper;

import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalMapper {

    public Professional toDomain(ProfessionalEntity entity) {
        return new Professional(entity.getId(), entity.getCategoryId(), entity.getFirstName(),
                entity.getLastName(), entity.isEnabled(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public ProfessionalEntity toEntity(Professional professional) {
        ProfessionalEntity entity = new ProfessionalEntity();
        entity.setId(professional.getId());
        entity.setCategoryId(professional.getCategoryId());
        entity.setFirstName(professional.getFirstName());
        entity.setLastName(professional.getLastName());
        entity.setEnabled(professional.isEnabled());
        entity.setCreatedAt(professional.getCreatedAt());
        entity.setUpdatedAt(professional.getUpdatedAt());
        return entity;
    }
}