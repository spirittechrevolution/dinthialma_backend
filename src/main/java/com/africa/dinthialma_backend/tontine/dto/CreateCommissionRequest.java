package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.CommissionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Corps de requête pour la création d'une commission sur une tontine. */
@Getter
@Setter
public class CreateCommissionRequest {

  /**
   * Type de commission.
   *
   * <ul>
   *   <li>{@code POURCENTAGE_JACKPOT} – taux en % déduit de chaque jackpot
   *   <li>{@code FRAIS_FIXES_PAR_CYCLE} – montant fixe FCFA déduit de chaque jackpot
   *   <li>{@code FRAIS_ADHESION} – montant fixe FCFA prélevé à l'entrée du membre
   * </ul>
   */
  @NotNull private CommissionType type;

  /**
   * Valeur de la commission.
   *
   * <ul>
   *   <li>Pour {@code POURCENTAGE_JACKPOT} : taux en % (ex : {@code 4.00} = 4 %)
   *   <li>Pour les autres types : montant en FCFA (ex : {@code 1000.00})
   * </ul>
   *
   * <p>Doit être strictement positif.
   */
  @NotNull
  @DecimalMin(value = "0.01", message = "La valeur doit être strictement positive")
  @Digits(integer = 8, fraction = 2)
  private BigDecimal valeur;

  /** Note optionnelle affichée aux membres (ex : "Frais de gestion du groupe"). */
  private String description;
}
