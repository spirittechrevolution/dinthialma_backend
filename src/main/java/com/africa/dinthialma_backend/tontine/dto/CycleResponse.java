package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/** Représentation d'un cycle de tontine retournée par l'API. */
@Getter
@Builder
public class CycleResponse {

  private UUID id;
  private UUID tontineId;
  private int numeroCycle;
  private LocalDate dateDebut;
  private LocalDate dateFin;
  private BigDecimal montantJackpot;
  private BigDecimal montantCommission;
  private BigDecimal montantNet;
  private CycleStatut statut;
  private LocalDate dateRemise;

  /** Bénéficiaire du jackpot – null si pas encore désigné. */
  private BeneficiaireInfo beneficiaire;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /** Construit un CycleResponse depuis une entité. */
  public static CycleResponse from(CycleTontine cycle) {
    BeneficiaireInfo beneficiaireInfo = null;
    if (cycle.getBeneficiaire() != null && cycle.getBeneficiaire().getUser() != null) {
      var user = cycle.getBeneficiaire().getUser();
      beneficiaireInfo =
          BeneficiaireInfo.builder()
              .membreId(cycle.getBeneficiaire().getId())
              .userId(user.getId())
              .firstName(user.getFirstName())
              .lastName(user.getLastName())
              .phone(user.getPhone())
              .ordreJackpot(cycle.getBeneficiaire().getOrdreJackpot())
              .build();
    }

    return CycleResponse.builder()
        .id(cycle.getId())
        .tontineId(cycle.getTontine() != null ? cycle.getTontine().getId() : null)
        .numeroCycle(cycle.getNumeroCycle())
        .dateDebut(cycle.getDateDebut())
        .dateFin(cycle.getDateFin())
        .montantJackpot(cycle.getMontantJackpot())
        .montantCommission(cycle.getMontantCommission())
        .montantNet(cycle.getMontantNet())
        .statut(cycle.getStatut())
        .dateRemise(cycle.getDateRemise())
        .beneficiaire(beneficiaireInfo)
        .createdAt(cycle.getCreatedAt())
        .updatedAt(cycle.getUpdatedAt())
        .build();
  }

  /** Informations résumées du bénéficiaire. */
  @Getter
  @Builder
  public static class BeneficiaireInfo {
    private UUID membreId;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String phone;
    private Integer ordreJackpot;
  }
}
