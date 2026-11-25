package com.saferoute.sos;

import com.saferoute.sos.security.KeycloakJwtAuthenticationConverter;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {

    @Test
    void extractsRealmRolesAsSpringAuthorities() {
        Jwt jwt = Jwt.withTokenValue("fake-token")
                .header("alg", "RS256")
                .subject("user-uuid-123")
                .claim("preferred_username", "admin")
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "USER")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();
        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getName()).isEqualTo("admin");
        assertThat(token.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "ROLE_USER");
    }
}
