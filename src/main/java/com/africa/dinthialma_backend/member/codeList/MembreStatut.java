package com.africa.dinthialma_backend.member.codeList;

/** Statut d'un membre dans une tontine. */
public enum MembreStatut {
  /** Membre actif – cotise normalement. */
  ACTIF,
  /** Membre suspendu – temporairement exclu des cotisations (ex : retards répétés). */
  SUSPENDU,
  /** Membre sorti définitivement de la tontine. */
  SORTI
}
