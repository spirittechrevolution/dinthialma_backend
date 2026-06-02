package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
      description = "Jackpot brut = somme des cotisations VALIDÉES (calculé à la clôture)",
      example = "500000")
  private BigDecimal montantJackpot;

  @Schema(description = "Total des commissions déduites (calculé à la clôture)", example = "10000")
  private BigDecimal montantCommission;

  @Schema(
      description = "Montant net total = jackpot brut - commissions (calculé à la clôture)",
      example = "490000")
  private BigDecimal montantNet;

  @Schema(
      description = "Statut courant du cycle",
      allowableValues = {"EN_ATTENTE", "EN_COURS", "TERMINE"})
  private CycleStatut statut;

  @Schema(description = "Date de clôture effective du cycle", example = "2024-07-31")
  private LocalDate dateRemise;

  @Schema(description = "Gagnants du jackpot – vide si pas encore désignés")
  private List<GagnantInfo> gagnants;

  @Schema(description = "Date de création du cycle")
  private LocalDateTime createdAt;

  @Schema(description = "Date de dernière modification")
  private LocalDateTime updatedAt;

  public static CycleResponse from(CycleTontine cycle) {
    List<GagnantInfo> gagnantInfos =
        cycle.getGagnants().stream()
            .filter(g -> g.getDeletedAt() == null)
            .map(
                g -> {
                  var user = g.getMembre().getUser();
                  return GagnantInfo.builder()
                      .membreId(g.getMembre().getId())
                      .userId(user.getId())
                      .firstName(user.getFirstName())
                      .lastName(user.getLastName())
                      .phone(user.getPhone())
                      .ordreJackpot(g.getMembre().getOrdreJackpot())
                      .montantRecu(g.getMontantRecu())
                      .dateJackpot(g.getMembre().getDateJackpot())
                      .build();
                })
            .toList();

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
        .gagnants(gagnantInfos)
        .createdAt(cycle.getCreatedAt())
        .updatedAt(cycle.getUpdatedAt())
        .build();
  }

  @Schema(description = "Informations d'un gagnant du jackpot")
  @Getter
  @Builder
  public static class GagnantInfo {

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

    @Schema(description = "Position dans la rotation des jackpots", example = "3")
    private Integer ordreJackpot;

    @Schema(
        description = "Montant reçu par ce gagnant = montantNet / nombreGagnants",
        example = "245000")
    private BigDecimal montantRecu;

    @Schema(description = "Date/heure de remise du jackpot")
    private LocalDateTime dateJackpot;
  }
}
