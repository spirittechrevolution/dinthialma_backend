package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête de désignation des gagnants du jackpot après clôture d'un cycle."
            + " Disponible uniquement pour les tontines avec ordreBeneficiaire = MANUEL."
            + " Le nombre de membreIds doit correspondre au nombreGagnants de la tontine"
            + " (ou être inférieur si moins de membres éligibles restants).")
@Getter
@Setter
public class SelectionnerBeneficiaireRequest {

  @Schema(
      description =
          "UUIDs des membres à désigner comme gagnants (tontine_membres.id)."
              + " Doit contenir entre 1 et nombreGagnants entrées."
              + " Null ou liste vide = sélection aléatoire parmi les membres éligibles.",
      nullable = true)
  private List<UUID> membreIds;
}
