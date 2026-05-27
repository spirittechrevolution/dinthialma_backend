package com.africa.dinthialma_backend.auth.dto;

import com.africa.dinthialma_backend.auth.codeList.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

  /**
   * Type de client – détermine la durée de vie de la session PIN créée après le login.
   *
   * <ul>
   *   <li>{@code WEB} → TTL 24 h
   *   <li>{@code MOBILE} → TTL 30 jours
   * </ul>
   */
  @NotNull(message = "Le type de client (WEB ou MOBILE) est obligatoire")
  private ClientType clientType;

  /** Informations sur l'appareil (optionnel) – ex. {@code "Chrome 124 / Windows 11"}. */
  private String deviceInfo;
}
