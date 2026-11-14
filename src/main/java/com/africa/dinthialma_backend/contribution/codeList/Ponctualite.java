package com.africa.dinthialma_backend.contribution.codeList;

import com.africa.dinthialma_backend.common.util.Fuseau;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.EcheanceCycle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
   * gestionnaire), sinon date de déclaration, comparée à l'échéance qu'elle couvre (B32), à défaut
   * à celle du tour. Une cotisation absente ou refusée ne compte pas.
   */
  public static Ponctualite de(Cotisation cotisation) {
    if (cotisation == null || cotisation.getStatut() == CotisationStatut.REFUSE) {
      return NON_DECLAREE;
    }
    // Sans date de saisie, la cotisation est en cours de création (réponse avant écriture en base)
    LocalDateTime date =
        cotisation.getDatePaiement() != null
            ? cotisation.getDatePaiement()
            : cotisation.getCreatedAt() != null ? cotisation.getCreatedAt() : Fuseau.maintenant();
    if (cotisation.getEcheance() != null) {
      return date.isBefore(finEcheance(cotisation.getEcheance())) ? A_TEMPS : EN_RETARD;
    }
    if (cotisation.getCycle() == null) {
      return A_TEMPS;
    }
    return date.isBefore(finEcheance(cotisation.getCycle())) ? A_TEMPS : EN_RETARD;
  }

  /** Premier instant après une échéance : la minute d'échéance est incluse en entier. */
  public static LocalDateTime finEcheance(EcheanceCycle echeance) {
    return echeance.getEcheanceAt().truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
  }

  /**
   * Premier instant après l'échéance du tour : la minute d'échéance ({@code echeanceAt}, B26) est
   * incluse en entier (échéance 23:59 → payé à 23:59:59 = à temps). Tour sans échéance enregistrée
   * : fin du jour {@code dateFin}.
   */
  public static LocalDateTime finEcheance(CycleTontine cycle) {
    if (cycle.getEcheanceAt() != null) {
      return cycle.getEcheanceAt().truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
    }
    return cycle.getDateFin().plusDays(1).atStartOfDay();
  }
}
