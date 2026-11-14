package com.africa.dinthialma_backend.contribution.dto;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.codeList.Ponctualite;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * Vue récapitulatif des cotisations d'un cycle par membre.
 *
 * <p>Un enregistrement par membre actif/suspendu de la tontine. {@code statutCotisation} est {@code
 * null} quand le membre n'a soumis aucune cotisation pour le cycle concerné.
 */
@Schema(
    description =
        "Vue récapitulatif par membre pour un cycle donné. Les champs de cotisation portent sur"
            + " l'échéance de référence (echeanceId : celle du filtre, sinon l'échéance en cours) ;"
            + " statutCotisation null = aucune cotisation soumise pour cette échéance. Le détail"
            + " par échéance est dans echeances (B32).")
@Getter
@Builder(toBuilder = true)
public class CycleRecapCotisationResponse {

  // ─── Membre ───────────────────────────────────────────────────────────────

  @Schema(description = "UUID du membership (tontine_membres.id)")
  private UUID membreId;

  @Schema(description = "UUID de l'utilisateur")
  private UUID userId;

  @Schema(description = "Prénom", example = "Fatou")
  private String firstName;

  @Schema(description = "Nom de famille", example = "Sow")
  private String lastName;

  @Schema(description = "Numéro de téléphone normalisé", example = "221770000001")
  private String phone;

  @Schema(description = "Statut du compte utilisateur (ACTIVE ou PRE_ENROLLED)")
  private AccountStatus accountStatus;

  @Schema(description = "Statut du membre dans la tontine (ACTIF, SUSPENDU)")
  private MembreStatut statutMembre;

  @Schema(description = "Ordre de passage au jackpot (null si non défini)")
  private Integer ordreJackpot;

  // ─── Cotisation ───────────────────────────────────────────────────────────

  @Schema(description = "UUID de la cotisation (null si aucune cotisation soumise)")
  private UUID cotisationId;

  @Schema(description = "Montant cotisé en FCFA (null si aucune cotisation)")
  private BigDecimal montant;

  @Schema(description = "Méthode de paiement (null si aucune cotisation)", example = "WAVE")
  private String methodePaiement;

  @Schema(description = "Référence de transaction (null si aucune cotisation)")
  private String referenceTransaction;

  @Schema(description = "Note libre (null si aucune cotisation)")
  private String note;

  @Schema(
      description = "Statut de la cotisation. null = aucune cotisation soumise pour ce cycle.",
      allowableValues = {"EN_ATTENTE", "VALIDE", "EN_RETARD", "REFUSE"})
  private CotisationStatut statutCotisation;

  @Schema(
      description =
          "Ponctualité du membre sur ce tour : A_TEMPS, EN_RETARD ou NON_DECLAREE (aucune"
              + " déclaration retenue)",
      allowableValues = {"A_TEMPS", "EN_RETARD", "NON_DECLAREE"})
  private Ponctualite ponctualite;

  @Schema(description = "Date de validation par l'admin (null si pas encore validé)")
  private LocalDateTime dateValidation;

  @Schema(
      description =
          "Motif du refus – présent seulement si la seule déclaration du cycle est REFUSE",
      nullable = true)
  private String motifRefus;

  @Schema(description = "Date de soumission de la cotisation (null si aucune)")
  private LocalDateTime cotisationCreatedAt;

  @Schema(description = "Utilisateur ayant enregistré la cotisation (null si aucune)")
  private CotisationResponse.SaisiePar enregistrePar;

  // ─── Échéances du tour (B32) ──────────────────────────────────────────────

  @Schema(description = "Échéance de référence des champs de cotisation ci-dessus", nullable = true)
  private UUID echeanceId;

  @Schema(description = "Rang de l'échéance de référence (1 à N)", example = "2", nullable = true)
  private Integer numeroEcheance;

  @Schema(description = "Limite de paiement de l'échéance de référence, avec fuseau")
  private OffsetDateTime echeanceAt;

  @Schema(description = "Nombre d'échéances du tour", example = "4")
  private int nombreEcheances;

  @Schema(
      description = "Échéances déjà ouvertes au paiement (dues à ce jour, en cours comprise)",
      example = "2")
  private int echeancesDues;

  @Schema(description = "Échéances couvertes par une cotisation VALIDE", example = "1")
  private int echeancesPayees;

  @Schema(description = "Détail par échéance du tour, dans l'ordre")
  private List<EcheanceRecap> echeances;

  /** Situation du membre sur une échéance du tour. */
  @Getter
  @Builder
  @Schema(description = "Situation du membre sur une échéance du tour")
  public static class EcheanceRecap {

    @Schema(description = "UUID de l'échéance")
    private UUID echeanceId;

    @Schema(description = "Rang dans le tour (1 à N)", example = "1")
    private int numero;

    @Schema(description = "Limite de paiement, avec fuseau")
    private OffsetDateTime echeanceAt;

    @Schema(description = "Échéance ouverte au paiement (due à ce jour)")
    private boolean ouverte;

    @Schema(description = "Cotisation retenue sur l'échéance (null si aucune)", nullable = true)
    private UUID cotisationId;

    @Schema(
        description = "Statut de la cotisation (null = aucune)",
        allowableValues = {"EN_ATTENTE", "VALIDE", "EN_RETARD", "REFUSE"},
        nullable = true)
    private CotisationStatut statutCotisation;

    @Schema(description = "Montant (null si aucune cotisation)", nullable = true)
    private BigDecimal montant;

    @Schema(description = "Ponctualité sur cette échéance")
    private Ponctualite ponctualite;
  }
}
