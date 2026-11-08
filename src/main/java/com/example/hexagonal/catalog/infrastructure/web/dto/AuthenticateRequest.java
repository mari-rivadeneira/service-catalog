package com.example.hexagonal.catalog.infrastructure.web.dto;

public record AuthenticateRequest(String username, String password, Boolean rememberMe) {
}
