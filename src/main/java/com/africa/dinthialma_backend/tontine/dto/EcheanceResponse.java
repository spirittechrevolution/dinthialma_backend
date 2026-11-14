package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.StatutEcheance;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/** Échéance de cotisation d'un tour (B32). */
@Schema(description = "Échéance de cotisation d'un tour")
@Getter
@Builder
public class EcheanceResponse {

  @Schema(
      description = "UUID de l'échéance (à passer en echeanceId pour payer une échéance passée)")
  private UUID id;

  @Schema(description = "Rang dans le tour (1 à N)", example = "2")
  private int numero;

  @Schema(description = "Premier jour de la période de cotisation", example = "2026-11-23")
  private LocalDate dateDebut;

  @Schema(description = "Limite de paiement, avec fuseau", example = "2026-11-29T20:00:00Z")
  private OffsetDateTime echeanceAt;

  @Schema(description = "Statut calculé : A_VENIR, EN_COURS, ECHUE")
  private StatutEcheance statut;
}
