package com.africa.dinthialma_backend.admin.dto;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(
    description =
        "Vue détaillée d'un utilisateur pour l'interface d'administration (SUPER_ADMIN)."
            + " Utilisé dans la liste paginée et le détail individuel.")
@Getter
@Builder
public class AdminUserResponse {

  @Schema(description = "UUID interne de l'utilisateur")
  private UUID id;

  @Schema(description = "Identifiant Keycloak de l'utilisateur")
  private String keycloakId;

  @Schema(description = "Prénom", example = "Mamadou")
  private String firstName;

  @Schema(description = "Nom de famille", example = "Diallo")
  private String lastName;

  @Schema(description = "Numéro de téléphone normalisé (sans le +)", example = "221783703310")
  private String phone;

  @Schema(description = "Adresse email (peut être null)")
  private String email;

  @Schema(description = "URL de l'avatar (peut être null)")
  private String avatarUrl;

  @Schema(description = "Compte actif ou désactivé", example = "true")
  private boolean active;

  @Schema(
      description =
          "Statut d'activation du compte. PRE_ENROLLED = ajouté par un admin de tontine, pas"
              + " encore inscrit sur l'application.",
      allowableValues = {"ACTIVE", "PRE_ENROLLED"})
  private AccountStatus accountStatus;

  @Schema(description = "Un code PIN a été configuré sur ce compte", example = "true")
  private boolean pinConfigured;

  @Schema(
      description = "Liste des rôles Keycloak triés alphabétiquement",
      example = "[\"DINTHIALMA_ADMIN\", \"DINTHIALMA_USER\"]")
  private List<String> roles;

  @Schema(description = "Date de création du compte")
  private LocalDateTime createdAt;

  @Schema(description = "Date de dernière modification")
  private LocalDateTime updatedAt;
}
