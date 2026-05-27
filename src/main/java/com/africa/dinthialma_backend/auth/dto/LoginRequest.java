package com.africa.dinthialma_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

  /**
   * Identifiant de l'utilisateur : numéro de téléphone (avec ou sans {@code +}) ou adresse email.
   *
   * <p>Exemples valides :
   *
   * <ul>
   *   <li>{@code 221783703310}
   *   <li>{@code +221783703310} ← le {@code +} est retiré automatiquement côté service
   *   <li>{@code admin@dinthialma.io}
   * </ul>
   */
  @NotBlank(message = "L'identifiant (téléphone ou email) est obligatoire")
  private String username;

  @NotBlank(message = "Le mot de passe est obligatoire")
  private String password;
}
