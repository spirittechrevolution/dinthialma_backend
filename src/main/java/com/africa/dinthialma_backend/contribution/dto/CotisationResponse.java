package com.africa.dinthialma_backend.contribution.dto;

import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

  @Schema(description = "UUID du cycle concerné")
  private UUID cycleId;

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
      allowableValues = {"EN_ATTENTE", "VALIDE", "EN_RETARD"})
  private CotisationStatut statut;

  @Schema(description = "Note libre du membre", example = "Paiement juillet 2024")
  private String note;

  @Schema(description = "Date de validation par l'admin (null si pas encore validé)")
  private LocalDateTime dateValidation;

  @Schema(description = "Utilisateur ayant saisi la cotisation (membre ou admin)")
  private SaisiePar enregistrePar;

  @Schema(description = "Admin qui a validé la cotisation (null si pas encore validé)")
  private ValideurInfo validePar;

  @Schema(description = "Date d'enregistrement de la cotisation")
  private LocalDateTime createdAt;

  @Schema(description = "Date de dernière modification")
  private LocalDateTime updatedAt;

  public static CotisationResponse from(Cotisation cotisation) {
    MembreInfo membreInfo = null;
    if (cotisation.getMembre() != null && cotisation.getMembre().getUser() != null) {
      var m = cotisation.getMembre();
      var u = m.getUser();
      membreInfo =
          MembreInfo.builder()
              .membreId(m.getId())
              .userId(u.getId())
              .firstName(u.getFirstName())
              .lastName(u.getLastName())
              .phone(u.getPhone())
              .build();
    }

    SaisiePar saisieParInfo = null;
    if (cotisation.getEnregistrePar() != null) {
      var s = cotisation.getEnregistrePar();
      saisieParInfo =
          SaisiePar.builder()
              .id(s.getId())
              .firstName(s.getFirstName())
              .lastName(s.getLastName())
              .phone(s.getPhone())
              .build();
    }

    ValideurInfo valideurInfo = null;
    if (cotisation.getValidePar() != null) {
      var v = cotisation.getValidePar();
      valideurInfo =
          ValideurInfo.builder()
              .id(v.getId())
              .firstName(v.getFirstName())
              .lastName(v.getLastName())
              .build();
    }

    return CotisationResponse.builder()
        .id(cotisation.getId())
        .tontineId(cotisation.getTontine() != null ? cotisation.getTontine().getId() : null)
        .cycleId(cotisation.getCycle() != null ? cotisation.getCycle().getId() : null)
        .membre(membreInfo)
        .montant(cotisation.getMontant())
        .methodePaiement(cotisation.getMethodePaiement())
        .referenceTransaction(cotisation.getReferenceTransaction())
        .statut(cotisation.getStatut())
        .note(cotisation.getNote())
        .enregistrePar(saisieParInfo)
        .dateValidation(cotisation.getDateValidation())
        .validePar(valideurInfo)
        .createdAt(cotisation.getCreatedAt())
        .updatedAt(cotisation.getUpdatedAt())
        .build();
  }

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
