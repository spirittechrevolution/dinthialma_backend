package com.africa.dinthialma_backend.contribution.dto;

import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/** Représentation d'une cotisation retournée par l'API. */
@Getter
@Builder
public class CotisationResponse {

  private UUID id;
  private UUID tontineId;
  private UUID cycleId;
  private MembreInfo membre;
  private BigDecimal montant;
  private String methodePaiement;
  private String referenceTransaction;
  private CotisationStatut statut;
  private String note;
  private LocalDateTime dateValidation;
  private ValideurInfo validePar;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /** Construit un CotisationResponse depuis une entité. */
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
        .dateValidation(cotisation.getDateValidation())
        .validePar(valideurInfo)
        .createdAt(cotisation.getCreatedAt())
        .updatedAt(cotisation.getUpdatedAt())
        .build();
  }

  /** Informations résumées du cotisant. */
  @Getter
  @Builder
  public static class MembreInfo {
    private UUID membreId;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String phone;
  }

  /** Informations résumées de l'admin valideur. */
  @Getter
  @Builder
  public static class ValideurInfo {
    private UUID id;
    private String firstName;
    private String lastName;
  }
}
