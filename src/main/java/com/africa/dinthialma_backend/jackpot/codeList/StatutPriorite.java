package com.africa.dinthialma_backend.jackpot.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Statut d'une priorité de jackpot. */
@Schema(
    description =
        "EN_ATTENTE = demande du membre à traiter ; ACCEPTEE = passera en premier au prochain tour"
            + " désigné ; REFUSEE = refusée par le gestionnaire ; ANNULEE = annulée ; SERVIE = le"
            + " membre a reçu le jackpot")
public enum StatutPriorite {
  EN_ATTENTE,
  ACCEPTEE,
  REFUSEE,
  ANNULEE,
  SERVIE;

  /** Priorité encore en cours (une seule autorisée par membership). */
  public boolean estActive() {
    return this == EN_ATTENTE || this == ACCEPTEE;
  }
}
