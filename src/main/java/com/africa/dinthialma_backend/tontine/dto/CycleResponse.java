package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Représentation d'un cycle de tontine")
@Getter
@Builder
public class CycleResponse {

  @Schema(description = "UUID du cycle")
  private UUID id;

  @Schema(description = "UUID de la tontine parente")
  private UUID tontineId;

  @Schema(description = "Numéro de ce cycle dans la séquence (1 = premier)", example = "3")
  private int numeroCycle;

  @Schema(description = "Date de début du cycle", example = "2024-07-01")
  private LocalDate dateDebut;

  @Schema(description = "Date de fin prévue du cycle", example = "2024-07-31")
  private LocalDate dateFin;

  @Schema(
      description = "Jackpot brut = somme des cotisations VALIDÉES du cycle (calculé à la clôture)",
      example = "60000")
  private BigDecimal montantJackpot;

  @Schema(description = "Total des commissions déduites (calculé à la clôture)", example = "2400")
  private BigDecimal montantCommission;

  @Schema(
      description =
          "Montant net remis au bénéficiaire = jackpot brut - commissions (calculé à la clôture)",
      example = "57600")
  private BigDecimal montantNet;

  @Schema(
      description = "Statut courant du cycle",
      allowableValues = {"EN_ATTENTE", "EN_COURS", "TERMINE"})
  private CycleStatut statut;

  @Schema(
      description = "Date de remise effective du jackpot (date de clôture du cycle)",
      example = "2024-07-31")
  private LocalDate dateRemise;

  @Schema(description = "Bénéficiaire du jackpot – null si pas encore désigné")
  private BeneficiaireInfo beneficiaire;

  @Schema(description = "Date de création du cycle")
  private LocalDateTime createdAt;

  @Schema(description = "Date de dernière modification")
  private LocalDateTime updatedAt;

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

  @Schema(description = "Informations résumées du bénéficiaire du jackpot")
  @Getter
  @Builder
  public static class BeneficiaireInfo {

    @Schema(description = "UUID du membre (tontine_membres.id)")
    private UUID membreId;

    @Schema(description = "UUID de l'utilisateur (users.id)")
    private UUID userId;

    @Schema(description = "Prénom", example = "Fatou")
    private String firstName;

    @Schema(description = "Nom de famille", example = "Sow")
    private String lastName;

    @Schema(description = "Numéro de téléphone normalisé", example = "221770000001")
    private String phone;

    @Schema(
        description = "Position dans la rotation des jackpots (1 = premier bénéficiaire)",
        example = "3")
    private Integer ordreJackpot;
  }
}
