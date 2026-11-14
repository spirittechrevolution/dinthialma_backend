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
import com.africa.dinthialma_backend.tontine.calendrier.RythmeJackpot;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.StatutEcheance;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.EcheanceCycle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Rythme du jackpot : N cotisations par tour (B32)")
class RythmeJackpotTest {

  private static final BigDecimal CINQ_MILLE = new BigDecimal("5000");
  private static final LocalDate MERCREDI = LocalDate.of(2026, 11, 11);

  private static void invalide(ThrowingCallable appel) {
    assertThatThrownBy(appel)
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.COTISATIONS_PAR_TOUR_INVALIDE));
  }

  private static PeriodeTour periode(LocalDate debut, LocalDate fin) {
    return new PeriodeTour(debut, fin, fin.atTime(20, 0));
  }

  @Test
  @DisplayName("valeur saisie prioritaire ; défaut 1 ; jackpot visé 500 000 F → 10 cotisations")
  void cotisationsParTour() throws CustomException {
    assertThat(RythmeJackpot.cotisationsParTour(TontineType.ROTATIVE, 4, null, CINQ_MILLE, 10, 1))
        .isEqualTo(4);
    assertThat(
            RythmeJackpot.cotisationsParTour(TontineType.ROTATIVE, null, null, CINQ_MILLE, 10, 1))
        .isEqualTo(1);
    assertThat(
            RythmeJackpot.cotisationsParTour(
                TontineType.ROTATIVE, null, new BigDecimal("500000"), CINQ_MILLE, 10, 1))
        .isEqualTo(10);
    // la valeur saisie prime sur le jackpot visé
    assertThat(
            RythmeJackpot.cotisationsParTour(
                TontineType.ROTATIVE, 3, new BigDecimal("500000"), CINQ_MILLE, 10, 1))
        .isEqualTo(3);
    // événementielle : jackpot visé ignoré, 1 seulement
    assertThat(
            RythmeJackpot.cotisationsParTour(
                TontineType.EVENEMENTIELLE, null, new BigDecimal("500000"), CINQ_MILLE, 10, 1))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("refus : moins de 1, plusieurs hors rotative, jackpot visé sans montant ni membres")
  void refus() {
    invalide(() -> RythmeJackpot.cotisationsParTour(TontineType.ROTATIVE, 0, null, null, 0, 1));
    invalide(
        () -> RythmeJackpot.cotisationsParTour(TontineType.EVENEMENTIELLE, 2, null, null, 0, 1));
    invalide(
        () ->
            RythmeJackpot.cotisationsParTour(
                TontineType.ROTATIVE, null, new BigDecimal("500000"), null, 10, 1));
    invalide(
        () ->
            RythmeJackpot.cotisationsParTour(
                TontineType.ROTATIVE, null, new BigDecimal("500000"), CINQ_MILLE, 0, 1));
  }

  @Test
  @DisplayName("jackpot visé inexact : N le plus proche ; gagnants ; jackpot prévu en francs")
  void deductionEtJackpot() throws CustomException {
    assertThat(RythmeJackpot.deduire(new BigDecimal("520000"), CINQ_MILLE, 10, 1)).isEqualTo(10);
    assertThat(RythmeJackpot.deduire(new BigDecimal("530000"), CINQ_MILLE, 10, 1)).isEqualTo(11);
    assertThat(RythmeJackpot.deduire(new BigDecimal("1000"), CINQ_MILLE, 10, 1)).isEqualTo(1);
    // 2 gagnants : 25 000 F par gagnant et par échéance
    assertThat(RythmeJackpot.deduire(new BigDecimal("100000"), CINQ_MILLE, 10, 2)).isEqualTo(4);
    assertThat(RythmeJackpot.jackpotParTour(CINQ_MILLE, 10, 1, 4)).isEqualByComparingTo("200000");
    assertThat(RythmeJackpot.jackpotParTour(CINQ_MILLE, 10, 3, 1)).isEqualByComparingTo("16667");
    assertThat(RythmeJackpot.jackpotParTour(null, 10, 1, 4)).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("chaque semaine le dimanche, 4 par tour → tours de 4 dimanches consécutifs")
  void toursDeQuatreEcheances() throws CustomException {
    Rythme dimanche = Rythme.lire(1, "SEMAINE", "DIMANCHE", "20:00");
    List<List<PeriodeTour>> tours =
        CalendrierTours.grouper(CalendrierTours.generer(dimanche, MERCREDI, 8, null), 4);
    assertThat(tours).hasSize(2);
    assertThat(tours.get(0)).hasSize(4);
    assertThat(tours.get(0).get(3).fin()).isEqualTo(LocalDate.of(2026, 12, 6));
    assertThat(tours.get(1).get(0).debut()).isEqualTo(LocalDate.of(2026, 12, 7));

    CycleTontine cycle =
        CalendrierTours.tour(
            Fixtures.tontine(Fixtures.user("G")), 1, tours.get(0), CycleStatut.EN_ATTENTE);
    assertThat(cycle.getDateDebut()).isEqualTo(MERCREDI);
    assertThat(cycle.getDateFin()).isEqualTo(LocalDate.of(2026, 12, 6));
    assertThat(cycle.getEcheanceAt()).isEqualTo(LocalDate.of(2026, 12, 6).atTime(20, 0));
    assertThat(cycle.getEcheances())
        .extracting(EcheanceCycle::getNumero)
        .containsExactly(1, 2, 3, 4);
    assertThat(cycle.getEcheances().get(1).getDateDebut()).isEqualTo(LocalDate.of(2026, 11, 16));

    // dernier groupe incomplet, taille 0 traitée comme 1
    assertThat(CalendrierTours.grouper(tours.get(0), 3))
        .extracting(List::size)
        .containsExactly(3, 1);
    assertThat(CalendrierTours.grouper(tours.get(0), 0)).hasSize(4);
  }

  @Test
  @DisplayName(
      "tour manuel : N échéances sans date de fin ; date plus courte → moins, dernière ramenée")
  void tourManuel() throws CustomException {
    Rythme dimanche = Rythme.lire(1, "SEMAINE", "DIMANCHE", null);
    assertThat(CalendrierTours.echeancesDuTour(dimanche, 4, MERCREDI, null, null)).hasSize(4);
    // reliquat de 4 jours sur 7 (≥ moitié) : échéance raccourcie conservée
    List<PeriodeTour> court =
        CalendrierTours.echeancesDuTour(dimanche, 4, MERCREDI, LocalDate.of(2026, 11, 26), null);
    assertThat(court)
        .extracting(PeriodeTour::fin)
        .containsExactly(
            LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 26));
    // date de fin le jour même : une seule échéance
    assertThat(CalendrierTours.echeancesDuTour(dimanche, 4, MERCREDI, MERCREDI, null)).hasSize(1);
  }

  @Test
  @DisplayName(
      "tour manuel : reliquat de moins d'une demi-période fusionné dans l'échéance d'avant")
  void tourManuelReliquatCourt() throws CustomException {
    Rythme dimanche = Rythme.lire(1, "SEMAINE", "DIMANCHE", "20:00");
    // 23 → 25/11 : 3 jours sur 7 → l'échéance du 22 est prolongée jusqu'au 25
    List<PeriodeTour> hebdo =
        CalendrierTours.echeancesDuTour(dimanche, 4, MERCREDI, LocalDate.of(2026, 11, 25), null);
    assertThat(hebdo)
        .extracting(PeriodeTour::fin)
        .containsExactly(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 25));
    assertThat(hebdo.get(1).debut()).isEqualTo(LocalDate.of(2026, 11, 16));
    assertThat(hebdo.get(1).echeanceAt()).isEqualTo(LocalDate.of(2026, 11, 25).atTime(20, 0));

    // mensuel du 14/11 au 14/12 : une seule échéance le 14/12 (pas d'échéance d'un jour)
    Rythme mensuel = Rythme.lire(1, "MOIS", null, null);
    LocalDate debut = LocalDate.of(2026, 11, 14);
    List<PeriodeTour> mois =
        CalendrierTours.echeancesDuTour(mensuel, 1, debut, LocalDate.of(2026, 12, 14), null);
    assertThat(mois).hasSize(1);
    assertThat(mois.get(0).debut()).isEqualTo(debut);
    assertThat(mois.get(0).fin()).isEqualTo(LocalDate.of(2026, 12, 14));
  }

  @Test
  @DisplayName("déplacement d'un tour : échéances recalées, en trop retirées, manquantes ajoutées")
  void deplacement() {
    LocalDate j = LocalDate.of(2026, 11, 1);
    CycleTontine cycle =
        CalendrierTours.tour(
            Fixtures.tontine(Fixtures.user("G")),
            1,
            List.of(periode(j, j.plusDays(6)), periode(j.plusDays(7), j.plusDays(13))),
            CycleStatut.EN_ATTENTE);

    CalendrierTours.deplacer(
        cycle,
        List.of(
            periode(j, j.plusDays(2)),
            periode(j.plusDays(3), j.plusDays(5)),
            periode(j.plusDays(6), j.plusDays(8))));
    assertThat(cycle.getEcheances()).hasSize(3);
    assertThat(cycle.getEcheances().get(2).getNumero()).isEqualTo(3);
    assertThat(cycle.getDateFin()).isEqualTo(j.plusDays(8));

    CalendrierTours.deplacer(cycle, List.of(periode(j, j.plusDays(4))));
    assertThat(cycle.getEcheances().stream().filter(e -> e.getDeletedAt() == null)).hasSize(1);
    assertThat(cycle.getEcheances().get(0).getEcheanceAt()).isEqualTo(j.plusDays(4).atTime(20, 0));
  }

  @Test
  @DisplayName("statut d'une échéance : à venir, en cours (minute limite incluse), échue")
  void statut() {
    EcheanceCycle e =
        EcheanceCycle.builder()
            .dateDebut(LocalDate.of(2026, 11, 16))
            .echeanceAt(LocalDate.of(2026, 11, 22).atTime(20, 0))
            .build();
    assertThat(StatutEcheance.de(e, LocalDate.of(2026, 11, 15).atTime(23, 0)))
        .isEqualTo(StatutEcheance.A_VENIR);
    assertThat(StatutEcheance.de(e, LocalDate.of(2026, 11, 16).atStartOfDay()))
        .isEqualTo(StatutEcheance.EN_COURS);
    assertThat(StatutEcheance.de(e, LocalDate.of(2026, 11, 22).atTime(20, 0, 59)))
        .isEqualTo(StatutEcheance.EN_COURS);
    assertThat(StatutEcheance.de(e, LocalDate.of(2026, 11, 22).atTime(20, 1)))
        .isEqualTo(StatutEcheance.ECHUE);
  }
}
