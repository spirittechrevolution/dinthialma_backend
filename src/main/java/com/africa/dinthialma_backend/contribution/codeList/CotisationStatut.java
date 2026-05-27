package com.africa.dinthialma_backend.contribution.codeList;

/** Statut d'une cotisation. */
public enum CotisationStatut {
  /** Paiement enregistré par le membre – en attente de validation par l'admin. */
  EN_ATTENTE,
  /** Paiement confirmé et validé par l'admin. */
  VALIDE,
  /** Paiement non effectué avant la clôture du cycle. */
  EN_RETARD
}
