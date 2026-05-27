package com.africa.dinthialma_backend.auth.codeList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Rôles utilisateur Dinthialma – mappés sur les rôles Keycloak du realm "dinthialma".
 *
 * <p>Un utilisateur peut cumuler plusieurs rôles (ex : MEMBER + ADMIN).
 *
 * <p>Cycle d'attribution :
 *
 * <ul>
 *   <li>{@link #USER} → attribué dès l'inscription
 *   <li>{@link #MEMBER} → attribué lors de l'adhésion à une tontine en tant que cotisant
 *   <li>{@link #ADMIN} → attribué lors de la création d'une tontine
 *   <li>{@link #SUPER_ADMIN} → attribution manuelle par la plateforme
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {

  /** Utilisateur inscrit – accès de base à l'application, sans participation active. */
  USER("DINTHIALMA_USER"),

  /** Cotisant actif dans au moins une tontine. */
  MEMBER("DINTHIALMA_MEMBER"),

  /** Administrateur d'au moins une tontine. */
  ADMIN("DINTHIALMA_ADMIN"),

  /** Administrateur plateforme – accès total. */
  SUPER_ADMIN("DINTHIALMA_SUPER_ADMIN");

  /** Nom du rôle tel qu'il apparaît dans le {@code realm_access} du JWT Keycloak. */
  private final String keycloakRole;
}
