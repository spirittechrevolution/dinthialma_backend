package com.africa.dinthialma_backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Tokens JWT retournés après une connexion réussie")
@Getter
@Setter
@NoArgsConstructor
public class LoginResponse {

  @Schema(
      description =
          "JWT d'accès à transmettre dans le header Authorization: Bearer <token>. Valide ~5 min"
              + " par défaut Keycloak.")
  @JsonProperty("access_token")
  private String accessToken;

  @Schema(description = "Token de rafraîchissement – utiliser sur /v1/auth/refresh")
  @JsonProperty("refresh_token")
  private String refreshToken;

  @Schema(description = "Durée de vie de l'access_token en secondes", example = "300")
  @JsonProperty("expires_in")
  private int expiresIn;

  @Schema(description = "Durée de vie du refresh_token en secondes", example = "1800")
  @JsonProperty("refresh_expires_in")
  private int refreshExpiresIn;

  @Schema(description = "Type du token", example = "Bearer")
  @JsonProperty("token_type")
  private String tokenType;

  @Schema(description = "Identifiant de session Keycloak")
  @JsonProperty("session_state")
  private String sessionState;

  @Schema(description = "Scopes accordés", example = "openid profile")
  private String scope;

  @Schema(description = "Indique si l'utilisateur a configuré un code PIN", example = "true")
  @JsonProperty("pin_configured")
  private boolean pinConfigured;
}
