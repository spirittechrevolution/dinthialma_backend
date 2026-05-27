package com.africa.dinthialma_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ForgotPasswordRequest {

  @NotBlank(message = "L'email est obligatoire")
  @Email(message = "Format email invalide")
  private String email;
}
