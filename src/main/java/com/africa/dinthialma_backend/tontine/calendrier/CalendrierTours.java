package com.africa.dinthialma_backend.tontine.calendrier;

import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.EcheanceCycle;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    return generer(rythme, dateDebut, nombreTours, dateLimite, null);
  }

  /**
   * Variante qui poursuit un calendrier existant : {@code echeancePrecedente} = jour d'échéance du
   * dernier tour déjà commencé (null = premier tour de la tontine).
   */
  public static List<PeriodeTour> generer(
      Rythme rythme,
      LocalDate dateDebut,
      int nombreTours,
      LocalDate dateLimite,
      LocalDate echeancePrecedente) {
    List<PeriodeTour> periodes = new ArrayList<>();
    LocalDate debut = dateDebut;
    LocalDate precedente = echeancePrecedente;
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

  /**
   * Regroupe des échéances consécutives en tours de {@code parTour} échéances (B32) ; le dernier
   * groupe peut être plus court (événementielle tronquée sur la date de l'événement).
   */
  public static List<List<PeriodeTour>> grouper(List<PeriodeTour> echeances, int parTour) {
    int taille = Math.max(1, parTour);
    List<List<PeriodeTour>> tours = new ArrayList<>();
    for (int i = 0; i < echeances.size(); i += taille) {
      tours.add(List.copyOf(echeances.subList(i, Math.min(i + taille, echeances.size()))));
    }
    return tours;
  }

  /**
   * Échéances d'un tour ouvert à la main (B32) : sans date de fin, les {@code parTour} échéances du
   * rythme à partir de {@code debut} ; avec une date de fin choisie, les échéances du rythme qui
   * tombent dans [debut, dateFin], la dernière ramenée sur {@code dateFin} (au moins une).
   *
   * <p>Reliquat court : si la dernière période, coupée sur {@code dateFin}, fait moins de la moitié
   * d'une période complète du rythme, elle est fusionnée dans l'échéance précédente, prolongée
   * jusqu'à {@code dateFin} (mensuel du 14/11 au 14/12 = une seule échéance, pas une échéance d'un
   * jour le 14/12).
   *
   * @param echeancePrecedente jour d'échéance du tour précédent (null s'il n'y en a pas)
   */
  public static List<PeriodeTour> echeancesDuTour(
      Rythme rythme,
      int parTour,
      LocalDate debut,
      LocalDate dateFin,
      LocalDate echeancePrecedente) {
    if (dateFin == null) {
      return generer(rythme, debut, Math.max(1, parTour), null, echeancePrecedente);
    }
    List<PeriodeTour> echeances =
        new ArrayList<>(generer(rythme, debut, Integer.MAX_VALUE, dateFin, echeancePrecedente));
    int n = echeances.size();
    if (n >= 2) {
      PeriodeTour derniere = echeances.get(n - 1);
      PeriodeTour avant = echeances.get(n - 2);
      LocalDate finComplete = rythme.finDuTour(derniere.debut(), avant.fin());
      long reliquat = ChronoUnit.DAYS.between(derniere.debut(), derniere.fin()) + 1;
      long complete = ChronoUnit.DAYS.between(derniere.debut(), finComplete) + 1;
      if (reliquat * 2 < complete) {
        echeances.remove(n - 1);
        echeances.set(n - 2, new PeriodeTour(avant.debut(), dateFin, rythme.echeanceAt(dateFin)));
      }
    }
    return List.copyOf(echeances);
  }

  /** Replace un tour sur une nouvelle période (une seule échéance). */
  public static void deplacer(CycleTontine cycle, PeriodeTour periode) {
    deplacer(cycle, List.of(periode));
  }

  /**
   * Replace un tour et ses échéances : la i-ème échéance active prend la i-ème période ; les
   * échéances en trop sont retirées (suppression logique), celles qui manquent sont ajoutées.
   */
  public static void deplacer(CycleTontine cycle, List<PeriodeTour> echeances) {
    PeriodeTour derniere = echeances.get(echeances.size() - 1);
    cycle.setDateDebut(echeances.get(0).debut());
    cycle.setDateFin(derniere.fin());
    cycle.setEcheanceAt(derniere.echeanceAt());
    List<EcheanceCycle> actives =
        cycle.getEcheances().stream().filter(e -> e.getDeletedAt() == null).toList();
    for (int i = 0; i < Math.max(actives.size(), echeances.size()); i++) {
      if (i >= echeances.size()) {
        actives.get(i).setDeletedAt(LocalDateTime.now());
      } else if (i >= actives.size()) {
        cycle.getEcheances().add(echeance(cycle, i + 1, echeances.get(i)));
      } else {
        actives.get(i).setNumero(i + 1);
        actives.get(i).setDateDebut(echeances.get(i).debut());
        actives.get(i).setEcheanceAt(echeances.get(i).echeanceAt());
      }
    }
  }

  /** Nouveau tour (non enregistré) avec une seule échéance. */
  public static CycleTontine tour(
      Tontine tontine, int numero, PeriodeTour periode, CycleStatut statut) {
    return tour(tontine, numero, List.of(periode), statut);
  }

  /**
   * Nouveau tour (non enregistré) avec ses échéances numérotées de 1 à N (B32) : il commence avec
   * la première et se termine sur la dernière, dont il reprend la limite de paiement.
   */
  public static CycleTontine tour(
      Tontine tontine, int numero, List<PeriodeTour> echeances, CycleStatut statut) {
    PeriodeTour derniere = echeances.get(echeances.size() - 1);
    CycleTontine cycle =
        CycleTontine.builder()
            .tontine(tontine)
            .numeroCycle(numero)
            .dateDebut(echeances.get(0).debut())
            .dateFin(derniere.fin())
            .echeanceAt(derniere.echeanceAt())
            .statut(statut)
            .build();
    for (int i = 0; i < echeances.size(); i++) {
      cycle.getEcheances().add(echeance(cycle, i + 1, echeances.get(i)));
    }
    return cycle;
  }

  private static EcheanceCycle echeance(CycleTontine cycle, int numero, PeriodeTour periode) {
    return EcheanceCycle.builder()
        .cycle(cycle)
        .numero(numero)
        .dateDebut(periode.debut())
        .echeanceAt(periode.echeanceAt())
        .build();
  }
}
