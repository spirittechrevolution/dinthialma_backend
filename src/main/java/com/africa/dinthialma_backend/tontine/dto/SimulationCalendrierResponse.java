package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Résultat de la simulation du calendrier (B26, B32). */
@Schema(description = "Calendrier simulé : tours, échéances de cotisation (400 au plus listées)")
@Getter
@Builder
public class SimulationCalendrierResponse {

  @Schema(description = "Nombre total de tours", example = "10")
  private int nombreTours;

  @Schema(description = "Cotisations (échéances) par tour retenues (B32)", example = "4")
  private int cotisationsParTour;

  @Schema(
      description =
          "Jackpot brut prévu par tour (et par gagnant) : montant × membres × cotisations ÷"
              + " gagnants ; absent sans montant",
      example = "200000")
  private BigDecimal jackpotParTour;

  @Schema(description = "Première échéance de cotisation", example = "2026-11-14T23:59:00Z")
  private OffsetDateTime premiereEcheance;

  @Schema(description = "Dernière échéance de cotisation", example = "2026-12-11T23:59:00Z")
  private OffsetDateTime derniereEcheance;

  @Schema(description = "Rythme lisible", example = "tous les 3 jours, avant 23h59")
  private String rythme;

  @Schema(description = "Toutes les échéances de cotisation, dans l'ordre (400 au plus)")
  private List<OffsetDateTime> echeances;

  @Schema(description = "Tours et leurs échéances (400 tours au plus)")
  private List<TourSimule> tours;

  /** Un tour simulé. */
  @Getter
  @Builder
  public static class TourSimule {

    @Schema(example = "1")
    private int numero;

    private LocalDate dateDebut;

    @Schema(description = "Jour de la dernière échéance du tour")
    private LocalDate dateFin;

    private List<OffsetDateTime> echeances;
  }
}
