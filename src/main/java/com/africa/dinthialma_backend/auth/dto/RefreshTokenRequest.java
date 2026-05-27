package com.africa.dinthialma_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Requête de rafraîchissement de token JWT via le refresh_token Keycloak. */
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenRequest {

  @NotBlank(message = "Le refresh token est obligatoire")
  private String refreshToken;
}
