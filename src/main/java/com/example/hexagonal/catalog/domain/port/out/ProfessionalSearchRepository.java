package com.example.hexagonal.catalog.domain.port.out;

import com.example.hexagonal.catalog.domain.model.PageResult;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalSearchCriteria;

public interface ProfessionalSearchRepository {

    /** Ordena por apellido, nombre e id. Los criterios ya vienen validados por el caso de uso. */
    PageResult<Professional> search(ProfessionalSearchCriteria criteria);
}