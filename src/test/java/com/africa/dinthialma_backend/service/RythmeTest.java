package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.calendrier.CalendrierTours;
import com.africa.dinthialma_backend.tontine.calendrier.CalendrierTours.PeriodeTour;
import com.africa.dinthialma_backend.tontine.calendrier.Rythme;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.UniteRythme;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Rythme et calendrier des tours (B26)")
class RythmeTest {

  private static final LocalDate JEUDI_12_NOV = LocalDate.of(2026, 11, 12);
  private static final LocalDate MERCREDI_11_NOV = LocalDate.of(2026, 11, 11);

  private static List<LocalDate> fins(List<PeriodeTour> periodes) {
    return periodes.stream().map(PeriodeTour::fin).toList();
  }

  private static void refuse(ThrowingCallable appel, ErrorCode code) {
    assertThatThrownBy(appel)
        .isInstanceOfSatisfying(
            BadRequestException.class, e -> assertThat(e.getCode()).isEqualTo(code));
  }

  @Nested
  @DisplayName("lecture et validation")
  class Lecture {

    @Test
    @DisplayName("valeurs normalisées : unité et jour en majuscules, jour du mois sans zéro")
    void normalisation() throws CustomException {
      Rythme r = Rythme.lire(2, "semaine", " dimanche ", "07:30");
      assertThat(r).isEqualTo(new Rythme(2, UniteRythme.SEMAINE, "DIMANCHE", LocalTime.of(7, 30)));
      assertThat(Rythme.lire(1, "MOIS", "05", null).jourEcheance()).isEqualTo("5");
      assertThat(Rythme.lire(1, "MOIS", "dernier", "").jourEcheance()).isEqualTo("DERNIER");
      assertThat(Rythme.lire(3, "JOUR", " ", null))
          .isEqualTo(new Rythme(3, UniteRythme.JOUR, null, Rythme.HEURE_PAR_DEFAUT));
    }

    @Test
    @DisplayName("bornes : JOUR 1–90, SEMAINE 1–26, MOIS 1–12 ; unité inconnue -> RYTHME_INVALIDE")
    void bornes() throws CustomException {
      assertThat(Rythme.lire(90, "JOUR", null, null).intervalle()).isEqualTo(90);
      assertThat(Rythme.lire(26, "SEMAINE", null, null).intervalle()).isEqualTo(26);
      assertThat(Rythme.lire(12, "MOIS", null, null).intervalle()).isEqualTo(12);
      refuse(() -> Rythme.lire(91, "JOUR", null, null), ErrorCode.RYTHME_INVALIDE);
      refuse(() -> Rythme.lire(27, "SEMAINE", null, null), ErrorCode.RYTHME_INVALIDE);
      refuse(() -> Rythme.lire(13, "MOIS", null, null), ErrorCode.RYTHME_INVALIDE);
      refuse(() -> Rythme.lire(0, "JOUR", null, null), ErrorCode.RYTHME_INVALIDE);
      refuse(() -> Rythme.lire(null, "JOUR", null, null), ErrorCode.RYTHME_INVALIDE);
      refuse(() -> Rythme.lire(1, "ANNEE", null, null), ErrorCode.RYTHME_INVALIDE);
      refuse(() -> Rythme.lire(1, null, null, null), ErrorCode.RYTHME_INVALIDE);
    }

    @Test
    @DisplayName("jour incohérent avec l'unité -> JOUR_ECHEANCE_INVALIDE ; heure -> HEURE_…")
    void jourEtHeure() {
      refuse(() -> Rythme.lire(1, "JOUR", "LUNDI", null), ErrorCode.JOUR_ECHEANCE_INVALIDE);
      refuse(() -> Rythme.lire(1, "SEMAINE", "15", null), ErrorCode.JOUR_ECHEANCE_INVALIDE);
      refuse(() -> Rythme.lire(1, "MOIS", "LUNDI", null), ErrorCode.JOUR_ECHEANCE_INVALIDE);
      refuse(() -> Rythme.lire(1, "MOIS", "32", null), ErrorCode.JOUR_ECHEANCE_INVALIDE);
      refuse(() -> Rythme.lire(1, "MOIS", "0", null), ErrorCode.JOUR_ECHEANCE_INVALIDE);
      refuse(() -> Rythme.lire(1, "MOIS", null, "25:00"), ErrorCode.HEURE_ECHEANCE_INVALIDE);
      refuse(() -> Rythme.lire(1, "MOIS", null, "8h"), ErrorCode.HEURE_ECHEANCE_INVALIDE);
    }

    @Test
    @DisplayName("fréquences historiques ⇄ rythme ; autre rythme -> PERSONNALISEE")
    void frequences() {
      for (String code :
          List.of("JOURNALIERE", "HEBDOMADAIRE", "BIMENSUEL", "MENSUEL", "TRIMESTRIEL")) {
        assertThat(Rythme.depuisFrequence(code.toLowerCase()).orElseThrow().frequence())
            .isEqualTo(code);
      }
      assertThat(Rythme.depuisFrequence("BIMENSUEL").orElseThrow())
          .isEqualTo(new Rythme(2, UniteRythme.SEMAINE, null, Rythme.HEURE_PAR_DEFAUT));
      assertThat(Rythme.depuisFrequence("ANNUEL")).isEmpty();
      assertThat(Rythme.depuisFrequence(null)).isEmpty();
      assertThat(new Rythme(3, UniteRythme.JOUR, null, null).frequence())
          .isEqualTo(Rythme.PERSONNALISEE);
      assertThat(new Rythme(3, UniteRythme.SEMAINE, null, null).frequence())
          .isEqualTo(Rythme.PERSONNALISEE);
      assertThat(new Rythme(6, UniteRythme.MOIS, null, null).frequence())
          .isEqualTo(Rythme.PERSONNALISEE);
    }

    @Test
    @DisplayName("résolution : rythme prioritaire, sinon fréquence (+ jour/heure), sinon refus")
    void resolution() throws CustomException {
      assertThat(Rythme.resoudre(3, "JOUR", null, null, "MENSUEL").unite())
          .isEqualTo(UniteRythme.JOUR);
      Rythme mensuel = Rythme.resoudre(null, null, "30", "20:00", "MENSUEL");
      assertThat(mensuel).isEqualTo(new Rythme(1, UniteRythme.MOIS, "30", LocalTime.of(20, 0)));
      refuse(() -> Rythme.resoudre(null, null, null, null, null), ErrorCode.RYTHME_INVALIDE);
      refuse(() -> Rythme.resoudre(null, null, null, null, "ANNUEL"), ErrorCode.RYTHME_INVALIDE);
      refuse(() -> Rythme.resoudre(3, null, null, null, null), ErrorCode.RYTHME_INVALIDE);
    }

    @Test
    @DisplayName("modification : champ absent conservé, jour vide retiré, fréquence traduite")
    void modification() throws CustomException {
      Rythme actuel = new Rythme(1, UniteRythme.SEMAINE, "DIMANCHE", LocalTime.of(20, 0));
      assertThat(actuel.modifier(null, null, null, null, null)).isEqualTo(actuel);
      assertThat(actuel.modifier(2, null, null, null, null))
          .isEqualTo(new Rythme(2, UniteRythme.SEMAINE, "DIMANCHE", LocalTime.of(20, 0)));
      assertThat(actuel.modifier(null, null, "", "23:00", null))
          .isEqualTo(new Rythme(1, UniteRythme.SEMAINE, null, LocalTime.of(23, 0)));
      assertThat(actuel.modifier(null, null, "", null, "MENSUEL").unite())
          .isEqualTo(UniteRythme.MOIS);
      // changement d'unité avec un jour devenu incohérent
      refuse(
          () -> actuel.modifier(null, "MOIS", null, null, null), ErrorCode.JOUR_ECHEANCE_INVALIDE);
      assertThat(actuel.memeCadence(actuel.modifier(null, null, "LUNDI", null, null))).isTrue();
      assertThat(actuel.memeCadence(actuel.modifier(3, null, null, null, null))).isFalse();
      assertThat(actuel.memeCadence(new Rythme(1, UniteRythme.MOIS, null, null))).isFalse();
    }

    @Test
    @DisplayName("libellé lisible")
    void libelle() {
      assertThat(new Rythme(3, UniteRythme.JOUR, null, null).libelle())
          .isEqualTo("tous les 3 jours, avant 23h59");
      assertThat(new Rythme(1, UniteRythme.JOUR, null, LocalTime.of(18, 0)).libelle())
          .isEqualTo("chaque jour, avant 18h00");
      assertThat(new Rythme(1, UniteRythme.SEMAINE, "DIMANCHE", null).libelle())
          .isEqualTo("chaque semaine le dimanche, avant 23h59");
      assertThat(new Rythme(2, UniteRythme.SEMAINE, null, null).libelle())
          .startsWith("toutes les 2 semaines");
      assertThat(new Rythme(1, UniteRythme.MOIS, "30", null).libelle())
          .isEqualTo("chaque mois le 30, avant 23h59");
      assertThat(new Rythme(6, UniteRythme.MOIS, "DERNIER", null).libelle())
          .isEqualTo("tous les 6 mois le dernier jour, avant 23h59");
      assertThat(Rythme.formaterHeure(null)).isNull();
    }
  }

  @Nested
  @DisplayName("calendrier (critères d'acceptation B26)")
  class Calendrier {

    @Test
    @DisplayName(
        "« tous les 3 jours » à partir du 12/11, 10 membres -> 10 tours : 14/11, 17/11… à 23h59")
    void tousLesTroisJours() throws CustomException {
      List<PeriodeTour> tours =
          CalendrierTours.generer(Rythme.lire(3, "JOUR", null, null), JEUDI_12_NOV, 10, null);
      assertThat(tours).hasSize(10);
      assertThat(tours.get(0).fin()).isEqualTo(LocalDate.of(2026, 11, 14));
      assertThat(tours.get(1).debut()).isEqualTo(LocalDate.of(2026, 11, 15));
      assertThat(tours.get(1).fin()).isEqualTo(LocalDate.of(2026, 11, 17));
      assertThat(tours.get(9).fin()).isEqualTo(LocalDate.of(2026, 12, 11));
      assertThat(tours.get(0).echeanceAt()).isEqualTo(LocalDate.of(2026, 11, 14).atTime(23, 59));
    }

    @Test
    @DisplayName("« chaque semaine le dimanche » à partir d'un mercredi -> dimanche suivant")
    void chaqueDimanche() throws CustomException {
      Rythme dimanche = Rythme.lire(1, "SEMAINE", "DIMANCHE", "20:00");
      assertThat(fins(CalendrierTours.generer(dimanche, MERCREDI_11_NOV, 3, null)))
          .containsExactly(
              LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 29));
      // démarrage un dimanche : 1ʳᵉ échéance le dimanche d'après (décision 2026-11-13)
      assertThat(
              CalendrierTours.generer(dimanche, LocalDate.of(2026, 11, 15), 1, null).get(0).fin())
          .isEqualTo(LocalDate.of(2026, 11, 22));
      // toutes les 2 semaines
      assertThat(
              fins(
                  CalendrierTours.generer(
                      Rythme.lire(2, "SEMAINE", "DIMANCHE", null), MERCREDI_11_NOV, 2, null)))
          .containsExactly(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 29));
    }

    @Test
    @DisplayName("« chaque mois le 31 » -> 30/11, 31/12, 31/01, 28/02, 31/03")
    void chaqueMoisLe31() throws CustomException {
      assertThat(
              fins(
                  CalendrierTours.generer(
                      Rythme.lire(1, "MOIS", "31", null), JEUDI_12_NOV, 5, null)))
          .containsExactly(
              LocalDate.of(2026, 11, 30),
              LocalDate.of(2026, 12, 31),
              LocalDate.of(2027, 1, 31),
              LocalDate.of(2027, 2, 28),
              LocalDate.of(2027, 3, 31));
    }

    @Test
    @DisplayName(
        "mois : le 30 après un 28 février, DERNIER, démarrage le jour même, tous les 2 mois")
    void moisParticuliers() throws CustomException {
      assertThat(
              fins(
                  CalendrierTours.generer(
                      Rythme.lire(1, "MOIS", "30", null), LocalDate.of(2027, 2, 1), 2, null)))
          .containsExactly(LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 30));
      assertThat(
              fins(
                  CalendrierTours.generer(
                      Rythme.lire(1, "MOIS", "DERNIER", null), JEUDI_12_NOV, 2, null)))
          .containsExactly(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 31));
      assertThat(
              CalendrierTours.generer(
                      Rythme.lire(1, "MOIS", "30", null), LocalDate.of(2026, 11, 30), 1, null)
                  .get(0)
                  .fin())
          .isEqualTo(LocalDate.of(2026, 12, 30));
      assertThat(
              fins(
                  CalendrierTours.generer(
                      Rythme.lire(2, "MOIS", "5", null), JEUDI_12_NOV, 2, null)))
          .containsExactly(LocalDate.of(2026, 12, 5), LocalDate.of(2027, 2, 5));
    }

    @Test
    @DisplayName("fréquences historiques : mêmes dates qu'avant B26")
    void compatibilite() {
      LocalDate debut = LocalDate.of(2026, 1, 1);
      assertThat(fins(CalendrierTours.generer(rythme("MENSUEL"), debut, 2, null)))
          .containsExactly(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28));
      assertThat(fins(CalendrierTours.generer(rythme("JOURNALIERE"), debut, 2, null)))
          .containsExactly(debut, debut.plusDays(1));
      assertThat(fins(CalendrierTours.generer(rythme("HEBDOMADAIRE"), debut, 1, null)))
          .containsExactly(LocalDate.of(2026, 1, 7));
      assertThat(fins(CalendrierTours.generer(rythme("BIMENSUEL"), debut, 1, null)))
          .containsExactly(LocalDate.of(2026, 1, 14));
      assertThat(fins(CalendrierTours.generer(rythme("TRIMESTRIEL"), debut, 1, null)))
          .containsExactly(LocalDate.of(2026, 3, 31));
    }

    @Test
    @DisplayName("date limite (événementielle) : dernier tour tronqué ; aucun tour si dépassée")
    void dateLimite() {
      List<PeriodeTour> tours =
          CalendrierTours.generer(
              rythme("HEBDOMADAIRE"),
              LocalDate.of(2026, 1, 1),
              Integer.MAX_VALUE,
              LocalDate.of(2026, 1, 17));
      assertThat(fins(tours))
          .containsExactly(
              LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 14), LocalDate.of(2026, 1, 17));
      assertThat(
              CalendrierTours.generer(
                  rythme("MENSUEL"),
                  LocalDate.of(2026, 2, 1),
                  Integer.MAX_VALUE,
                  LocalDate.of(2026, 1, 31)))
          .isEmpty();
      assertThat(
              CalendrierTours.generer(
                  rythme("JOURNALIERE"), LocalDate.of(2026, 1, 1), Integer.MAX_VALUE, null))
          .hasSize(CalendrierTours.TOURS_MAXIMUM);
    }

    @Test
    @DisplayName(
        "suite d'un calendrier : alignée -> + intervalle ; jour changé -> prochaine occurrence")
    void suiteDeCalendrier() throws CustomException {
      Rythme deuxSemainesDimanche = Rythme.lire(2, "SEMAINE", "DIMANCHE", null);
      LocalDate dimanche = LocalDate.of(2026, 11, 15);
      assertThat(
              CalendrierTours.generer(deuxSemainesDimanche, dimanche.plusDays(1), 1, null, dimanche)
                  .get(0)
                  .fin())
          .isEqualTo(LocalDate.of(2026, 11, 29));
      // échéance précédente un samedi : on ne rallonge pas le tour à 3 semaines
      LocalDate samedi = LocalDate.of(2026, 11, 14);
      assertThat(deuxSemainesDimanche.finDuTour(samedi.plusDays(1), samedi))
          .isEqualTo(LocalDate.of(2026, 11, 22));
      // mois : précédente le 10 alors que le jour est le 30
      Rythme le30 = Rythme.lire(1, "MOIS", "30", null);
      assertThat(le30.finDuTour(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 10)))
          .isEqualTo(LocalDate.of(2026, 11, 30));
      assertThat(le30.finDuTour(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 30)))
          .isEqualTo(LocalDate.of(2026, 12, 30));
    }

    @Test
    @DisplayName("tour déplacé : dates et échéance active mises à jour, échéance supprimée ignorée")
    void deplacer() {
      CycleTontine cycle =
          CalendrierTours.tour(
              Fixtures.tontine(Fixtures.user("G")),
              1,
              new PeriodeTour(
                  JEUDI_12_NOV, JEUDI_12_NOV.plusDays(2), JEUDI_12_NOV.plusDays(2).atTime(23, 59)),
              CycleStatut.EN_ATTENTE);
      PeriodeTour nouvelle =
          new PeriodeTour(
              JEUDI_12_NOV, JEUDI_12_NOV.plusDays(5), JEUDI_12_NOV.plusDays(5).atTime(8, 0));
      CalendrierTours.deplacer(cycle, nouvelle);
      assertThat(cycle.getDateFin()).isEqualTo(nouvelle.fin());
      assertThat(cycle.getEcheances().get(0).getEcheanceAt()).isEqualTo(nouvelle.echeanceAt());

      cycle.getEcheances().get(0).setDeletedAt(LocalDateTime.now());
      CalendrierTours.deplacer(
          cycle, new PeriodeTour(JEUDI_12_NOV, JEUDI_12_NOV, JEUDI_12_NOV.atTime(9, 0)));
      assertThat(cycle.getEcheances().get(0).getEcheanceAt()).isEqualTo(nouvelle.echeanceAt());
    }

    @Test
    @DisplayName("tour créé avec son échéance (numéro 1, mêmes dates)")
    void tourAvecEcheance() {
      Tontine tontine = Fixtures.tontine(Fixtures.user("G"));
      PeriodeTour periode =
          new PeriodeTour(
              LocalDate.of(2026, 11, 12),
              LocalDate.of(2026, 11, 14),
              LocalDate.of(2026, 11, 14).atTime(20, 0));
      CycleTontine cycle = CalendrierTours.tour(tontine, 3, periode, CycleStatut.EN_ATTENTE);
      assertThat(cycle.getNumeroCycle()).isEqualTo(3);
      assertThat(cycle.getEcheanceAt()).isEqualTo(periode.echeanceAt());
      assertThat(cycle.getEcheances()).hasSize(1);
      assertThat(cycle.getEcheances().get(0).getNumero()).isEqualTo(1);
      assertThat(cycle.getEcheances().get(0).getCycle()).isSameAs(cycle);
      assertThat(cycle.getEcheances().get(0).getDateDebut()).isEqualTo(periode.debut());
    }

    @Test
    @DisplayName("tontine sans rythme enregistré : repli sur la fréquence, puis sur MENSUEL")
    void replisTontine() {
      Tontine tontine = Fixtures.tontine(Fixtures.user("G"));
      tontine.setFrequence("HEBDOMADAIRE");
      assertThat(tontine.getRythme().unite()).isEqualTo(UniteRythme.SEMAINE);
      tontine.setFrequence("INCONNUE");
      assertThat(tontine.getRythme().unite()).isEqualTo(UniteRythme.MOIS);
      tontine.appliquerRythme(new Rythme(3, UniteRythme.JOUR, null, LocalTime.of(9, 0)));
      assertThat(tontine.getFrequence()).isEqualTo(Rythme.PERSONNALISEE);
      tontine.setHeureEcheance(null);
      assertThat(tontine.getRythme().heureEcheance()).isEqualTo(Rythme.HEURE_PAR_DEFAUT);
    }

    private Rythme rythme(String frequence) {
      return Rythme.depuisFrequence(frequence).orElseThrow();
    }
  }
}
