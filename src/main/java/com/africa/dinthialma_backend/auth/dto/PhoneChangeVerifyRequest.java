package com.africa.dinthialma_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
    description =
        "Étape 2/2 du changement de numéro de téléphone."
            + " Vérifie l'OTP et applique le changement en DB et dans Keycloak."
            + " Toutes les sessions actives sont révoquées – reconnexion obligatoire.")
@Getter
@Setter
@NoArgsConstructor
public class PhoneChangeVerifyRequest {

  @Schema(
      description = "Nouveau numéro de téléphone au format international",
      example = "+221701234567")
  @NotBlank(message = "Le nouveau numéro de téléphone est obligatoire")
  private String newPhone;

  @Schema(
      description = "Code OTP à 6 chiffres reçu par SMS sur le nouveau numéro",
      example = "789012")
  @NotBlank(message = "Le code OTP est obligatoire")
  private String code;
}
