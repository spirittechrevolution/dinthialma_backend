package com.africa.dinthialma_backend.notification.codeList;

/** Nature d'un rappel lié à une échéance de cotisation (B26). */
public enum NatureRappel {
  /** La veille de l'échéance, à 18h00 (membres n'ayant pas déclaré). */
  VEILLE,
  /** N heures avant l'échéance (membres n'ayant pas déclaré). */
  HEURES_AVANT,
  /** Échéance passée avec des cotisations non déclarées (gestionnaire). */
  COTISATIONS_MANQUANTES
}
