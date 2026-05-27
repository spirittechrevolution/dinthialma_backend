package com.africa.dinthialma_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResetPasswordByPhoneRequest {

  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Format international requis")
  private String phone;

  @NotBlank(message = "Le code OTP est obligatoire")
  @Pattern(regexp = "^\\d{6}$", message = "Le code OTP doit contenir 6 chiffres")
  private String code;

  @NotBlank(message = "Le nouveau mot de passe est obligatoire")
  @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
  private String newPassword;
}
