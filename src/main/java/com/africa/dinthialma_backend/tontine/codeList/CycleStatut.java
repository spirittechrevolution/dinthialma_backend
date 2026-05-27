package com.africa.dinthialma_backend.tontine.codeList;

/** Cycle de vie d'un cycle de tontine. */
public enum CycleStatut {
  /** Cycle programmé mais pas encore démarré. */
  EN_ATTENTE,
  /** Cycle en cours – les membres peuvent cotiser. */
  EN_COURS,
  /** Jackpot remis au bénéficiaire – cycle clôturé. */
  TERMINE
}
