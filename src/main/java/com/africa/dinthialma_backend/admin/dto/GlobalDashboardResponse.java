package com.africa.dinthialma_backend.admin.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/**
 * Réponse du dashboard global réservé au SUPER_ADMIN.
 *
 * <p>Agrège les métriques utilisateurs, tontines, finances et activité récente de toute la
 * plateforme.
 */
@Getter
@Builder
public class GlobalDashboardResponse {

  private UsersStats users;
  private TontinesStats tontines;
  private FinancesStats finances;
  private ActiviteRecente activite24h;

  /** Statistiques utilisateurs. */
  @Getter
  @Builder
  public static class UsersStats {
    /** Total comptes non supprimés. */
    private long total;

    /** Comptes actifs. */
    private long actifs;

    /** Comptes désactivés (active = false). */
    private long desactives;

    /** Nouveaux inscrits depuis le 1er du mois courant. */
    private long nouveauxCeMois;
  }

  /** Statistiques tontines. */
  @Getter
  @Builder
  public static class TontinesStats {
    private long total;
    private long brouillon;
    private long actives;
    private long suspendues;
    private long terminees;
  }

  /** Statistiques financières. */
  @Getter
  @Builder
  public static class FinancesStats {
    /** Nombre de cotisations en attente de validation (toute plateforme). */
    private long cotisationsEnAttente;

    /** Nombre de cotisations marquées en retard (toute plateforme). */
    private long cotisationsEnRetard;

    /** Somme des montants des cotisations VALIDÉES depuis le 1er du mois courant. */
    private BigDecimal montantCotisationsCeMois;
  }

  /** Activité des dernières 24 heures. */
  @Getter
  @Builder
  public static class ActiviteRecente {
    private long nouveauxInscrits;
    private long cotisationsEnregistrees;
  }
}
