package com.africa.dinthialma_backend.tontine.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Mode de gestion des cycles d'une tontine. */
@Schema(
    description =
        "Mode de gestion des cycles. AUTOMATIQUE = tous les cycles générés à l'activation,"
            + " MANUEL = l'admin ouvre chaque cycle manuellement.")
public enum ModeCycle {
  /** Tous les cycles sont générés automatiquement à l'activation de la tontine. */
  AUTOMATIQUE,
  /** L'admin ouvre chaque cycle manuellement au moment voulu. */
  MANUEL
}
