package com.africa.dinthialma_backend.tontine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Corps de requête pour la modification d'une commission (valeur et/ou description). */
@Getter
@Setter
public class UpdateCommissionRequest {

  /**
   * Nouvelle valeur de la commission – null si inchangée.
   *
   * <p>Doit être strictement positive si fournie.
   */
  @DecimalMin(value = "0.01", message = "La valeur doit être strictement positive")
  @Digits(integer = 8, fraction = 2)
  private BigDecimal valeur;

  /** Nouvelle description – null si inchangée. Chaîne vide pour effacer la description. */
  private String description;
}
