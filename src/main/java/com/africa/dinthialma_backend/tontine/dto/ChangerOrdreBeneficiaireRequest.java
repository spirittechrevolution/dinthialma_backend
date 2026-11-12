package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Changement de la règle de désignation des gagnants, y compris en cours")
@Getter
@Setter
public class ChangerOrdreBeneficiaireRequest {

  @Schema(
      description =
          "Nouvel ordre : ROTATION, ALEATOIRE, MANUEL ou HYBRIDE. S'applique à la prochaine"
              + " clôture de tour ; les gagnants déjà désignés ne changent pas.",
      allowableValues = {"ROTATION", "ALEATOIRE", "MANUEL", "HYBRIDE"},
      example = "HYBRIDE")
  @NotBlank(message = "ordreBeneficiaire est obligatoire")
  private String ordreBeneficiaire;
}
