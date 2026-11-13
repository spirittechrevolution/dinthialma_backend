package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Résultat de la simulation du calendrier (B26). */
@Schema(description = "Calendrier simulé : nombre de tours et échéances (400 au plus listées)")
@Getter
@Builder
public class SimulationCalendrierResponse {

  @Schema(description = "Nombre total de tours", example = "10")
  private int nombreTours;

  @Schema(description = "Échéance du premier tour", example = "2026-11-14T23:59:00Z")
  private OffsetDateTime premiereEcheance;

  @Schema(description = "Échéance du dernier tour", example = "2026-12-11T23:59:00Z")
  private OffsetDateTime derniereEcheance;

  @Schema(description = "Rythme lisible", example = "tous les 3 jours, avant 23h59")
  private String rythme;

  @Schema(description = "Échéances des tours, dans l'ordre (400 au plus)")
  private List<OffsetDateTime> echeances;
}
