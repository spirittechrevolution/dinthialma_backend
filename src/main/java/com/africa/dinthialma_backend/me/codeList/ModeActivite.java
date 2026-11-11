package com.africa.dinthialma_backend.me.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Périmètre du fil d'activité. */
@Schema(
    description =
        "COTISE = mes propres cotisations et jackpots ; GERE = ceux des tontines que j'ai créées")
public enum ModeActivite {
  COTISE,
  GERE
}
