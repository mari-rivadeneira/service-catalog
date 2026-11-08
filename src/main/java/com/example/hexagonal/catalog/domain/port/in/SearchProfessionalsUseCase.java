package com.example.hexagonal.catalog.domain.port.in;

import com.example.hexagonal.catalog.domain.model.PageResult;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalSearchCriteria;

public interface SearchProfessionalsUseCase {

    /**
     * Busca sobre la copia local, sin consultar a la catedra.
     *
     * @throws com.example.hexagonal.catalog.application.exception.InvalidSearchCriteriaException
     *         si los filtros o la paginacion son invalidos
     */
    PageResult<Professional> search(ProfessionalSearchCriteria criteria);
}