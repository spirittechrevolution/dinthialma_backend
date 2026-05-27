package com.africa.dinthialma_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LogoutRequest {

  @NotBlank(message = "Le refresh token est obligatoire")
  private String refreshToken;
}
