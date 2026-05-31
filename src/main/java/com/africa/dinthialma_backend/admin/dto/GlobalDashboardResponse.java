package com.africa.dinthialma_backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Schema(
    description =
        "Dashboard global plateforme réservé au SUPER_ADMIN."
            + " Agrège les métriques utilisateurs, tontines, finances et activité des 24 dernières"
            + " heures.")
@Getter
@Builder
public class GlobalDashboardResponse {

  @Schema(description = "Statistiques globales des utilisateurs")
  private UsersStats users;

  @Schema(description = "Statistiques globales des tontines")
  private TontinesStats tontines;

  @Schema(description = "Statistiques financières globales")
  private FinancesStats finances;

  @Schema(description = "Activité des 24 dernières heures")
  private ActiviteRecente activite24h;

  @Schema(description = "Statistiques des comptes utilisateurs")
  @Getter
  @Builder
  public static class UsersStats {

    @Schema(description = "Nombre total de comptes non supprimés", example = "1250")
    private long total;

    @Schema(description = "Nombre de comptes actifs", example = "1200")
    private long actifs;

    @Schema(description = "Nombre de comptes désactivés (active = false)", example = "50")
    private long desactives;

    @Schema(description = "Nouveaux inscrits depuis le 1er du mois courant", example = "34")
    private long nouveauxCeMois;
  }

  @Schema(description = "Statistiques des tontines")
  @Getter
  @Builder
  public static class TontinesStats {

    @Schema(description = "Nombre total de tontines non supprimées", example = "350")
    private long total;

    @Schema(description = "Tontines en statut BROUILLON", example = "12")
    private long brouillon;

    @Schema(description = "Tontines en statut ACTIVE", example = "280")
    private long actives;

    @Schema(description = "Tontines en statut SUSPENDUE", example = "8")
    private long suspendues;

    @Schema(description = "Tontines dont tous les cycles sont TERMINÉS", example = "50")
    private long terminees;
  }

  @Schema(description = "Statistiques financières de la plateforme")
  @Getter
  @Builder
  public static class FinancesStats {

    @Schema(
        description = "Nombre de cotisations EN_ATTENTE de validation sur toute la plateforme",
        example = "156")
    private long cotisationsEnAttente;

    @Schema(description = "Nombre de cotisations EN_RETARD sur toute la plateforme", example = "23")
    private long cotisationsEnRetard;

    @Schema(
        description =
            "Somme des montants des cotisations VALIDÉES depuis le 1er du mois courant en FCFA",
        example = "14500000")
    private BigDecimal montantCotisationsCeMois;
  }

  @Schema(description = "Activité des dernières 24 heures sur la plateforme")
  @Getter
  @Builder
  public static class ActiviteRecente {

    @Schema(description = "Nouveaux comptes inscrits dans les 24 dernières heures", example = "8")
    private long nouveauxInscrits;

    @Schema(description = "Cotisations enregistrées dans les 24 dernières heures", example = "45")
    private long cotisationsEnregistrees;
  }
}
