package com.africa.dinthialma_backend.tontine.calendrier;

import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.EcheanceCycle;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Calendrier des tours d'une tontine (B26) : génération des périodes selon le {@link Rythme} et
 * création des tours avec leur échéance.
 */
public final class CalendrierTours {

  /** Garde-fou : nombre maximal de tours générés en une fois. */
  public static final int TOURS_MAXIMUM = 5000;

  private CalendrierTours() {}

  /** Période d'un tour : premier jour, jour d'échéance et date-heure limite de paiement. */
  public record PeriodeTour(LocalDate debut, LocalDate fin, LocalDateTime echeanceAt) {}

  /**
   * Périodes successives : chaque tour commence le lendemain de l'échéance du précédent.
   *
   * @param nombreTours nombre de tours voulus (rotative), {@link Integer#MAX_VALUE} pour s'arrêter
   *     sur {@code dateLimite}
   * @param dateLimite dernier jour possible (événementielle : le dernier tour s'y termine, même
   *     incomplet) ; null = aucune
   */
  public static List<PeriodeTour> generer(
      Rythme rythme, LocalDate dateDebut, int nombreTours, LocalDate dateLimite) {
    List<PeriodeTour> periodes = new ArrayList<>();
    LocalDate debut = dateDebut;
    LocalDate precedente = null;
    int maximum = Math.min(nombreTours, TOURS_MAXIMUM);
    while (periodes.size() < maximum && (dateLimite == null || !debut.isAfter(dateLimite))) {
      LocalDate fin = rythme.finDuTour(debut, precedente);
      if (dateLimite != null && fin.isAfter(dateLimite)) {
        fin = dateLimite;
      }
      periodes.add(new PeriodeTour(debut, fin, rythme.echeanceAt(fin)));
      precedente = fin;
      debut = fin.plusDays(1);
    }
    return periodes;
  }

  /** Nouveau tour (non enregistré) avec son unique échéance (une cotisation par tour). */
  public static CycleTontine tour(
      Tontine tontine, int numero, PeriodeTour periode, CycleStatut statut) {
    CycleTontine cycle =
        CycleTontine.builder()
            .tontine(tontine)
            .numeroCycle(numero)
            .dateDebut(periode.debut())
            .dateFin(periode.fin())
            .echeanceAt(periode.echeanceAt())
            .statut(statut)
            .build();
    cycle
        .getEcheances()
        .add(
            EcheanceCycle.builder()
                .cycle(cycle)
                .numero(1)
                .dateDebut(periode.debut())
                .echeanceAt(periode.echeanceAt())
                .build());
    return cycle;
  }
}
