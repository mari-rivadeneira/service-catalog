package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.EntityNotFoundException;
import com.example.hexagonal.catalog.domain.model.Professional;
import com.example.hexagonal.catalog.domain.port.in.GetProfessionalUseCase;
import com.example.hexagonal.catalog.domain.port.out.ProfessionalRepository;
import org.springframework.stereotype.Service;

@Service
public class GetProfessionalUseCaseImpl implements GetProfessionalUseCase {

    private final ProfessionalRepository professionalRepository;

    public GetProfessionalUseCaseImpl(ProfessionalRepository professionalRepository) {
        this.professionalRepository = professionalRepository;
    }

    @Override
    public Professional getById(long id) {
        return professionalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Profesional no encontrado: " + id));
    }
}