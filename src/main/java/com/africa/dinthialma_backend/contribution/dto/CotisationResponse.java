package com.africa.dinthialma_backend.contribution.dto;

import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.codeList.Ponctualite;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Représentation d'une cotisation (paiement d'un membre pour un cycle)")
@Getter
@Builder
public class CotisationResponse {

  @Schema(description = "UUID de la cotisation")
  private UUID id;

  @Schema(description = "UUID de la tontine parente")
  private UUID tontineId;

  @Schema(description = "Nom de la tontine parente", example = "Tontine Famille Diallo")
  private String tontineNom;

  @Schema(description = "UUID du cycle concerné")
  private UUID cycleId;

  @Schema(description = "Numéro du cycle concerné", example = "5")
  private Integer numeroCycle;

  @Schema(description = "UUID de l'échéance du tour couverte par cette cotisation (B32)")
  private UUID echeanceId;

  @Schema(description = "Rang de l'échéance dans le tour (1 à N)", example = "2")
  private Integer numeroEcheance;

  @Schema(
      description = "Limite de paiement de l'échéance, avec fuseau",
      example = "2026-11-29T20:00:00Z")
  private OffsetDateTime echeanceAt;

  @Schema(description = "Informations du cotisant")
  private MembreInfo membre;

  @Schema(description = "Montant cotisé en FCFA", example = "5000")
  private BigDecimal montant;

  @Schema(description = "Méthode de paiement utilisée", example = "WAVE")
  private String methodePaiement;

  @Schema(description = "Référence de transaction externe", example = "WAVE-TXN-20240701-XYZ123")
  private String referenceTransaction;

  @Schema(
      description = "Statut de la cotisation",
      allowableValues = {"EN_ATTENTE", "VALIDE", "EN_RETARD", "REFUSE"})
  private CotisationStatut statut;

  @Schema(description = "Note libre du membre", example = "Paiement juillet 2024")
  private String note;

  @Schema(description = "Date de validation par l'admin (null si pas encore validé)")
  private LocalDateTime dateValidation;

  @Schema(description = "Utilisateur ayant saisi la cotisation (membre ou admin)")
  private SaisiePar enregistrePar;

  @Schema(description = "Admin qui a validé la cotisation (null si pas encore validé)")
  private ValideurInfo validePar;

  @Schema(
      description = "Motif du refus – présent seulement si statut = REFUSE",
      example = "Transfert non reçu",
      nullable = true)
  private String motifRefus;

  @Schema(description = "Admin ayant refusé la déclaration (null si non refusée)", nullable = true)
  private ValideurInfo refusePar;

  @Schema(description = "Date du refus (null si non refusée)", nullable = true)
  private LocalDateTime dateRefus;

  @Schema(description = "Date d'enregistrement de la cotisation")
  private LocalDateTime createdAt;

  @Schema(
      description = "Date du paiement remis en main propre (encaissement admin), sinon null",
      nullable = true)
  private LocalDateTime datePaiement;

  @Schema(
      description =
          "Ponctualité : payée avant l'échéance du tour ou non (distincte du statut, dont"
              + " EN_RETARD signifie « non validée à la clôture »)",
      allowableValues = {"A_TEMPS", "EN_RETARD", "NON_DECLAREE"})
  private Ponctualite ponctualite;

  @Schema(description = "Date de dernière modification")
  private LocalDateTime updatedAt;

  @Schema(description = "Informations résumées du cotisant")
  @Getter
  @Builder
  public static class MembreInfo {

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
  }

  @Schema(description = "Utilisateur ayant saisi la cotisation (membre ou admin)")
  @Getter
  @Builder
  public static class SaisiePar {

    @Schema(description = "UUID de l'utilisateur")
    private UUID id;

    @Schema(description = "Prénom", example = "Aminata")
    private String firstName;

    @Schema(description = "Nom de famille", example = "Diallo")
    private String lastName;

    @Schema(description = "Numéro de téléphone", example = "221770000001")
    private String phone;
  }

  @Schema(description = "Informations résumées de l'admin ayant validé la cotisation")
  @Getter
  @Builder
  public static class ValideurInfo {

    @Schema(description = "UUID de l'admin valideur")
    private UUID id;

    @Schema(description = "Prénom de l'admin", example = "Mamadou")
    private String firstName;

    @Schema(description = "Nom de famille de l'admin", example = "Diallo")
    private String lastName;
  }
}
