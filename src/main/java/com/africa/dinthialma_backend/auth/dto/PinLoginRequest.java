package com.africa.dinthialma_backend.auth.dto;

import com.africa.dinthialma_backend.auth.codeList.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Requête de connexion rapide par code PIN (WEB ou MOBILE). */
@Getter
@Setter
@NoArgsConstructor
public class PinLoginRequest {

  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  private String phone;

  @NotBlank(message = "Le code PIN est obligatoire")
  @Pattern(regexp = "\\d{6}", message = "Le code PIN doit contenir exactement 6 chiffres")
  private String pin;

  @NotNull(message = "Le type de client est obligatoire (WEB ou MOBILE)")
  private ClientType clientType;

  /**
   * Informations sur l'appareil.
   *
   * <ul>
   *   <li>WEB : user-agent du navigateur (header HTTP, optionnel)
   *   <li>MOBILE : device_id fourni par l'application (optionnel en MVP)
   * </ul>
   */
  private String deviceInfo;
}
