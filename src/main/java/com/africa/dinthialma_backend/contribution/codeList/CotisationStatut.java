package com.africa.dinthialma_backend.contribution.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Statut d'une cotisation. */
@Schema(
    description =
        "Statut d'une cotisation. EN_ATTENTE = enregistrée par le membre, en attente de"
            + " validation admin. VALIDE = confirmée et prise en compte dans le jackpot."
            + " EN_RETARD = non effectuée avant la clôture du cycle."
            + " REFUSE = déclaration rejetée par l'admin (motif fourni) ; le membre peut"
            + " déclarer à nouveau sur le même cycle.")
public enum CotisationStatut {
  /** Paiement enregistré par le membre – en attente de validation par l'admin. */
  EN_ATTENTE,
  /** Paiement confirmé et validé par l'admin. */
  VALIDE,
  /** Paiement non effectué avant la clôture du cycle. */
  EN_RETARD,
  /**
   * Déclaration rejetée par l'admin (ex : transfert non reçu). Statut final : elle n'entre dans
   * aucun total et ne bloque pas une nouvelle déclaration sur le même cycle.
   */
  REFUSE
}
