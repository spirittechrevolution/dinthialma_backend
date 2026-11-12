package com.africa.dinthialma_backend.contribution.codeList;

import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Ponctualité d'une cotisation (S01), distincte de son statut : le membre a-t-il payé avant
 * l'échéance du tour ? Le statut {@code EN_RETARD} garde son sens historique (« non validée à la
 * clôture du tour »).
 */
@Schema(
    description =
        "A_TEMPS = déclarée (ou payée en main propre) avant l'échéance du tour ; EN_RETARD = après"
            + " l'échéance ; NON_DECLAREE = aucune déclaration retenue (aucune, ou seulement"
            + " refusée)")
public enum Ponctualite {
  A_TEMPS,
  EN_RETARD,
  NON_DECLAREE;

  /**
   * Ponctualité d'une cotisation : {@code datePaiement} si renseignée (paiement remis au
   * gestionnaire), sinon date de déclaration, comparée à l'échéance du tour. Une cotisation absente
   * ou refusée ne compte pas.
   */
  public static Ponctualite de(Cotisation cotisation) {
    if (cotisation == null || cotisation.getStatut() == CotisationStatut.REFUSE) {
      return NON_DECLAREE;
    }
    LocalDateTime date =
        cotisation.getDatePaiement() != null
            ? cotisation.getDatePaiement()
            : cotisation.getCreatedAt();
    if (date == null || cotisation.getCycle() == null) {
      return A_TEMPS;
    }
    return date.isBefore(finEcheance(cotisation.getCycle())) ? A_TEMPS : EN_RETARD;
  }

  /**
   * Premier instant après l'échéance du tour. En attendant l'heure d'échéance de B26 : fin du jour
   * {@code dateFin} (23h59 incluse).
   */
  public static LocalDateTime finEcheance(CycleTontine cycle) {
    return cycle.getDateFin().plusDays(1).atStartOfDay();
  }
}
