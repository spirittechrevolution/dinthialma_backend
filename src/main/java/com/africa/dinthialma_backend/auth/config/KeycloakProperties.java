package com.africa.dinthialma_backend.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Propriétés Keycloak mappées depuis application.yml → keycloak.* */
@Component
@ConfigurationProperties(prefix = "keycloak")
@Getter
@Setter
public class KeycloakProperties {

  private String realm;
  private String authServerUrl;
  private String clientId;
  private String clientSecret;
  private String issuerUri;

  /**
   * Identifiant de l'administrateur du realm {@code master}. Utilisé exclusivement par le Keycloak
   * Admin Client pour les opérations de gestion (création d'utilisateurs, attribution de rôles…).
   */
  private String adminUsername;

  /** Mot de passe de l'administrateur du realm {@code master}. */
  private String adminPassword;
}
