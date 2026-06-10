package com.africa.dinthialma_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
    description =
        "Requête de réinitialisation du code PIN via OTP."
            + " Étape 3/3 du flux reset PIN : envoi OTP → vérification OTP → ce body.")
@Getter
@Setter
@NoArgsConstructor
public class PinResetRequest {

  @Schema(
      description = "Numéro de téléphone (doit correspondre au numéro ayant reçu l'OTP)",
      example = "+221783703310")
  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  private String phone;

  @Schema(description = "Code OTP reçu par SMS (6 chiffres)", example = "654321")
  @NotBlank(message = "L'OTP est obligatoire")
  private String otp;

  @Schema(description = "Nouveau code PIN à 6 chiffres", example = "654321")
  @NotBlank(message = "Le nouveau code PIN est obligatoire")
  @Pattern(regexp = "\\d{6}", message = "Le code PIN doit contenir exactement 6 chiffres")
  private String newPin;

  @Schema(description = "Confirmation du nouveau code PIN", example = "654321")
  @NotBlank(message = "La confirmation du code PIN est obligatoire")
  @Pattern(regexp = "\\d{6}", message = "La confirmation doit contenir exactement 6 chiffres")
  private String confirmPin;
}
