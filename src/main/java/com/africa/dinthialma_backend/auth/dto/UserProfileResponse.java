package com.africa.dinthialma_backend.auth.dto;

import com.africa.dinthialma_backend.auth.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "Profil complet de l'utilisateur connecté")
@Getter
@AllArgsConstructor
public class UserProfileResponse {

  @Schema(description = "UUID interne de l'utilisateur")
  private UUID id;

  @Schema(description = "Prénom", example = "Mamadou")
  private String firstName;

  @Schema(description = "Nom de famille", example = "Diallo")
  private String lastName;

  @Schema(description = "Numéro de téléphone normalisé (sans le +)", example = "221783703310")
  private String phone;

  @Schema(description = "Adresse email (peut être null)", example = "mamadou@email.com")
  private String email;

  @Schema(description = "URL de l'avatar (peut être null)")
  private String avatarUrl;

  @Schema(description = "Compte actif ou désactivé", example = "true")
  private boolean active;

  @Schema(description = "Un code PIN a été configuré sur ce compte", example = "true")
  private boolean pinConfigured;

  @Schema(
      description = "Le PIN est expiré (> 90 jours) et doit être reconfiguré",
      example = "false")
  private boolean pinExpired;

  @Schema(
      description = "Rôles Keycloak de l'utilisateur",
      example = "[\"DINTHIALMA_USER\", \"DINTHIALMA_ADMIN\"]")
  private Set<String> roles;

  @Schema(description = "Date de création du compte")
  private LocalDateTime createdAt;

  public static UserProfileResponse from(User user) {
    boolean pinExpired = false;
    if (user.getPinCreatedAt() != null) {
      pinExpired =
          user.getPinCreatedAt()
              .plusDays(com.africa.dinthialma_backend.common.constants.Constants.Pin.EXPIRY_DAYS)
              .isBefore(LocalDateTime.now());
    }

    Set<String> roleNames =
        user.getRoles().stream()
            .map(r -> r.getRole().getKeycloakRole())
            .collect(Collectors.toSet());

    return new UserProfileResponse(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getPhone(),
        user.getEmail(),
        user.getAvatarUrl(),
        user.isActive(),
        user.getPinHash() != null,
        pinExpired,
        roleNames,
        user.getCreatedAt());
  }
}
