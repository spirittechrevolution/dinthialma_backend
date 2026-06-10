package com.africa.dinthialma_backend.tontine.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Cycle de vie d'une tontine. */
@Schema(
    description =
        "Statut du cycle de vie d'une tontine."
            + " BROUILLON → ACTIVE → SUSPENDUE (réversible) ou TERMINEE (irréversible).")
public enum TontineStatut {
  /** Création en cours – membres non encore complets, tontine non démarrée. */
  BROUILLON,
  /** Tontine démarrée – cycles en cours. */
  ACTIVE,
  /** Tontine temporairement mise en pause par l'admin. */
  SUSPENDUE,
  /** Tous les cycles sont terminés – jackpot remis à tous les membres. */
  TERMINEE
}
