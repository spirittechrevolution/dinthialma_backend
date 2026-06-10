package com.africa.dinthialma_backend.auth.dto;

import com.africa.dinthialma_backend.auth.codeList.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
    description =
        "Requête de déconnexion. Le refresh_token est invalidé côté Keycloak"
            + " et la session PIN correspondante est supprimée en base.")
@Getter
@Setter
@NoArgsConstructor
public class LogoutRequest {

  @Schema(description = "Token de rafraîchissement à invalider")
  @NotBlank(message = "Le refresh token est obligatoire")
  private String refreshToken;

  @Schema(
      description =
          "Type de client concerné (WEB ou MOBILE). Si absent, toutes les sessions sont révoquées.",
      nullable = true,
      allowableValues = {"WEB", "MOBILE"})
  private ClientType clientType;
}
