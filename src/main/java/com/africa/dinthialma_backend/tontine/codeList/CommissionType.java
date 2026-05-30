package com.africa.dinthialma_backend.tontine.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Types de commission qu'un admin peut définir sur une tontine.
 *
 * <p>L'admin peut combiner plusieurs types ou n'en appliquer aucun. Une seule entrée par type est
 * autorisée par tontine ({@code UNIQUE(tontine_id, type)}).
 */
@Schema(
    description =
        "Type de commission appliquée automatiquement à chaque clôture de cycle."
            + " POURCENTAGE_JACKPOT = % du jackpot brut,"
            + " FRAIS_FIXES_PAR_CYCLE = montant fixe en FCFA par cycle,"
            + " FRAIS_ADHESION = frais uniques à l'entrée du membre (ne réduit pas le jackpot).")
public enum CommissionType {

  /**
   * Pourcentage prélevé sur chaque jackpot avant remise au bénéficiaire. La {@code valeur}
   * représente le taux en % (ex : {@code 5.00} = 5 %).
   */
  POURCENTAGE_JACKPOT,

  /**
   * Montant fixe en FCFA déduit du jackpot à chaque cycle. La {@code valeur} représente le montant
   * (ex : {@code 1000.00} = 1 000 FCFA).
   */
  FRAIS_FIXES_PAR_CYCLE,

  /**
   * Frais uniques payés par chaque nouveau membre à son entrée dans la tontine. La {@code valeur}
   * représente le montant (ex : {@code 500.00} = 500 FCFA).
   */
  FRAIS_ADHESION
}
