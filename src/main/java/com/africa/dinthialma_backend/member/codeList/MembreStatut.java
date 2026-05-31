package com.africa.dinthialma_backend.member.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Statut d'un membre dans une tontine. */
@Schema(
    description =
        "Statut d'un membre dans une tontine. ACTIF = cotise normalement."
            + " SUSPENDU = exclu temporairement (ex : retards répétés)."
            + " SORTI = exclu définitivement de la tontine.")
public enum MembreStatut {
  /** Membre actif – cotise normalement. */
  ACTIF,
  /** Membre suspendu – temporairement exclu des cotisations (ex : retards répétés). */
  SUSPENDU,
  /** Membre sorti définitivement de la tontine. */
  SORTI
}
