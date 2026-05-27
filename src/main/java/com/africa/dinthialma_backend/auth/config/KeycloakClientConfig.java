package com.africa.dinthialma_backend.auth.config;

import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du client Keycloak Admin.
 *
 * <p>Authentification via le realm {@code master} avec les credentials de l'admin Keycloak
 * (grant_type=password, client_id=admin-cli). Cette approche garantit que le token obtenu contient
 * bien les droits {@code realm-management} nécessaires aux opérations CRUD utilisateurs dans
 * Keycloak 26.x.
 */
@Configuration
@RequiredArgsConstructor
public class KeycloakClientConfig {

  private final KeycloakProperties keycloakProperties;

  @Bean
  public Keycloak keycloakAdminClient() {
    return KeycloakBuilder.builder()
        .serverUrl(keycloakProperties.getAuthServerUrl())
        .realm("master")
        .clientId("admin-cli")
        .username(keycloakProperties.getAdminUsername())
        .password(keycloakProperties.getAdminPassword())
        .grantType("password")
        .build();
  }
}
