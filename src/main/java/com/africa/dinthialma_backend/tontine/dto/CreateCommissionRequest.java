package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.CommissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête de création d'une commission sur une tontine."
            + " Maximum 1 commission par type (POURCENTAGE_JACKPOT, FRAIS_FIXES_PAR_CYCLE,"
            + " FRAIS_ADHESION).")
@Getter
@Setter
public class CreateCommissionRequest {

  @Schema(
      description =
          "Type de commission :\n"
              + "- POURCENTAGE_JACKPOT : taux en % déduit de chaque jackpot à la clôture\n"
              + "- FRAIS_FIXES_PAR_CYCLE : montant fixe FCFA déduit de chaque jackpot\n"
              + "- FRAIS_ADHESION : montant fixe FCFA prélevé à l'entrée du membre (ne réduit pas"
              + " le jackpot)",
      example = "POURCENTAGE_JACKPOT",
      allowableValues = {"POURCENTAGE_JACKPOT", "FRAIS_FIXES_PAR_CYCLE", "FRAIS_ADHESION"})
  @NotNull
  private CommissionType type;

  @Schema(
      description =
          "Valeur de la commission :\n"
              + "- Pour POURCENTAGE_JACKPOT : taux en % (ex : 4.00 = 4 %)\n"
              + "- Pour les autres types : montant en FCFA (ex : 1000.00)\n"
              + "Doit être strictement positif.",
      example = "4.00")
  @NotNull
  @DecimalMin(value = "0.01", message = "La valeur doit être strictement positive")
  @Digits(integer = 8, fraction = 2)
  private BigDecimal valeur;

  @Schema(
      description = "Note optionnelle affichée aux membres",
      example = "Frais de gestion du groupe")
  private String description;
}
