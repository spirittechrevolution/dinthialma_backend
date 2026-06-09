package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête de création d'une tontine."
            + " L'utilisateur appelant devient automatiquement l'administrateur (créateur) de la"
            + " tontine. La tontine est créée en statut BROUILLON.\n\n"
            + "**ROTATIVE** : ordreBeneficiaire, nombreMembres, modeCycle et montant sont"
            + " obligatoires.\n"
            + "**EVENEMENTIELLE** : dateEcheance est obligatoire ; montant est obligatoire si"
            + " montantLibre=false ; ordreBeneficiaire et nombreMembres sont ignorés.")
@Getter
@Setter
public class CreateTontineRequest {

  @Schema(
      description = "Type de tontine",
      example = "ROTATIVE",
      allowableValues = {"ROTATIVE", "EVENEMENTIELLE"})
  @NotNull
  private TontineType tontineType = TontineType.ROTATIVE;

  @Schema(
      description = "Nom du groupe de tontine (max 150 caractères)",
      example = "Tontine Famille Diallo")
  @NotBlank
  @Size(max = 150)
  private String nom;

  @Schema(
      description = "Description optionnelle du groupe",
      example = "Tontine mensuelle de la famille Diallo")
  private String description;

  @Schema(description = "Montant fixe de cotisation par cycle en FCFA", example = "5000")
  @Positive
  private BigDecimal montant;

  @Schema(
      description =
          "Fréquence de cotisation – code de la liste FREQUENCE_TONTINE."
              + " Exemples : JOURNALIERE, HEBDOMADAIRE, BIMENSUEL, MENSUEL, TRIMESTRIEL.",
      example = "MENSUEL")
  @NotBlank
  private String frequence;

  @Schema(
      description =
          "Ordre de désignation des bénéficiaires (ROTATIVE uniquement)."
              + " Codes ORDRE_BENEFICIAIRE : FIXE, ALEATOIRE, ROTATION, MANUEL.",
      example = "FIXE")
  private String ordreBeneficiaire;

  @Schema(
      description =
          "Mode de gestion des cycles (ROTATIVE uniquement)."
              + " AUTOMATIQUE = cycles générés à l'activation, MANUEL = l'admin ouvre chaque cycle.",
      example = "AUTOMATIQUE",
      allowableValues = {"AUTOMATIQUE", "MANUEL"})
  private ModeCycle modeCycle;

  @Schema(
      description = "Date de démarrage du premier cycle (aujourd'hui ou dans le futur)",
      example = "2024-07-01")
  @NotNull
  @FutureOrPresent
  private LocalDate dateDebut;

  @Schema(
      description =
          "Nombre total de membres attendus (ROTATIVE uniquement, min 2, max 100)."
              + " Détermine le nombre de cycles en mode AUTOMATIQUE.",
      example = "10",
      minimum = "2",
      maximum = "100")
  @Min(0)
  private Integer nombreMembres;

  @Schema(
      description =
          "Nombre de membres qui reçoivent le jackpot à chaque cycle (ROTATIVE, défaut 1).",
      example = "2",
      minimum = "1")
  @Min(1)
  private int nombreGagnants = 1;

  // ─── Champs EVENEMENTIELLE ──────────────────────────────────────────────

  @Schema(
      description =
          "Date cible de l'événement (EVENEMENTIELLE obligatoire)."
              + " Exemples : Tabaski, Korité, mariage, baptême.",
      example = "2026-03-20")
  private LocalDate dateEcheance;

  @Schema(
      description = "Nom de l'événement (optionnel, max 200 caractères)",
      example = "Tabaski 2026")
  @Size(max = 200)
  private String nomEvenement;

  @Schema(
      description =
          "Si true, chaque membre cotise librement le montant qu'il souhaite."
              + " Si false (défaut), le champ montant est imposé à tous.",
      example = "false")
  private boolean montantLibre = false;

  @Schema(
      description =
          "Montant minimum de cotisation (EVENEMENTIELLE + montantLibre=true uniquement)."
              + " Null = aucun plancher.",
      example = "500")
  @Positive
  private BigDecimal montantMinimum;
}
