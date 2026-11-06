package com.example.hexagonal.catalog.domain.port.out;

import com.example.hexagonal.catalog.domain.model.User;

public interface TokenIssuer {

    String issue(User user, boolean rememberMe);
}
