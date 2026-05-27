package com.africa.dinthialma_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Étape 1 – Demande de changement de numéro de téléphone. OTP envoyé au nouveau numéro. */
@Getter
@Setter
@NoArgsConstructor
public class PhoneChangeRequest {

  @NotBlank(message = "Le nouveau numéro de téléphone est obligatoire")
  private String newPhone;
}
