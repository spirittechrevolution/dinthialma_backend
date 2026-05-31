package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.CommissionType;
import com.africa.dinthialma_backend.tontine.entity.TontineCommission;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Représentation d'une commission configurée sur une tontine")
@Getter
@Builder
public class CommissionResponse {

  @Schema(description = "UUID de la commission")
  private UUID id;

  @Schema(description = "UUID de la tontine parente")
  private UUID tontineId;

  @Schema(
      description = "Type de commission",
      allowableValues = {"POURCENTAGE_JACKPOT", "FRAIS_FIXES_PAR_CYCLE", "FRAIS_ADHESION"})
  private CommissionType type;

  @Schema(
      description =
          "Valeur : taux en % pour POURCENTAGE_JACKPOT, montant FCFA pour les autres types",
      example = "4.00")
  private BigDecimal valeur;

  @Schema(description = "Note affichée aux membres", example = "Frais de gestion du groupe")
  private String description;

  @Schema(description = "Date de création")
  private LocalDateTime createdAt;

  @Schema(description = "Date de dernière modification")
  private LocalDateTime updatedAt;

  public static CommissionResponse from(TontineCommission commission) {
    return CommissionResponse.builder()
        .id(commission.getId())
        .tontineId(commission.getTontine() != null ? commission.getTontine().getId() : null)
        .type(commission.getType())
        .valeur(commission.getValeur())
        .description(commission.getDescription())
        .createdAt(commission.getCreatedAt())
        .updatedAt(commission.getUpdatedAt())
        .build();
  }
}
