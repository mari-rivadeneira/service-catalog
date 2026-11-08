package com.example.hexagonal.catalog.domain.port.in;

import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;

import java.util.List;

public interface ListCategoriesUseCase {

    /** Todas las categorias (habilitadas y deshabilitadas), ordenadas por nombre. */
    List<ProfessionalCategory> listCategories();
}
