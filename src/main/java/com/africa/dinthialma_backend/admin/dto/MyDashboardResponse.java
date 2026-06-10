package com.africa.dinthialma_backend.admin.dto;

import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(
    description =
        "Tableau de bord personnel d'un administrateur de tontines."
            + " Affiche les métriques de chaque tontine qu'il gère (dont il est le créateur).")
@Getter
@Builder
public class MyDashboardResponse {

  @Schema(description = "Nombre total de tontines gérées par cet admin", example = "3")
  private int nombreTontinesGerees;

  @Schema(description = "Détail par tontine gérée")
  private List<TontineStats> tontines;

  @Schema(description = "Métriques d'une tontine gérée par l'admin")
  @Getter
  @Builder
  public static class TontineStats {

    @Schema(description = "UUID de la tontine")
    private UUID tontineId;

    @Schema(description = "Nom de la tontine", example = "Tontine Famille Diallo")
    private String nom;

    @Schema(
        description = "Statut courant",
        allowableValues = {"BROUILLON", "ACTIVE", "SUSPENDUE", "TERMINEE"})
    private TontineStatut statut;

    @Schema(description = "Nombre total de membres attendus", example = "12")
    private int nombreMembres;

    @Schema(
        description = "Nombre de cotisations EN_ATTENTE de validation dans cette tontine",
        example = "4")
    private long cotisationsEnAttente;

    @Schema(description = "Nombre de cotisations EN_RETARD dans cette tontine", example = "1")
    private long cotisationsEnRetard;

    @Schema(
        description = "Somme totale des cotisations VALIDÉES dans cette tontine en FCFA",
        example = "240000")
    private BigDecimal montantTotalValide;

    @Schema(description = "Cycle actuellement EN_COURS – null si aucun")
    private CycleEnCoursInfo cycleEnCours;
  }

  @Schema(description = "Informations résumées du cycle EN_COURS d'une tontine")
  @Getter
  @Builder
  public static class CycleEnCoursInfo {

    @Schema(description = "UUID du cycle")
    private UUID cycleId;

    @Schema(description = "Numéro du cycle dans la séquence", example = "5")
    private int numeroCycle;

    @Schema(description = "Date de début du cycle", example = "2024-07-01")
    private LocalDate dateDebut;

    @Schema(description = "Date de fin prévue du cycle", example = "2024-07-31")
    private LocalDate dateFin;

    @Schema(
        description = "Prénom + nom du bénéficiaire désigné – null si pas encore désigné",
        example = "Fatou Sow")
    private String beneficiaireNom;

    @Schema(description = "UUID de l'utilisateur bénéficiaire")
    private UUID beneficiaireUserId;
  }
}
