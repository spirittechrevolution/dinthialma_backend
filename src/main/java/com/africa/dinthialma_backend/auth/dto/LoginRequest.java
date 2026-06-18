package com.africa.dinthialma_backend.auth.dto;

import com.africa.dinthialma_backend.auth.codeList.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Corps de la requête de connexion par identifiant + mot de passe")
@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

  @Schema(
      description = "Téléphone (ex : 221783703310, avec ou sans +) ou adresse email",
      example = "221783703310")
  @NotBlank(message = "L'identifiant (téléphone ou email) est obligatoire")
  private String username;

  @Schema(description = "Mot de passe du compte", example = "MonMDP@2024")
  @NotBlank(message = "Le mot de passe est obligatoire")
  private String password;

  @Schema(
      description =
          "Type de client – détermine la durée de vie de la session PIN : WEB = 1 an, MOBILE = 1"
              + " an",
      example = "WEB",
      allowableValues = {"WEB", "MOBILE"})
  @NotNull(message = "Le type de client (WEB ou MOBILE) est obligatoire")
  private ClientType clientType;

  @Schema(
      description = "Informations sur l'appareil (optionnel) – ex. \"Chrome 124 / Windows 11\"",
      example = "Chrome 124 / Windows 11")
  private String deviceInfo;
}
