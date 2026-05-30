package com.africa.dinthialma_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
    description =
        "Requête de déconnexion. Le refresh_token est invalidé côté Keycloak,"
            + " rendant tout renouvellement de l'access_token impossible.")
@Getter
@Setter
@NoArgsConstructor
public class LogoutRequest {

  @Schema(description = "Token de rafraîchissement à invalider")
  @NotBlank(message = "Le refresh token est obligatoire")
  private String refreshToken;
}
