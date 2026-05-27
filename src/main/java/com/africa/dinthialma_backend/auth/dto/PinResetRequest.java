package com.africa.dinthialma_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Requête de réinitialisation du code PIN via OTP. */
@Getter
@Setter
@NoArgsConstructor
public class PinResetRequest {

  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  private String phone;

  @NotBlank(message = "L'OTP est obligatoire")
  private String otp;

  @NotBlank(message = "Le nouveau code PIN est obligatoire")
  @Pattern(regexp = "\\d{6}", message = "Le code PIN doit contenir exactement 6 chiffres")
  private String newPin;

  @NotBlank(message = "La confirmation du code PIN est obligatoire")
  @Pattern(regexp = "\\d{6}", message = "La confirmation doit contenir exactement 6 chiffres")
  private String confirmPin;
}
