package com.saferoute.gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Configuration de sécurité réactive de l'API Gateway (Keycloak JWT - S8).
 * Active en mode sécurisé (`saferoute.security.enabled=true`), permissive en mode démo local.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "saferoute.security.enabled", havingValue = "true")
    public SecurityWebFilterChain securedGatewayFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/sos-service/swagger-ui/**",
                                "/sos-service/swagger-ui.html",
                                "/sos-service/v3/api-docs/**",
                                "/sos-service/actuator/health",
                                "/sos-service/actuator/info"
                        ).permitAll()
                        .pathMatchers("/actuator/metrics/**", "/actuator/loggers/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.GET, "/sos/active", "/sos-service/sos/active").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/sos/*/resolve", "/sos-service/sos/*/resolve").hasRole("ADMIN")
                        .pathMatchers("/sos/**", "/emergency/**", "/sos-service/**").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(reactiveKeycloakJwtConverter()))
                );

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "saferoute.security.enabled", havingValue = "false", matchIfMissing = true)
    public SecurityWebFilterChain localDevGatewayFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> reactiveKeycloakJwtConverter() {
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

        Converter<Jwt, AbstractAuthenticationToken> delegate = jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>(defaultConverter.convert(jwt));
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
                for (Object role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase()));
                }
            }
            String principal = jwt.getClaimAsString("preferred_username");
            if (principal == null || principal.isBlank()) {
                principal = jwt.getSubject();
            }
            return new JwtAuthenticationToken(jwt, authorities, principal);
        };

        return new ReactiveJwtAuthenticationConverterAdapter(delegate);
    }
}
