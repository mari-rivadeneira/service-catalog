package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.InvalidSearchCriteriaException;
import com.example.hexagonal.catalog.domain.model.PageResult;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.model.ProfessionalSearchCriteria;
import com.example.hexagonal.catalog.domain.port.in.SearchProfessionalsUseCase;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalSearchRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SearchProfessionalsUseCaseImpl implements SearchProfessionalsUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PAGE = 100_000;
    private static final int MAX_NAME_LENGTH = 100;

    private final ProfessionalSearchRepository searchRepository;

    public SearchProfessionalsUseCaseImpl(ProfessionalSearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Override
    public PageResult<Professional> search(ProfessionalSearchCriteria criteria) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (criteria.page() < 0 || criteria.page() > MAX_PAGE) {
            errors.put("page", "debe estar entre 0 y " + MAX_PAGE);
        }
        if (criteria.size() < 1 || criteria.size() > MAX_PAGE_SIZE) {
            errors.put("size", "debe estar entre 1 y " + MAX_PAGE_SIZE);
        }

        String name = criteria.name() == null || criteria.name().isBlank() ? null : criteria.name().trim();
        if (name != null && name.length() > MAX_NAME_LENGTH) {
            errors.put("name", "no puede superar los " + MAX_NAME_LENGTH + " caracteres");
        }

        if (criteria.dayOfWeek() != null && Boolean.FALSE.equals(criteria.hasSchedules())) {
            errors.put("dayOfWeek", "no se puede combinar con hasSchedules=false");
        }

        if (!errors.isEmpty()) {
            throw new InvalidSearchCriteriaException(errors);
        }

        return searchRepository.search(new ProfessionalSearchCriteria(
                criteria.categoryId(), name, criteria.enabled(), criteria.hasSchedules(),
                criteria.dayOfWeek(), criteria.page(), criteria.size()));
    }
}