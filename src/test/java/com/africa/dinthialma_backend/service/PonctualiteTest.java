package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.codeList.Ponctualite;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Ponctualité d'une cotisation (S01)")
class PonctualiteTest {

  private CycleTontine cycle;
  private TontineMembre membre;

  @BeforeEach
  void setUp() {
    User gestionnaire = Fixtures.user("Gestionnaire");
    Tontine tontine = Fixtures.tontine(gestionnaire);
    membre = Fixtures.membre(tontine, Fixtures.user("Awa"), 1);
    cycle = Fixtures.cycle(tontine, 1, CycleStatut.TERMINE);
    cycle.setDateFin(LocalDate.of(2026, 11, 22));
  }

  private Cotisation declaree(CotisationStatut statut, int jour, int heure, int minute) {
    Cotisation c = Fixtures.cotisation(membre, cycle, statut);
    c.setCreatedAt(LocalDate.of(2026, 11, jour).atTime(heure, minute));
    return c;
  }

  @Test
  @DisplayName("déclarée le jour d'échéance à 23h59 -> A_TEMPS, même validée ou EN_RETARD après")
  void aTemps() {
    assertThat(Ponctualite.de(declaree(CotisationStatut.EN_ATTENTE, 22, 23, 59)))
        .isEqualTo(Ponctualite.A_TEMPS);
    // statut EN_RETARD = non validée à la clôture ; la déclaration était pourtant à temps
    assertThat(Ponctualite.de(declaree(CotisationStatut.EN_RETARD, 20, 10, 0)))
        .isEqualTo(Ponctualite.A_TEMPS);
  }

  @Test
  @DisplayName("déclarée le lendemain à 00h00 -> EN_RETARD")
  void enRetard() {
    assertThat(Ponctualite.de(declaree(CotisationStatut.VALIDE, 23, 0, 0)))
        .isEqualTo(Ponctualite.EN_RETARD);
  }

  @Test
  @DisplayName("échéance à 20h00 (B26) : 20h00:59 -> A_TEMPS ; 20h01 -> EN_RETARD")
  void heureEcheance() {
    cycle.setEcheanceAt(LocalDate.of(2026, 11, 22).atTime(20, 0));
    Cotisation limite = declaree(CotisationStatut.VALIDE, 22, 20, 0);
    limite.setCreatedAt(limite.getCreatedAt().plusSeconds(59));
    assertThat(Ponctualite.de(limite)).isEqualTo(Ponctualite.A_TEMPS);
    assertThat(Ponctualite.de(declaree(CotisationStatut.VALIDE, 22, 20, 1)))
        .isEqualTo(Ponctualite.EN_RETARD);
  }

  @Test
  @DisplayName("datePaiement (main propre) prime sur la date de saisie")
  void datePaiement() {
    Cotisation saisieTard = declaree(CotisationStatut.VALIDE, 25, 9, 0);
    saisieTard.setDatePaiement(LocalDate.of(2026, 11, 21).atTime(18, 0));
    assertThat(Ponctualite.de(saisieTard)).isEqualTo(Ponctualite.A_TEMPS);
  }

  @Test
  @DisplayName(
      "aucune cotisation ou seulement refusée -> NON_DECLAREE ; dates inconnues -> A_TEMPS")
  void casParticuliers() {
    assertThat(Ponctualite.de(null)).isEqualTo(Ponctualite.NON_DECLAREE);
    assertThat(Ponctualite.de(declaree(CotisationStatut.REFUSE, 20, 10, 0)))
        .isEqualTo(Ponctualite.NON_DECLAREE);

    Cotisation sansDate = declaree(CotisationStatut.EN_ATTENTE, 20, 10, 0);
    sansDate.setCreatedAt(null);
    assertThat(Ponctualite.de(sansDate)).isEqualTo(Ponctualite.A_TEMPS);
    Cotisation sansTour = declaree(CotisationStatut.EN_ATTENTE, 20, 10, 0);
    sansTour.setCycle(null);
    assertThat(Ponctualite.de(sansTour)).isEqualTo(Ponctualite.A_TEMPS);
  }
}
