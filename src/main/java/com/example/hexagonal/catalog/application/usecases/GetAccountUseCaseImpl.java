package com.example.hexagonal.catalog.application.usecases;

import com.example.hexagonal.catalog.application.exception.EntityNotFoundException;
import com.example.hexagonal.catalog.domain.model.User;
import com.example.hexagonal.catalog.domain.port.in.GetAccountUseCase;
import com.example.hexagonal.catalog.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class GetAccountUseCaseImpl implements GetAccountUseCase {

    private final UserRepository userRepository;

    public GetAccountUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getAccount(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    }
}