package com.africa.dinthialma_backend.jackpot.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Qui est à l'origine de la priorité. */
@Schema(
    description =
        "DEMANDE_MEMBRE = demandée par le membre dans l'application ; GESTIONNAIRE = attribuée"
            + " directement par le gestionnaire (accord pris hors application)")
public enum OriginePriorite {
  DEMANDE_MEMBRE,
  GESTIONNAIRE
}
