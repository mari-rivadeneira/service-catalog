package com.example.hexagonal.catalog.domain.port.in;

import com.example.hexagonal.catalog.domain.model.Professional;

public interface GetProfessionalUseCase {

    /** @throws com.example.hexagonal.catalog.application.exception.EntityNotFoundException si no existe */
    Professional getById(long id);
}
