package com.africa.dinthialma_backend.contribution.dto;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        "Vue récapitulatif par membre pour un cycle donné."
            + " statutCotisation null = aucune cotisation soumise pour ce cycle.")
@Getter
@Builder
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
      allowableValues = {"EN_ATTENTE", "VALIDE", "EN_RETARD"})
  private CotisationStatut statutCotisation;

  @Schema(description = "Date de validation par l'admin (null si pas encore validé)")
  private LocalDateTime dateValidation;

  @Schema(description = "Date de soumission de la cotisation (null si aucune)")
  private LocalDateTime cotisationCreatedAt;

  @Schema(description = "Utilisateur ayant enregistré la cotisation (null si aucune)")
  private CotisationResponse.SaisiePar enregistrePar;

  public static CycleRecapCotisationResponse from(TontineMembre membre, Cotisation cotisation) {
    var u = membre.getUser();

    CycleRecapCotisationResponseBuilder builder =
        CycleRecapCotisationResponse.builder()
            .membreId(membre.getId())
            .userId(u.getId())
            .firstName(u.getFirstName())
            .lastName(u.getLastName())
            .phone(u.getPhone())
            .accountStatus(u.getAccountStatus())
            .statutMembre(membre.getStatut())
            .ordreJackpot(membre.getOrdreJackpot());

    if (cotisation != null) {
      CotisationResponse.SaisiePar saisieParInfo = null;
      if (cotisation.getEnregistrePar() != null) {
        var s = cotisation.getEnregistrePar();
        saisieParInfo =
            CotisationResponse.SaisiePar.builder()
                .id(s.getId())
                .firstName(s.getFirstName())
                .lastName(s.getLastName())
                .phone(s.getPhone())
                .build();
      }
      builder
          .cotisationId(cotisation.getId())
          .montant(cotisation.getMontant())
          .methodePaiement(cotisation.getMethodePaiement())
          .referenceTransaction(cotisation.getReferenceTransaction())
          .note(cotisation.getNote())
          .statutCotisation(cotisation.getStatut())
          .dateValidation(cotisation.getDateValidation())
          .cotisationCreatedAt(cotisation.getCreatedAt())
          .enregistrePar(saisieParInfo);
    }

    return builder.build();
  }
}
