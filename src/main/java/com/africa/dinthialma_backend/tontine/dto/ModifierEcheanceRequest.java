package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

/**
 * Modification du jour et de l'heure d'échéance et des rappels d'une tontine (B26). Un champ absent
 * (null) garde sa valeur ; le rythme (intervalle, unité) ne se modifie qu'en BROUILLON, par {@code
 * PUT /v1/tontines/{id}}.
 */
@Schema(
    description =
        "Jour / heure d'échéance et rappels (B26) ; appliqués aux tours pas encore commencés")
@Getter
@Setter
public class ModifierEcheanceRequest {

  @Schema(
      description =
          "Nouveau jour d'échéance (null = inchangé, \"\" = aucun jour imposé) : SEMAINE →"
              + " LUNDI…DIMANCHE ; MOIS → 1…31 ou DERNIER",
      example = "DIMANCHE")
  private String jourEcheance;

  @Schema(description = "Nouvelle heure d'échéance HH:mm (null = inchangée)", example = "20:00")
  private String heureEcheance;

  @Schema(description = "Rappels avant échéance (champ absent = inchangé)")
  @Valid
  private RappelsEcheance rappels;
}
