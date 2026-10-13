package com.example.floodmonitor.config;

import com.example.floodmonitor.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sicherheit:
 *  - /api/** nur mit Login (HTTP Basic), Benutzer und Passwort stehen in application.properties
 *  - Webclient und Swagger UI ohne Login
 *  - keine Session (stateless), deshalb ist kein CSRF-Schutz noetig
 */
@Configuration
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, e) -> {
                    // 401 im gleichen Fehlerformat wie alle anderen Fehler.
                    // Ohne "WWW-Authenticate"-Header, sonst zeigt der Browser ein eigenes Login-Fenster.
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    objectMapper.writeValue(response.getWriter(), ApiError.of(HttpStatus.UNAUTHORIZED,
                            "Anmeldung erforderlich: Benutzer oder Passwort falsch", request.getRequestURI()));
                }));
        return http.build();
    }
}
