package com.saferoute.sos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "keycloak-bearer";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Safe Route - MS4 SoS Service API")
                        .description("Microservice de gestion des urgences SOS pour le projet Safe Route.\n"
                                + "Responsable : Imen.\n"
                                + "Expose les endpoints REST (RFC 9457), communique en synchrone via OpenFeign (MS1) "
                                + "et en asynchrone via RabbitMQ, sécurisé par Keycloak JWT (profil secured/docker).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Imen - Responsable MS4 SoS Service")
                                .email("imen@saferoute.tn")))
                .servers(List.of(
                        new Server().url("http://localhost:8084").description("Accès direct au microservice"),
                        new Server().url("http://localhost:8080/sos-service").description("Accès via API Gateway (Discovery Locator)"),
                        new Server().url("http://localhost:8080").description("Accès via API Gateway (Route explicite)")
                ))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Jeton JWT Keycloak (requis en mode sécurisé / Docker)")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
    }
}
