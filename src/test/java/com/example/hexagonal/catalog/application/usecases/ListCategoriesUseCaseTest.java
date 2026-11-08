package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.domain.model.ProfessionalCategory;
import com.example.hexagonal.catalog.domain.port.out.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCategoriesUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    private ProfessionalCategory category(long id, String name) {
        Instant now = Instant.now();
        return new ProfessionalCategory(id, name, null, true, now, now);
    }

    @Test
    @DisplayName("Devuelve las categorias ordenadas por nombre sin distinguir mayusculas")
    void shouldReturnCategoriesSortedByName() {
        when(categoryRepository.findAll()).thenReturn(List.of(
                category(1L, "pediatria"), category(2L, "Clinica"), category(3L, "Anestesia")));

        List<ProfessionalCategory> result = new ListCategoriesUseCaseImpl(categoryRepository).listCategories();

        assertEquals(List.of("Anestesia", "Clinica", "pediatria"),
                result.stream().map(ProfessionalCategory::getName).toList());
    }
}
