package com.africa.dinthialma_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
    description =
        "Requête de réinitialisation du mot de passe (étape 3/3)."
            + " Le téléphone doit correspondre au numéro ayant reçu l'OTP vérifié.")
@Getter
@Setter
@NoArgsConstructor
public class ResetPasswordByPhoneRequest {

  @Schema(description = "Numéro de téléphone au format international", example = "+221783703310")
  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Format international requis")
  private String phone;

  @Schema(description = "Code OTP à 6 chiffres reçu par SMS et déjà vérifié", example = "123456")
  @NotBlank(message = "Le code OTP est obligatoire")
  @Pattern(regexp = "^\\d{6}$", message = "Le code OTP doit contenir 6 chiffres")
  private String code;

  @Schema(description = "Nouveau mot de passe (minimum 8 caractères)", example = "NouveauMDP@2024")
  @NotBlank(message = "Le nouveau mot de passe est obligatoire")
  @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
  private String newPassword;
}
