package com.africa.dinthialma_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
    description =
        "Requête d'envoi d'OTP par SMS. Utilisée pour l'inscription, le reset mot de passe et le"
            + " reset PIN.")
@Getter
@Setter
@NoArgsConstructor
public class SendOtpRequest {

  @Schema(
      description = "Numéro de téléphone au format international (indicatif pays inclus)",
      example = "+221783703310")
  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  @Pattern(
      regexp = "^\\+[1-9]\\d{7,14}$",
      message = "Le numéro de téléphone doit être au format international (ex: +221700000000)")
  private String phone;
}
