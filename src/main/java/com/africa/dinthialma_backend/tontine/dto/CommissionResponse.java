package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.CommissionType;
import com.africa.dinthialma_backend.tontine.entity.TontineCommission;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/** Représentation d'une commission de tontine retournée par l'API. */
@Getter
@Builder
public class CommissionResponse {

  private UUID id;
  private UUID tontineId;
  private CommissionType type;
  private BigDecimal valeur;
  private String description;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /** Construit un CommissionResponse depuis une entité. */
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
