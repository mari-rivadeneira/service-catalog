package com.example.hexagonal.catalog.infrastructure.web.dto;

import com.example.hexagonal.catalog.domain.model.User;

import java.time.Instant;
import java.util.Set;

// Datos de la cuenta, con la forma del usuario de JHipster. Nunca incluye la contrasena ni su hash.
public record AccountResponse(Long id, String login, String firstName, String lastName, String email,
                              String imageUrl, boolean activated, String langKey, Set<String> authorities,
                              String createdBy, Instant createdDate, String lastModifiedBy,
                              Instant lastModifiedDate) {

    public static AccountResponse from(User user) {
        return new AccountResponse(user.getId(), user.getLogin(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getImageUrl(), user.isActivated(), user.getLangKey(),
                Set.copyOf(user.getAuthorities()), user.getCreatedBy(), user.getCreatedDate(),
                user.getLastModifiedBy(), user.getLastModifiedDate());
    }
}
