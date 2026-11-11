package com.africa.dinthialma_backend.me.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Changement de mot de passe d'un utilisateur connecté")
@Getter
@Setter
public class ChangePasswordRequest {

  @Schema(description = "Mot de passe actuel", example = "AncienMDP@2024")
  @NotBlank(message = "L'ancien mot de passe est obligatoire")
  private String ancienMotDePasse;

  @Schema(description = "Nouveau mot de passe (minimum 8 caractères)", example = "NouveauMDP@2026")
  @NotBlank(message = "Le nouveau mot de passe est obligatoire")
  @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
  private String nouveauMotDePasse;

  @Schema(
      description =
          "true = déconnecter aussi les autres appareils (toutes les sessions sauf celle-ci)",
      example = "false")
  private boolean deconnecterAutresAppareils;
}
