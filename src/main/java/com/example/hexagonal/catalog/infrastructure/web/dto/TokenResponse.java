package com.example.hexagonal.catalog.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(@JsonProperty("id_token") String idToken) {
}