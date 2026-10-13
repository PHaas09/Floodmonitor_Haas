package com.example.floodmonitor.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI-Doku. springdoc erzeugt die Endpunkte automatisch aus dem Controller.
 * Swagger UI: http://localhost:8080/swagger-ui.html   YAML: http://localhost:8080/v3/api-docs.yaml
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "FloodMonitor REST API", version = "v1",
                description = "Messstationen, Messwerte, Warnungen und Statistik eines Hochwasser-Frühwarnsystems. "
                        + "Antworten als JSON (Standard) oder XML über den Accept-Header."),
        security = @SecurityRequirement(name = "basicAuth"))
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic",
        description = "HTTP Basic Login, Benutzer: viewer")
public class OpenApiConfig {
}
