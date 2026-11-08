package com.example.hexagonal.catalog.domain.port.in;

import com.example.hexagonal.catalog.domain.model.User;

public interface GetAccountUseCase {

    User getAccount(long userId);
}