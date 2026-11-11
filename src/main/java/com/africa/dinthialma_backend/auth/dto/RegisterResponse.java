package com.africa.dinthialma_backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Réponse après inscription réussie")
@Getter
@Setter
@NoArgsConstructor
public class RegisterResponse {

  @Schema(description = "UUID interne de l'utilisateur en base Dinthialma")
  private UUID userId;

  @Schema(description = "Identifiant Keycloak de l'utilisateur")
  private String keycloakId;

  @Schema(description = "Numéro de téléphone normalisé (sans le +)", example = "221783703310")
  private String phone;

  @Schema(description = "Prénom", example = "Mamadou")
  private String firstName;

  @Schema(description = "Nom de famille", example = "Diallo")
  private String lastName;

  @Schema(description = "Adresse email (peut être null si non fournie)")
  private String email;

  @Schema(
      description = "Rôle Keycloak attribué automatiquement à l'inscription",
      example = "DINTHIALMA_USER")
  private String role;

  /**
   * Jetons de la connexion automatique (si {@code clientType} a été fourni et que la connexion a
   * réussi), aplatis au premier niveau : {@code access_token}, {@code refresh_token}, {@code
   * expires_in}, {@code pin_configured}… exactement comme la réponse de {@code /login}. Absents
   * sinon : le front redirige alors vers l'écran de connexion.
   */
  @JsonUnwrapped private LoginResponse tokens;

  public RegisterResponse(
      UUID userId,
      String keycloakId,
      String phone,
      String firstName,
      String lastName,
      String email,
      String role) {
    this.userId = userId;
    this.keycloakId = keycloakId;
    this.phone = phone;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.role = role;
  }
}
