package com.africa.dinthialma_backend.tontine.calendrier;

import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.tontine.codeList.UniteRythme;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Rythme de cotisation d'une tontine (B26) : « tous les {@code intervalle} {@code unite} », avec un
 * jour d'échéance facultatif (jour de la semaine, jour du mois ou dernier jour du mois) et une
 * heure limite de paiement.
 *
 * <p>Règle de calcul de l'échéance d'un tour (voir {@link #finDuTour(LocalDate, LocalDate)}) :
 *
 * <ul>
 *   <li>sans jour d'échéance : début du tour + intervalle − 1 jour (fonctionnement historique des
 *       fréquences MENSUEL, HEBDOMADAIRE…) ;
 *   <li>avec un jour d'échéance : 1ʳᵉ échéance = prochaine occurrence de ce jour <b>strictement
 *       après</b> le début de la tontine, puis une occurrence tous les {@code intervalle} ; un jour
 *       29, 30 ou 31 absent du mois tombe sur le dernier jour du mois.
 * </ul>
 */
public record Rythme(
    int intervalle, UniteRythme unite, String jourEcheance, LocalTime heureEcheance) {

  public static final LocalTime HEURE_PAR_DEFAUT = LocalTime.of(23, 59);

  /** Valeur de {@code frequence} quand le rythme ne correspond à aucun code historique. */
  public static final String PERSONNALISEE = "PERSONNALISEE";

  /** Jour d'échéance mensuel : dernier jour du mois. */
  public static final String DERNIER = "DERNIER";

  private static final List<String> JOURS_SEMAINE =
      List.of("LUNDI", "MARDI", "MERCREDI", "JEUDI", "VENDREDI", "SAMEDI", "DIMANCHE");

  private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH:mm");

  // ─── Construction et validation ──────────────────────────────────────────

  /**
   * Lit et valide un rythme saisi.
   *
   * @param unite JOUR, SEMAINE ou MOIS (casse indifférente)
   * @param jour jour d'échéance (null ou vide = aucun)
   * @param heure heure limite {@code HH:mm} (null ou vide = 23:59)
   * @throws CustomException {@code RYTHME_INVALIDE}, {@code JOUR_ECHEANCE_INVALIDE} ou {@code
   *     HEURE_ECHEANCE_INVALIDE}
   */
  public static Rythme lire(Integer intervalle, String unite, String jour, String heure)
      throws CustomException {
    UniteRythme u = lireUnite(unite);
    if (intervalle == null || intervalle < 1 || intervalle > u.getIntervalleMaximum()) {
      throw new BadRequestException(
          "Intervalle invalide pour l'unité "
              + u
              + " : entre 1 et "
              + u.getIntervalleMaximum()
              + " attendu",
          ErrorCode.RYTHME_INVALIDE);
    }
    return new Rythme(intervalle, u, lireJour(u, jour), lireHeure(heure));
  }

  /** Rythme équivalent à un code historique de fréquence (casse indifférente). */
  public static Optional<Rythme> depuisFrequence(String frequence) {
    if (frequence == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        switch (frequence.trim().toUpperCase(Locale.ROOT)) {
          case "JOURNALIERE" -> new Rythme(1, UniteRythme.JOUR, null, HEURE_PAR_DEFAUT);
          case "HEBDOMADAIRE" -> new Rythme(1, UniteRythme.SEMAINE, null, HEURE_PAR_DEFAUT);
          case "BIMENSUEL" -> new Rythme(2, UniteRythme.SEMAINE, null, HEURE_PAR_DEFAUT);
          case "MENSUEL" -> new Rythme(1, UniteRythme.MOIS, null, HEURE_PAR_DEFAUT);
          case "TRIMESTRIEL" -> new Rythme(3, UniteRythme.MOIS, null, HEURE_PAR_DEFAUT);
          default -> null;
        });
  }

  /**
   * Rythme demandé à la création : les champs du rythme priment ; à défaut, la fréquence historique
   * est traduite ; le jour et l'heure d'échéance s'appliquent dans les deux cas.
   *
   * @throws CustomException {@code RYTHME_INVALIDE} si aucun rythme n'est fourni
   */
  public static Rythme resoudre(
      Integer intervalle, String unite, String jour, String heure, String frequence)
      throws CustomException {
    if (intervalle != null || unite != null) {
      return lire(intervalle, unite, jour, heure);
    }
    return traduire(frequence).avecEcheance(jour, heure);
  }

  /**
   * Rythme modifié : un champ absent (null) garde sa valeur ; {@code jour} vide retire le jour
   * imposé.
   */
  public Rythme modifier(
      Integer nouvelIntervalle,
      String nouvelleUnite,
      String jour,
      String heure,
      String nouvelleFrequence)
      throws CustomException {
    Rythme base = this;
    if (nouvelIntervalle != null || nouvelleUnite != null) {
      base =
          lire(
              nouvelIntervalle != null ? nouvelIntervalle : intervalle,
              nouvelleUnite != null ? nouvelleUnite : unite.name(),
              null,
              null);
    } else if (nouvelleFrequence != null) {
      base = traduire(nouvelleFrequence);
    }
    return base.avecEcheance(
        jour != null ? jour : jourEcheance, heure != null ? heure : formaterHeure(heureEcheance));
  }

  /** Même intervalle et même unité (le jour et l'heure peuvent différer). */
  public boolean memeCadence(Rythme autre) {
    return intervalle == autre.intervalle && unite == autre.unite;
  }

  private static Rythme traduire(String frequence) throws CustomException {
    if (frequence == null || frequence.isBlank()) {
      throw new BadRequestException(
          "Rythme obligatoire : rythmeIntervalle + rythmeUnite, ou frequence",
          ErrorCode.RYTHME_INVALIDE);
    }
    return depuisFrequence(frequence)
        .orElseThrow(
            () ->
                new BadRequestException(
                    "Fréquence inconnue : « "
                        + frequence
                        + " ». Valeurs acceptées : JOURNALIERE, HEBDOMADAIRE, BIMENSUEL, MENSUEL,"
                        + " TRIMESTRIEL (ou rythmeIntervalle + rythmeUnite)",
                    ErrorCode.RYTHME_INVALIDE));
  }

  /** Même rythme avec un autre jour et une autre heure d'échéance (validés). */
  public Rythme avecEcheance(String jour, String heure) throws CustomException {
    return new Rythme(intervalle, unite, lireJour(unite, jour), lireHeure(heure));
  }

  private static UniteRythme lireUnite(String unite) throws CustomException {
    try {
      return UniteRythme.valueOf(unite.trim().toUpperCase(Locale.ROOT));
    } catch (RuntimeException e) {
      throw new BadRequestException(
          "Unité de rythme inconnue : « " + unite + " ». Valeurs acceptées : JOUR, SEMAINE, MOIS",
          ErrorCode.RYTHME_INVALIDE);
    }
  }

  private static String lireJour(UniteRythme unite, String jour) throws CustomException {
    if (jour == null || jour.isBlank()) {
      return null;
    }
    String valeur = jour.trim().toUpperCase(Locale.ROOT);
    if (unite == UniteRythme.SEMAINE && JOURS_SEMAINE.contains(valeur)) {
      return valeur;
    }
    if (unite == UniteRythme.MOIS) {
      if (DERNIER.equals(valeur)) {
        return valeur;
      }
      Integer n = entier(valeur);
      if (n != null && n >= 1 && n <= 31) {
        return String.valueOf(n);
      }
    }
    throw new BadRequestException(
        "Jour d'échéance « "
            + jour
            + " » incohérent avec l'unité "
            + unite
            + " (SEMAINE : LUNDI…DIMANCHE ; MOIS : 1…31 ou DERNIER ; JOUR : aucun)",
        ErrorCode.JOUR_ECHEANCE_INVALIDE);
  }

  private static LocalTime lireHeure(String heure) throws CustomException {
    if (heure == null || heure.isBlank()) {
      return HEURE_PAR_DEFAUT;
    }
    try {
      return LocalTime.parse(heure.trim(), FORMAT_HEURE);
    } catch (DateTimeParseException e) {
      throw new BadRequestException(
          "Heure d'échéance invalide : « " + heure + " » (format HH:mm attendu, ex. 23:59)",
          ErrorCode.HEURE_ECHEANCE_INVALIDE);
    }
  }

  private static Integer entier(String valeur) {
    try {
      return Integer.valueOf(valeur);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  // ─── Lecture ─────────────────────────────────────────────────────────────

  /** Code historique correspondant (intervalle + unité), sinon {@link #PERSONNALISEE}. */
  public String frequence() {
    return switch (unite) {
      case JOUR -> intervalle == 1 ? "JOURNALIERE" : PERSONNALISEE;
      case SEMAINE ->
          switch (intervalle) {
            case 1 -> "HEBDOMADAIRE";
            case 2 -> "BIMENSUEL";
            default -> PERSONNALISEE;
          };
      case MOIS ->
          switch (intervalle) {
            case 1 -> "MENSUEL";
            case 3 -> "TRIMESTRIEL";
            default -> PERSONNALISEE;
          };
    };
  }

  /** Libellé lisible, ex. « tous les 3 jours, avant 23h59 », « chaque mois le 30, avant 20h00 ». */
  public String libelle() {
    String cadence =
        switch (unite) {
          case JOUR -> intervalle == 1 ? "chaque jour" : "tous les " + intervalle + " jours";
          case SEMAINE ->
              intervalle == 1 ? "chaque semaine" : "toutes les " + intervalle + " semaines";
          case MOIS -> intervalle == 1 ? "chaque mois" : "tous les " + intervalle + " mois";
        };
    String jour = "";
    if (jourEcheance != null) {
      if (unite == UniteRythme.SEMAINE) {
        jour = " le " + jourEcheance.toLowerCase(Locale.ROOT);
      } else {
        jour = DERNIER.equals(jourEcheance) ? " le dernier jour" : " le " + jourEcheance;
      }
    }
    LocalTime h = heureEcheance != null ? heureEcheance : HEURE_PAR_DEFAUT;
    return cadence + jour + ", avant " + h.format(DateTimeFormatter.ofPattern("HH'h'mm"));
  }

  /** Heure d'échéance au format {@code HH:mm}. */
  public static String formaterHeure(LocalTime heure) {
    return heure == null ? null : heure.format(FORMAT_HEURE);
  }

  // ─── Calcul des échéances ────────────────────────────────────────────────

  /**
   * Dernier jour (jour d'échéance) d'un tour.
   *
   * @param debut premier jour du tour
   * @param echeancePrecedente jour d'échéance du tour précédent (null pour le 1ᵉʳ tour)
   */
  public LocalDate finDuTour(LocalDate debut, LocalDate echeancePrecedente) {
    if (jourEcheance == null) {
      return ajouter(debut, intervalle).minusDays(1);
    }
    if (echeancePrecedente != null) {
      LocalDate suivante = suivante(echeancePrecedente);
      if (!suivante.isBefore(debut)) {
        return suivante;
      }
    }
    return premiereOccurrenceApres(debut);
  }

  /** Date-heure limite de paiement d'un tour qui se termine le jour {@code fin}. */
  public LocalDateTime echeanceAt(LocalDate fin) {
    return fin.atTime(heureEcheance != null ? heureEcheance : HEURE_PAR_DEFAUT);
  }

  private LocalDate ajouter(LocalDate date, int n) {
    return switch (unite) {
      case JOUR -> date.plusDays(n);
      case SEMAINE -> date.plusWeeks(n);
      case MOIS -> date.plusMonths(n);
    };
  }

  private LocalDate suivante(LocalDate echeance) {
    if (unite == UniteRythme.SEMAINE) {
      return echeance.plusWeeks(intervalle).with(TemporalAdjusters.nextOrSame(jourSemaine()));
    }
    return jourDuMois(YearMonth.from(echeance).plusMonths(intervalle));
  }

  private LocalDate premiereOccurrenceApres(LocalDate date) {
    if (unite == UniteRythme.SEMAINE) {
      return date.with(TemporalAdjusters.next(jourSemaine()));
    }
    LocalDate ceMois = jourDuMois(YearMonth.from(date));
    return ceMois.isAfter(date) ? ceMois : jourDuMois(YearMonth.from(date).plusMonths(1));
  }

  private DayOfWeek jourSemaine() {
    return DayOfWeek.of(JOURS_SEMAINE.indexOf(jourEcheance) + 1);
  }

  private LocalDate jourDuMois(YearMonth mois) {
    if (DERNIER.equals(jourEcheance)) {
      return mois.atEndOfMonth();
    }
    return mois.atDay(Math.min(Integer.parseInt(jourEcheance), mois.lengthOfMonth()));
  }
}
