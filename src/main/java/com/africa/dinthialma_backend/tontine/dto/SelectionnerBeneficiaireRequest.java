package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête de sélection du bénéficiaire du jackpot après clôture d'un cycle."
            + " Si `membreId` est absent, la sélection est aléatoire parmi les membres éligibles"
            + " (ACTIF et n'ayant pas encore reçu de jackpot dans cette tontine).")
@Getter
@Setter
public class SelectionnerBeneficiaireRequest {

  @Schema(
      description =
          "UUID du membre à désigner comme bénéficiaire (tontine_membres.id)."
              + " Laisser null pour une sélection aléatoire.",
      example = "550e8400-e29b-41d4-a716-446655440099",
      nullable = true)
  private UUID membreId;
}
