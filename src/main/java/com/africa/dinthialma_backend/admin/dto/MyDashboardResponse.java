package com.africa.dinthialma_backend.admin.dto;

import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * Tableau de bord d'un administrateur de tontines (créateur).
 *
 * <p>Affiche les métriques de chaque tontine qu'il gère : cotisations en attente, en retard, cycle
 * en cours et prochain bénéficiaire.
 */
@Getter
@Builder
public class MyDashboardResponse {

  /** Nombre total de tontines gérées par cet admin. */
  private int nombreTontinesGerees;

  /** Détail par tontine. */
  private List<TontineStats> tontines;

  /** Métriques d'une tontine gérée par l'admin. */
  @Getter
  @Builder
  public static class TontineStats {
    private UUID tontineId;
    private String nom;
    private TontineStatut statut;
    private int nombreMembres;

    /** Cotisations EN_ATTENTE de validation dans cette tontine. */
    private long cotisationsEnAttente;

    /** Cotisations marquées EN_RETARD dans cette tontine. */
    private long cotisationsEnRetard;

    /** Montant total des cotisations VALIDÉES dans cette tontine. */
    private BigDecimal montantTotalValide;

    /** Cycle actuellement EN_COURS – null si aucun. */
    private CycleEnCoursInfo cycleEnCours;
  }

  /** Informations résumées du cycle EN_COURS d'une tontine. */
  @Getter
  @Builder
  public static class CycleEnCoursInfo {
    private UUID cycleId;
    private int numeroCycle;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    /** Prénom + nom du bénéficiaire désigné – null si pas encore désigné. */
    private String beneficiaireNom;

    private UUID beneficiaireUserId;
  }
}
