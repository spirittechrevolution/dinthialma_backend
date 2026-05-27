package com.africa.dinthialma_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Requête de configuration initiale du code PIN. */
@Getter
@Setter
@NoArgsConstructor
public class PinSetupRequest {

  @NotBlank(message = "Le code PIN est obligatoire")
  @Pattern(regexp = "\\d{6}", message = "Le code PIN doit contenir exactement 6 chiffres")
  private String pin;

  @NotBlank(message = "La confirmation du code PIN est obligatoire")
  @Pattern(regexp = "\\d{6}", message = "La confirmation doit contenir exactement 6 chiffres")
  private String confirmPin;
}
