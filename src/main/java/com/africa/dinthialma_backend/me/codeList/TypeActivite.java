package com.africa.dinthialma_backend.me.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Type d'une ligne du fil d'activité. */
@Schema(
    description =
        "COTISATION_DECLAREE (en attente), COTISATION_VALIDEE (déclarée par le membre puis"
            + " validée), COTISATION_ENCAISSEE (saisie par le gestionnaire), COTISATION_EN_RETARD,"
            + " COTISATION_REFUSEE, JACKPOT_RECU")
public enum TypeActivite {
  COTISATION_DECLAREE,
  COTISATION_VALIDEE,
  COTISATION_ENCAISSEE,
  COTISATION_EN_RETARD,
  COTISATION_REFUSEE,
  JACKPOT_RECU
}
