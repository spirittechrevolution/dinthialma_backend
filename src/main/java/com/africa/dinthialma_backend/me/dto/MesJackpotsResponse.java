package com.africa.dinthialma_backend.me.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(
    description =
        "Mes jackpots (tontines ROTATIVE) : ceux déjà reçus et une estimation des prochains.")
@Getter
@Builder
public class MesJackpotsResponse {

  @Schema(description = "Total des jackpots reçus, en FCFA", example = "57000")
  private BigDecimal totalRecu;

  @Schema(description = "Jackpots reçus, du plus récent au plus ancien")
  private List<JackpotRecu> recus;

  @Schema(
      description =
          "Prochains jackpots : une ligne par tontine ROTATIVE ACTIVE où je suis ACTIF et n'ai pas"
              + " encore gagné")
  @JsonProperty("aVenir")
  private List<JackpotAVenir> aVenir;

  /**
   * Getter explicite : celui de Lombok ({@code getAVenir}) serait sérialisé {@code avenir} (cf.
   * {@code MembreResponse#isARecuJackpot()}).
   */
  @JsonProperty("aVenir")
  public List<JackpotAVenir> getAVenir() {
    return aVenir;
  }

  @Schema(description = "Jackpot reçu")
  @Getter
  @Builder
  public static class JackpotRecu {

    @Schema(description = "UUID de la tontine")
    private UUID tontineId;

    @Schema(description = "Nom de la tontine", example = "Amis")
    private String tontineNom;

    @Schema(description = "UUID du cycle gagné")
    private UUID cycleId;

    @Schema(description = "Numéro du tour gagné", example = "3")
    private int numeroCycle;

    @Schema(description = "Date de remise (fin du tour si non renseignée)")
    private LocalDate dateRemise;

    @Schema(description = "Montant net reçu, en FCFA", example = "57000")
    private BigDecimal montantRecu;
  }

  @Schema(description = "Estimation de mon prochain jackpot dans une tontine")
  @Getter
  @Builder
  public static class JackpotAVenir {

    @Schema(description = "UUID de la tontine")
    private UUID tontineId;

    @Schema(description = "Nom de la tontine", example = "Famille")
    private String tontineNom;

    @Schema(description = "UUID de mon membership")
    private UUID monMembreId;

    @Schema(
        description =
            "ORDRE = rotation fixe (estimation possible) ; TIRAGE = tirage au sort à chaque tour ;"
                + " CHOIX = désigné par le gestionnaire",
        allowableValues = {"ORDRE", "TIRAGE", "CHOIX"})
    private String modeAttribution;

    @Schema(
        description =
            "ORDRE : mon rang parmi les membres qui n'ont pas encore gagné (1 = prochain tour)",
        example = "7",
        nullable = true)
    private Integer position;

    @Schema(
        description = "ORDRE : numéro du tour où je devrais gagner",
        example = "7",
        nullable = true)
    private Integer numeroCycleEstime;

    @Schema(
        description =
            "ORDRE : fin prévue de ce tour (null si le tour n'est pas encore planifié, ex. mode"
                + " MANUEL) ; toujours null pour TIRAGE et CHOIX",
        nullable = true)
    private LocalDate dateEstimee;

    @Schema(
        description =
            "Montant brut estimé par gagnant (montant × membres ACTIFS ÷ gagnants par tour),"
                + " avant commissions",
        example = "60000",
        nullable = true)
    private BigDecimal montantEstime;
  }
}
