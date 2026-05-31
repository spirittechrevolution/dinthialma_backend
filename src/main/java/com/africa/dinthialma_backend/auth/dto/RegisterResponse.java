package com.africa.dinthialma_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Réponse après inscription réussie")
@Getter
@Setter
@AllArgsConstructor
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
}
