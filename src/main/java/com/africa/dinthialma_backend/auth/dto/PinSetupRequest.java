package com.africa.dinthialma_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
    description =
        "Requête de configuration initiale du code PIN à 6 chiffres."
            + " Nécessite un JWT valide (connexion mot de passe préalable).")
@Getter
@Setter
@NoArgsConstructor
public class PinSetupRequest {

  @Schema(description = "Code PIN à 6 chiffres", example = "123456")
  @NotBlank(message = "Le code PIN est obligatoire")
  @Pattern(regexp = "\\d{6}", message = "Le code PIN doit contenir exactement 6 chiffres")
  private String pin;

  @Schema(description = "Confirmation du code PIN (doit être identique à pin)", example = "123456")
  @NotBlank(message = "La confirmation du code PIN est obligatoire")
  @Pattern(regexp = "\\d{6}", message = "La confirmation doit contenir exactement 6 chiffres")
  private String confirmPin;
}
