package com.africa.dinthialma_backend.auth.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Représentation de l'utilisateur courant extrait du JWT Keycloak. */
@Getter
@AllArgsConstructor
public class RequestUser {

  /** Subject Keycloak (UUID Keycloak de l'utilisateur). */
  private final String sub;

  /** Rôles realm Keycloak (ex : DINTHIALMA_ADMIN, DINTHIALMA_MEMBER). */
  private final List<String> roles;
}
