package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.InvalidSearchCriteriaException;
import com.example.hexagonal.catalog.domain.model.PageResult;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalSearchCriteria;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchProfessionalsUseCaseTest {

    @Mock
    private ProfessionalSearchRepository searchRepository;

    private SearchProfessionalsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchProfessionalsUseCaseImpl(searchRepository);
    }

    private ProfessionalSearchCriteria criteria(String name, Boolean hasSchedules, DayOfWeek day, int page, int size) {
        return new ProfessionalSearchCriteria(3L, name, true, hasSchedules, day, page, size);
    }

    @Test
    @DisplayName("Delega en el repositorio con el nombre sin espacios sobrantes y el resto de los filtros intactos")
    void shouldDelegateWithTrimmedName() {
        PageResult<Professional> expected = new PageResult<>(List.of(), 0, 20, 0, 0);
        when(searchRepository.search(criteria("Ana", true, DayOfWeek.MONDAY, 0, 20))).thenReturn(expected);

        PageResult<Professional> result = useCase.search(criteria("  Ana  ", true, DayOfWeek.MONDAY, 0, 20));

        assertSame(expected, result);
    }

    @Test
    @DisplayName("Un nombre en blanco equivale a no filtrar por nombre")
    void shouldTreatBlankNameAsNoFilter() {
        when(searchRepository.search(criteria(null, null, null, 0, 20)))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

        useCase.search(criteria("   ", null, null, 0, 20));

        ArgumentCaptor<ProfessionalSearchCriteria> captor = ArgumentCaptor.forClass(ProfessionalSearchCriteria.class);
        verify(searchRepository).search(captor.capture());
        assertNull(captor.getValue().name());
    }

    @Test
    @DisplayName("Paginacion fuera de rango: informa page y size y no consulta la base")
    void shouldRejectInvalidPaging() {
        InvalidSearchCriteriaException negative = assertThrows(InvalidSearchCriteriaException.class,
                () -> useCase.search(criteria(null, null, null, -1, 0)));
        assertTrue(negative.getFieldErrors().containsKey("page"));
        assertTrue(negative.getFieldErrors().containsKey("size"));

        assertTrue(assertThrows(InvalidSearchCriteriaException.class,
                () -> useCase.search(criteria(null, null, null, 0, 101))).getFieldErrors().containsKey("size"));
        assertTrue(assertThrows(InvalidSearchCriteriaException.class,
                () -> useCase.search(criteria(null, null, null, 100_001, 20))).getFieldErrors().containsKey("page"));

        verifyNoInteractions(searchRepository);
    }

    @Test
    @DisplayName("Un nombre de mas de 100 caracteres se rechaza")
    void shouldRejectTooLongName() {
        InvalidSearchCriteriaException ex = assertThrows(InvalidSearchCriteriaException.class,
                () -> useCase.search(criteria("x".repeat(101), null, null, 0, 20)));

        assertEquals(1, ex.getFieldErrors().size());
        assertTrue(ex.getFieldErrors().containsKey("name"));
        verifyNoInteractions(searchRepository);
    }

    @Test
    @DisplayName("dayOfWeek combinado con hasSchedules=false es contradictorio y se rechaza")
    void shouldRejectDayOfWeekWithoutSchedules() {
        InvalidSearchCriteriaException ex = assertThrows(InvalidSearchCriteriaException.class,
                () -> useCase.search(criteria(null, false, DayOfWeek.MONDAY, 0, 20)));

        assertTrue(ex.getFieldErrors().containsKey("dayOfWeek"));
        verifyNoInteractions(searchRepository);
    }
}
