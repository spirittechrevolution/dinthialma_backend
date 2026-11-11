package com.africa.dinthialma_backend.me.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Situation de l'appelant pour la cotisation du tour en cours d'une tontine. */
@Schema(
    description =
        "Cotisation du tour en cours."
            + " A_PAYER = aucun paiement déclaré ;"
            + " EN_ATTENTE = paiement déclaré, en attente de validation ;"
            + " VALIDE = paiement validé.")
public enum StatutCotisationDue {
  A_PAYER,
  EN_ATTENTE,
  VALIDE
}
