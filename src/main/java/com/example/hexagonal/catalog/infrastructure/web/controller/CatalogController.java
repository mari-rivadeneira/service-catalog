package com.example.hexagonal.catalog.infrastructure.web.controller;

import com.example.hexagonal.catalog.application.exception.InvalidSearchCriteriaException;
import com.example.hexagonal.catalog.domain.model.ProfessionalSearchCriteria;
import com.example.hexagonal.catalog.domain.port.in.GetProfessionalSchedulesUseCase;
import com.example.hexagonal.catalog.domain.port.in.GetProfessionalUseCase;
import com.example.hexagonal.catalog.domain.port.in.GetSyncStatusUseCase;
import com.example.hexagonal.catalog.domain.port.in.ListCategoriesUseCase;
import com.example.hexagonal.catalog.domain.port.in.SearchProfessionalsUseCase;
import com.example.hexagonal.catalog.infrastructure.web.dto.CategoryResponse;
import com.example.hexagonal.catalog.infrastructure.web.dto.PageResponse;
import com.example.hexagonal.catalog.infrastructure.web.dto.ProfessionalResponse;
import com.example.hexagonal.catalog.infrastructure.web.dto.SyncStatusResponse;
import com.example.hexagonal.catalog.infrastructure.web.dto.WeeklyScheduleResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Consulta del catalogo local. Todo se resuelve contra la copia sincronizada, sin consultar a la catedra. */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final SearchProfessionalsUseCase searchProfessionalsUseCase;
    private final GetProfessionalUseCase getProfessionalUseCase;
    private final GetProfessionalSchedulesUseCase getProfessionalSchedulesUseCase;
    private final ListCategoriesUseCase listCategoriesUseCase;
    private final GetSyncStatusUseCase getSyncStatusUseCase;

    public CatalogController(SearchProfessionalsUseCase searchProfessionalsUseCase,
                             GetProfessionalUseCase getProfessionalUseCase,
                             GetProfessionalSchedulesUseCase getProfessionalSchedulesUseCase,
                             ListCategoriesUseCase listCategoriesUseCase,
                             GetSyncStatusUseCase getSyncStatusUseCase) {
        this.searchProfessionalsUseCase = searchProfessionalsUseCase;
        this.getProfessionalUseCase = getProfessionalUseCase;
        this.getProfessionalSchedulesUseCase = getProfessionalSchedulesUseCase;
        this.listCategoriesUseCase = listCategoriesUseCase;
        this.getSyncStatusUseCase = getSyncStatusUseCase;
    }

    @GetMapping("/professionals")
    public PageResponse<ProfessionalResponse> searchProfessionals(
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            @RequestParam(name = "hasSchedules", required = false) Boolean hasSchedules,
            @RequestParam(name = "dayOfWeek", required = false) String dayOfWeek,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        ProfessionalSearchCriteria criteria = new ProfessionalSearchCriteria(
                categoryId, name, enabled, hasSchedules, parseDay(dayOfWeek), page, size);
        return PageResponse.from(searchProfessionalsUseCase.search(criteria), ProfessionalResponse::from);
    }

    @GetMapping("/professionals/{id}")
    public ProfessionalResponse getProfessional(@PathVariable("id") long id) {
        return ProfessionalResponse.from(getProfessionalUseCase.getById(id));
    }

    @GetMapping("/professionals/{id}/schedules")
    public List<WeeklyScheduleResponse> getSchedules(@PathVariable("id") long id) {
        return getProfessionalSchedulesUseCase.getSchedules(id).stream()
                .map(WeeklyScheduleResponse::from)
                .toList();
    }

    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return listCategoriesUseCase.listCategories().stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/sync/status")
    public SyncStatusResponse getSyncStatus() {
        return SyncStatusResponse.from(getSyncStatusUseCase.getStatus());
    }

    /** Acepta el dia en cualquier combinacion de mayusculas; un valor invalido es un error de validacion. */
    private static DayOfWeek parseDay(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidSearchCriteriaException(
                    Map.of("dayOfWeek", "debe ser un dia de la semana en ingles (MONDAY a SUNDAY)"));
        }
    }
}
