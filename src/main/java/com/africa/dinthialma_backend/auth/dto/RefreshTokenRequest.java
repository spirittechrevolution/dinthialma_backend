package com.africa.dinthialma_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Requête de rafraîchissement de token JWT via le refresh_token Keycloak")
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenRequest {

  @Schema(
      description =
          "Token de rafraîchissement obtenu lors du dernier login ou refresh."
              + " Expire selon la configuration Keycloak (défaut : 30 min).")
  @NotBlank(message = "Le refresh token est obligatoire")
  private String refreshToken;
}
