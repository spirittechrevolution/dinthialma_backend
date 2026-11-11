package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Historique d'un cycle terminé avec ses gagnants")
@Getter
@Builder
public class BeneficiaireHistoriqueResponse {

  @Schema(description = "UUID du cycle")
  private UUID cycleId;

  @Schema(description = "Numéro séquentiel du cycle", example = "2")
  private int numeroCycle;

  @Schema(description = "Date de clôture du cycle", example = "2024-07-31")
  private LocalDate dateRemise;

  @Schema(description = "Jackpot brut = somme des cotisations validées", example = "500000")
  private BigDecimal montantJackpot;

  @Schema(description = "Commissions prélevées", example = "10000")
  private BigDecimal montantCommission;

  @Schema(description = "Montant net total distribué aux gagnants", example = "490000")
  private BigDecimal montantNet;

  @Schema(
      description = "Montant reçu par chaque gagnant = montantNet / nombreGagnants",
      example = "245000")
  private BigDecimal montantParGagnant;

  @Schema(description = "Liste des gagnants du cycle")
  private List<GagnantInfo> gagnants;

  @Schema(description = "Informations d'un gagnant")
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

    @Schema(description = "Numéro de téléphone", example = "221770000001")
    private String phone;

    @Schema(description = "Position dans la rotation", example = "2")
    private Integer ordreJackpot;

    @Schema(description = "Montant effectivement reçu", example = "245000")
    private BigDecimal montantRecu;

    @Schema(description = "Date/heure de remise du jackpot")
    private LocalDateTime dateJackpot;
  }
}
