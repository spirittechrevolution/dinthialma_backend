package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
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
            + " tontine et se voit attribuer le rôle DINTHIALMA_ADMIN."
            + " La tontine est créée en statut BROUILLON.")
@Getter
@Setter
public class CreateTontineRequest {

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

  @Schema(description = "Montant de cotisation par membre par cycle en FCFA", example = "5000")
  @NotNull
  @Positive
  private BigDecimal montant;

  @Schema(
      description =
          "Fréquence de cotisation – code de la liste FREQUENCE_TONTINE."
              + " Exemples : HEBDOMADAIRE, MENSUEL, TRIMESTRIEL."
              + " Voir GET /v1/code-list/type/FREQUENCE_TONTINE pour la liste complète.",
      example = "MENSUEL")
  @NotBlank
  private String frequence;

  @Schema(
      description =
          "Ordre de désignation des bénéficiaires – code de la liste ORDRE_BENEFICIAIRE."
              + " Exemples : FIXE (défini à l'avance), ALEATOIRE (tirage au sort)."
              + " Voir GET /v1/code-list/type/ORDRE_BENEFICIAIRE.",
      example = "FIXE")
  @NotBlank
  private String ordreBeneficiaire;

  @Schema(
      description =
          "Mode de gestion des cycles : AUTOMATIQUE = tous les cycles générés à l'activation,"
              + " MANUEL = l'admin ouvre chaque cycle manuellement.",
      example = "AUTOMATIQUE",
      allowableValues = {"AUTOMATIQUE", "MANUEL"})
  @NotNull
  private ModeCycle modeCycle;

  @Schema(
      description = "Date de démarrage du premier cycle (aujourd'hui ou dans le futur)",
      example = "2024-07-01")
  @NotNull
  @FutureOrPresent
  private LocalDate dateDebut;

  @Schema(
      description =
          "Nombre total de membres attendus (min 2, max 100)."
              + " En mode AUTOMATIQUE, détermine le nombre de cycles = ceil(nombreMembres / nombreGagnants).",
      example = "10",
      minimum = "2",
      maximum = "100")
  @Min(2)
  @Max(100)
  private int nombreMembres;

  @Schema(
      description =
          "Nombre de membres qui reçoivent le jackpot à chaque cycle (défaut 1)."
              + " Le jackpot est divisé équitablement entre les gagnants."
              + " Ex : 10 membres, 2 gagnants → 5 cycles, chaque gagnant reçoit jackpot/2.",
      example = "2",
      minimum = "1")
  @Min(1)
  private int nombreGagnants = 1;
}
