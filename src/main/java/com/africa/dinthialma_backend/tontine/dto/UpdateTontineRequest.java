package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête de mise à jour d'une tontine."
            + " Tous les champs sont optionnels – seuls les champs non nuls sont mis à jour."
            + " Modification autorisée uniquement si la tontine est en statut BROUILLON.")
@Getter
@Setter
public class UpdateTontineRequest {

  @Schema(
      description = "Nouveau nom du groupe (2 à 150 caractères)",
      example = "Tontine Famille Diallo 2024")
  @Size(min = 2, max = 150)
  private String nom;

  @Schema(
      description = "Nouvelle description (null = inchangée)",
      example = "Tontine mensuelle mise à jour")
  private String description;

  @Schema(
      description = "Nouveau montant de cotisation par cycle en FCFA (doit être positif)",
      example = "10000")
  @Positive
  private BigDecimal montant;

  @Schema(
      description = "Nouvelle fréquence (code FREQUENCE_TONTINE, null = inchangée)",
      example = "HEBDOMADAIRE")
  private String frequence;

  @Schema(
      description = "Nouvel ordre des bénéficiaires (code ORDRE_BENEFICIAIRE, null = inchangé)",
      example = "ALEATOIRE")
  private String ordreBeneficiaire;

  @Schema(description = "Nouvelle date de démarrage (null = inchangée)", example = "2024-08-01")
  private LocalDate dateDebut;

  @Schema(
      description = "Nouveau nombre de membres attendus (min 2, max 100, null = inchangé)",
      example = "15",
      minimum = "2",
      maximum = "100")
  @Min(2)
  @Max(100)
  private Integer nombreMembres;

  @Schema(
      description = "Nouveau nombre de gagnants par cycle (min 1, null = inchangé)",
      example = "2",
      minimum = "1")
  @Min(1)
  private Integer nombreGagnants;
}
