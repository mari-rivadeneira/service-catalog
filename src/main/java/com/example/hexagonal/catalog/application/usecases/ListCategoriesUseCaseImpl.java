package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.port.in.ListCategoriesUseCase;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ListCategoriesUseCaseImpl implements ListCategoriesUseCase {

    private final CategoryRepository categoryRepository;

    public ListCategoriesUseCaseImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<ProfessionalCategory> listCategories() {
        List<ProfessionalCategory> categories = new ArrayList<>(categoryRepository.findAll());
        categories.sort(Comparator.comparing(ProfessionalCategory::getName, String.CASE_INSENSITIVE_ORDER));
        return categories;
    }
}
