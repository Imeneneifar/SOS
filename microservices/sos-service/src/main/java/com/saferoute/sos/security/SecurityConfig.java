package com.saferoute.sos.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Configuration de sécurité (S8 - Keycloak OAuth2 Resource Server / JWT).
 * - Mode sécurisé (`saferoute.security.enabled=true`, profils docker/secured) : validation JWT Keycloak + RBAC.
 * - Mode local démo (`saferoute.security.enabled=false`, par défaut) : accès ouvert pour tests Swagger/H2 sans Keycloak.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "saferoute.security.enabled", havingValue = "true")
    public SecurityFilterChain securedSecurityFilterChain(HttpSecurity http) throws Exception {
        KeycloakJwtAuthenticationConverter jwtConverter = new KeycloakJwtAuthenticationConverter();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info"
                        ).permitAll()
                        .requestMatchers("/actuator/metrics/**", "/actuator/loggers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/sos/active").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/sos/*/resolve").hasRole("ADMIN")
                        .requestMatchers("/sos/**", "/emergency/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeProblemJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Non authentifié",
                                        "Un jeton JWT Keycloak valide est requis pour accéder à cette ressource.",
                                        request.getRequestURI()))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeProblemJson(response, HttpServletResponse.SC_FORBIDDEN,
                                        "Accès refusé",
                                        "Vous ne disposez pas des droits nécessaires (rôle ADMIN requis) pour cette action.",
                                        request.getRequestURI()))
                );

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "saferoute.security.enabled", havingValue = "false", matchIfMissing = true)
    public SecurityFilterChain localDevSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    private void writeProblemJson(
            HttpServletResponse response,
            int status,
            String title,
            String detail,
            String instance) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String body = String.format(
                "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"instance\":\"%s\"}",
                title, status, detail, instance
        );
        response.getWriter().write(body);
    }
}
