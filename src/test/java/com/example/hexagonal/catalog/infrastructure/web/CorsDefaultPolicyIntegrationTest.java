package com.example.hexagonal.catalog.infrastructure.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Politica por defecto: sin CORS_ALLOWED_ORIGINS no se permite ningun origen. Requiere el compose activo. */
@SpringBootTest
class CorsDefaultPolicyIntegrationTest {

    private static final String FOREIGN_ORIGIN = "https://sitio-ajeno.example";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Una consulta previa (preflight) de cualquier origen se rechaza")
    void shouldRejectPreflightFromAnyOrigin() throws Exception {
        mockMvc.perform(options("/api/authenticate")
                        .header(HttpHeaders.ORIGIN, FOREIGN_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un pedido real de otro origen tambien se rechaza")
    void shouldRejectCrossOriginRequest() throws Exception {
        mockMvc.perform(post("/api/authenticate")
                        .header(HttpHeaders.ORIGIN, FOREIGN_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"x\", \"password\": \"y\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un cliente que no es navegador (sin cabecera Origin), como la app Android, no se ve afectado")
    void shouldNotAffectRequestsWithoutOrigin() throws Exception {
        mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"fantasma\", \"password\": \"clave\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}