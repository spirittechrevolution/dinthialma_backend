package com.africa.dinthialma_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI / Swagger UI – Dinthialma Backend.
 *
 * <p>Le {@code serverUrl} intègre le {@code context-path} ({@code /api}) afin que les appels lancés
 * depuis Swagger UI atterrissent sur le bon chemin (ex. {@code /api/v1/auth/login}).
 *
 * <p>Swagger UI accessible sur : {@code http://localhost:8081/api/swagger-ui.html}
 */
@Configuration
public class OpenApiConfig {

  @Value("${server.servlet.context-path:/api}")
  private String contextPath;

  @Value("${keycloak.auth-server-url:http://localhost:8280}")
  private String keycloakUrl;

  @Value("${keycloak.realm:dinthialma}")
  private String realm;

  @Bean
  public OpenAPI dinthialmaOpenAPI() {

    String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

    return new OpenAPI()
        // ── Serveur ─────────────────────────────────────────────────────
        .servers(
            List.of(
                new Server().url(contextPath).description("Serveur courant (context-path inclus)")))

        // ── Informations générales ───────────────────────────────────────
        .info(
            new Info()
                .title("Dinthialma API")
                .version("1.0.0")
                .description(
                    "API REST de la plateforme Dinthialma – Gestion de tontines et d'épargne collective")
                .contact(
                    new Contact().name("Spirit Tech Revolution").email("contact@dinthialma.io")))

        // ── Schémas de sécurité ──────────────────────────────────────────
        .components(
            new Components()
                // Bearer JWT (header Authorization: Bearer <token>)
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Collez ici le JWT obtenu via /v1/auth/login"))
                // OAuth2 Password Flow (bouton Authorize Swagger)
                .addSecuritySchemes(
                    "oauth2",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(
                            new OAuthFlows()
                                .password(
                                    new OAuthFlow().tokenUrl(tokenUrl).scopes(new Scopes())))));
  }
}
