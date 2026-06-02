package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Bénéficiaire d'un jackpot pour un cycle terminé")
@Getter
@Builder
public class BeneficiaireHistoriqueResponse {

  @Schema(description = "UUID du cycle")
  private UUID cycleId;

  @Schema(description = "Numéro séquentiel du cycle", example = "2")
  private int numeroCycle;

  @Schema(description = "Date de remise du jackpot", example = "2024-07-31")
  private LocalDate dateRemise;

  @Schema(description = "Jackpot brut = somme des cotisations validées", example = "60000")
  private BigDecimal montantJackpot;

  @Schema(description = "Commissions prélevées", example = "2400")
  private BigDecimal montantCommission;

  @Schema(description = "Montant net remis au bénéficiaire", example = "57600")
  private BigDecimal montantNet;

  @Schema(description = "Bénéficiaire du jackpot")
  private BeneficiaireInfo beneficiaire;

  public static BeneficiaireHistoriqueResponse from(CycleTontine cycle) {
    var membre = cycle.getBeneficiaire();
    var user = membre.getUser();

    BeneficiaireInfo info =
        BeneficiaireInfo.builder()
            .membreId(membre.getId())
            .userId(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phone(user.getPhone())
            .ordreJackpot(membre.getOrdreJackpot())
            .dateJackpot(membre.getDateJackpot())
            .build();

    return BeneficiaireHistoriqueResponse.builder()
        .cycleId(cycle.getId())
        .numeroCycle(cycle.getNumeroCycle())
        .dateRemise(cycle.getDateRemise())
        .montantJackpot(cycle.getMontantJackpot())
        .montantCommission(cycle.getMontantCommission())
        .montantNet(cycle.getMontantNet())
        .beneficiaire(info)
        .build();
  }

  @Schema(description = "Informations du bénéficiaire")
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

    @Schema(description = "Numéro de téléphone", example = "221770000001")
    private String phone;

    @Schema(description = "Position dans la rotation des jackpots", example = "2")
    private Integer ordreJackpot;

    @Schema(description = "Date/heure de remise du jackpot au membre")
    private LocalDateTime dateJackpot;
  }
}
