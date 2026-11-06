package com.example.hexagonal.catalog.infrastructure.web.dto;

public record RegisterRequest(String login, String password, String firstName, String lastName,
                              String email, String imageUrl, String langKey) {
}
