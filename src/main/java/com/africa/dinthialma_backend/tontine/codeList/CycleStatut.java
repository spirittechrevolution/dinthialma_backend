package com.africa.dinthialma_backend.tontine.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Cycle de vie d'un cycle de tontine. */
@Schema(
    description =
        "Statut d'un cycle de tontine. EN_ATTENTE = programmé mais non démarré,"
            + " EN_COURS = cotisations en cours, TERMINE = jackpot remis et cycle clôturé.")
public enum CycleStatut {
  /** Cycle programmé mais pas encore démarré. */
  EN_ATTENTE,
  /** Cycle en cours – les membres peuvent cotiser. */
  EN_COURS,
  /** Jackpot remis au bénéficiaire – cycle clôturé. */
  TERMINE
}
