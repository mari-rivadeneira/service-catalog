package com.example.hexagonal.catalog.infrastructure.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Solo existe en los tests: permite comprobar el 403 de la seguridad por metodo. */
@RestController
class AdminOnlyTestController {

    @GetMapping("/api/test/admin-only")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String adminOnly() {
        return "ok";
    }
}