package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rappels envoyés avant l'échéance aux membres actifs qui n'ont pas encore déclaré (B26). En
 * écriture, un champ absent garde sa valeur (création : valeur par défaut).
 */
@Schema(description = "Rappels avant échéance (défaut : la veille à 18h00 + 3 heures avant)")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RappelsEcheance {

  @Schema(description = "Rappel la veille de l'échéance à 18h00", example = "true")
  private Boolean veille;

  @Schema(description = "Rappel N heures avant l'échéance (0 = aucun, 1 à 24)", example = "3")
  @Min(0)
  @Max(24)
  private Integer heuresAvant;
}
