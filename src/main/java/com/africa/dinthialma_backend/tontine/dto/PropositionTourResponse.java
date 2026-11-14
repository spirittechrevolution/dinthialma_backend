package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Tour proposé à l'ouverture manuelle (B32) : dates et échéances calculées selon le rythme. */
@Schema(description = "Tour proposé (mode MANUEL) : le gestionnaire peut garder ou changer dateFin")
@Getter
@Builder
public class PropositionTourResponse {

  @Schema(description = "Premier jour du tour", example = "2026-11-16")
  private LocalDate dateDebut;

  @Schema(
      description = "Date de fin proposée (jour de la dernière échéance)",
      example = "2026-12-13")
  private LocalDate dateFin;

  @Schema(description = "Nombre d'échéances du tour", example = "4")
  private int nombreEcheances;

  @Schema(description = "Limites de paiement des échéances, dans l'ordre")
  private List<OffsetDateTime> echeances;
}
