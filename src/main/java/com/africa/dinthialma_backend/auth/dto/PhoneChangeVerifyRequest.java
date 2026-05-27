package com.africa.dinthialma_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Étape 2 – Vérification OTP pour confirmer le changement de numéro. */
@Getter
@Setter
@NoArgsConstructor
public class PhoneChangeVerifyRequest {

  @NotBlank(message = "Le nouveau numéro de téléphone est obligatoire")
  private String newPhone;

  @NotBlank(message = "Le code OTP est obligatoire")
  private String code;
}
