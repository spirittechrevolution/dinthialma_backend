package com.africa.dinthialma_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Requête de vérification d'un code OTP reçu par SMS")
@Getter
@Setter
@NoArgsConstructor
public class VerifyOtpRequest {

  @Schema(description = "Numéro de téléphone au format international", example = "+221783703310")
  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Format international requis")
  private String phone;

  @Schema(description = "Code OTP à 6 chiffres reçu par SMS", example = "123456")
  @NotBlank(message = "Le code OTP est obligatoire")
  @Pattern(regexp = "^\\d{6}$", message = "Le code OTP doit contenir 6 chiffres")
  private String code;
}
