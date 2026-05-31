package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête de modification d'une commission (valeur et/ou description)."
            + " Les champs null ne sont pas modifiés.")
@Getter
@Setter
public class UpdateCommissionRequest {

  @Schema(
      description = "Nouvelle valeur de la commission (null = inchangée, doit être > 0 si fournie)",
      example = "5.00")
  @DecimalMin(value = "0.01", message = "La valeur doit être strictement positive")
  @Digits(integer = 8, fraction = 2)
  private BigDecimal valeur;

  @Schema(
      description =
          "Nouvelle description (null = inchangée, chaîne vide pour effacer la description)",
      example = "Frais de gestion révisés")
  private String description;
}
