package com.africa.dinthialma_backend.admin.dto;

import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.codeList.UniteRythme;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.web.PagedModel;

@Schema(
    description =
        "Tableau de bord personnel d'un administrateur de tontines."
            + " Affiche les métriques de chaque tontine qu'il gère (dont il est le créateur).")
@Getter
@Builder
public class MyDashboardResponse {

  @Schema(
      description = "Nombre total de tontines gérées par cet admin (toutes pages confondues)",
      example = "3")
  private long nombreTontinesGerees;

  // ─── Agrégats globaux (toutes tontines gérées, indépendants de la pagination) ──

  @Schema(
      description = "Somme des cotisations VALIDÉES sur toutes les tontines gérées, en FCFA",
      example = "1250000")
  private BigDecimal montantTotalValide;

  @Schema(
      description = "Nombre total de membres (non supprimés) sur toutes les tontines gérées",
      example = "48")
  private long totalMembres;

  @Schema(
      description = "Nombre total de cotisations EN_ATTENTE sur toutes les tontines gérées",
      example = "7")
  private long totalCotisationsEnAttente;

  @Schema(
      description = "Nombre total de cotisations EN_RETARD sur toutes les tontines gérées",
      example = "2")
  private long totalCotisationsEnRetard;

  @Schema(
      description =
          "Demandes d'adhésion EN_ATTENTE sur toutes mes tontines (badge « À valider »). 0 si aucune.",
      example = "2")
  private long totalDemandesEnAttente;

  @Schema(
      description = "Somme VALIDÉE sur les tours EN_COURS de toutes les tontines gérées, en FCFA",
      example = "40000")
  private BigDecimal totalCollecteToursEnCours;

  @Schema(
      description =
          "Somme attendue sur les tours EN_COURS de toutes les tontines gérées (montant par"
              + " membre × membres ACTIFS), en FCFA. Les tontines à montant libre sans plancher"
              + " sont exclues.",
      example = "60000")
  private BigDecimal totalAttenduToursEnCours;

  @Schema(
      description =
          "Détail paginé par tontine gérée : `{ content: [...], page: { size, number,"
              + " totalElements, totalPages } }`. Pagination :"
              + " `?page=0&size=20&sort=createdAt,desc`")
  private PagedModel<TontineStats> tontines;

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

    @Schema(
        description = "Type de tontine",
        allowableValues = {"ROTATIVE", "EVENEMENTIELLE"})
    private TontineType tontineType;

    @Schema(
        description = "Montant de la cotisation en FCFA (null si événementielle à montant libre)",
        example = "20000",
        nullable = true)
    private BigDecimal montant;

    @Schema(description = "Fréquence de cotisation", example = "MENSUEL")
    private String frequence;

    @Schema(description = "« Tous les N … » (B26)", example = "1")
    private Integer rythmeIntervalle;

    @Schema(description = "Unité du rythme (B26)", example = "MOIS")
    private UniteRythme rythmeUnite;

    @Schema(description = "Jour d'échéance (LUNDI…DIMANCHE, 1…31, DERNIER)", example = "30")
    private String jourEcheance;

    @Schema(description = "Heure limite de paiement HH:mm (Dakar)", example = "23:59")
    private String heureEcheance;

    @Schema(description = "Date de début de la tontine", example = "2026-01-01")
    private LocalDate dateDebut;

    @Schema(
        description = "Date d'échéance – événementielle uniquement, null sinon",
        example = "2026-06-16",
        nullable = true)
    private LocalDate dateEcheance;

    @Schema(
        description = "Nom de l'événement – événementielle uniquement, null sinon",
        example = "Tabaski 2026",
        nullable = true)
    private String nomEvenement;

    @Schema(
        description = "Montant libre – événementielle uniquement, null sinon",
        example = "false",
        nullable = true)
    private Boolean montantLibre;

    @Schema(
        description =
            "Événementielle uniquement (null sinon) : nombre de membres ayant au moins une"
                + " cotisation VALIDÉE",
        example = "9",
        nullable = true)
    private Long nombreMembresCotise;

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

    @Schema(
        description =
            "ROTATIVE à ordre MANUEL : au moins un tour TERMINÉ attend la désignation de ses"
                + " gagnants",
        example = "false")
    private boolean gagnantsADesigner;

    @Schema(
        description = "Demandes d'adhésion EN_ATTENTE sur cette tontine. 0 si aucune.",
        example = "1")
    private long demandesEnAttente;

    @Schema(
        description =
            "Demandes de priorité sur le jackpot EN_ATTENTE (à accepter ou refuser). 0 si aucune.",
        example = "0")
    private long prioritesEnAttente;
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
        description = "Limite de paiement du tour (B26), avec fuseau",
        example = "2026-11-30T23:59:00Z")
    private OffsetDateTime echeanceAt;

    @Schema(
        description = "Prénom + nom du bénéficiaire désigné – null si pas encore désigné",
        example = "Fatou Sow")
    private String beneficiaireNom;

    @Schema(description = "UUID de l'utilisateur bénéficiaire")
    private UUID beneficiaireUserId;

    @Schema(description = "Somme des cotisations VALIDÉES sur ce tour, en FCFA", example = "40000")
    private BigDecimal montantCollecte;

    @Schema(
        description = "Somme des cotisations EN_ATTENTE de validation sur ce tour, en FCFA",
        example = "5000")
    private BigDecimal montantEnAttente;

    @Schema(
        description =
            "Montant attendu sur ce tour (montant par membre × membres ACTIFS), en FCFA – null si"
                + " montant libre sans plancher",
        example = "60000",
        nullable = true)
    private BigDecimal montantAttendu;

    @Schema(description = "Membres dont la cotisation du tour est VALIDÉE", example = "8")
    private long membresAyantPaye;

    @Schema(
        description = "Membres ayant déclaré un paiement EN_ATTENTE de validation",
        example = "1")
    private long membresEnAttente;

    @Schema(description = "Membres ACTIFS de la tontine (base du montant attendu)", example = "12")
    private long membresActifs;
  }
}
