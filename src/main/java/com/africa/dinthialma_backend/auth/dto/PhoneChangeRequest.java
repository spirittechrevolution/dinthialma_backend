package com.africa.dinthialma_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
    description =
        "Étape 1/2 du changement de numéro de téléphone."
            + " Un OTP est envoyé au NOUVEAU numéro pour confirmer qu'il appartient à l'utilisateur."
            + " Les tontines et memberships sont conservés (liés à l'UUID, pas au numéro).")
@Getter
@Setter
@NoArgsConstructor
public class PhoneChangeRequest {

  @Schema(
      description = "Nouveau numéro de téléphone au format international",
      example = "+221701234567")
  @NotBlank(message = "Le nouveau numéro de téléphone est obligatoire")
  private String newPhone;
}
