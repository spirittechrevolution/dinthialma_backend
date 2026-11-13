package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.dto.SimulationCalendrierRequest;
import com.africa.dinthialma_backend.tontine.dto.SimulationCalendrierResponse;
import com.africa.dinthialma_backend.tontine.service.impl.CalendrierServiceImpl;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CalendrierServiceImpl – simulation (B26)")
class CalendrierServiceImplTest {

  private final CalendrierServiceImpl service = new CalendrierServiceImpl();

  private static SimulationCalendrierRequest requete() {
    SimulationCalendrierRequest r = new SimulationCalendrierRequest();
    r.setRythmeIntervalle(3);
    r.setRythmeUnite("JOUR");
    r.setDateDebut(LocalDate.of(2026, 11, 12));
    r.setNombreMembres(10);
    return r;
  }

  private static OffsetDateTime utc(int annee, int mois, int jour, int heure, int minute) {
    return OffsetDateTime.of(annee, mois, jour, heure, minute, 0, 0, ZoneOffset.UTC);
  }

  @Test
  @DisplayName("rotative : 10 membres, tous les 3 jours -> 10 tours du 14/11 au 11/12, en UTC")
  void rotative() throws Exception {
    SimulationCalendrierResponse r = service.simuler(requete());
    assertThat(r.getNombreTours()).isEqualTo(10);
    assertThat(r.getPremiereEcheance()).isEqualTo(utc(2026, 11, 14, 23, 59));
    assertThat(r.getDerniereEcheance()).isEqualTo(utc(2026, 12, 11, 23, 59));
    assertThat(r.getEcheances()).hasSize(10);
    assertThat(r.getRythme()).isEqualTo("tous les 3 jours, avant 23h59");
  }

  @Test
  @DisplayName("rotative : 2 gagnants par tour -> moitié moins de tours ; fréquence acceptée")
  void gagnantsEtFrequence() throws Exception {
    SimulationCalendrierRequest r = requete();
    r.setNombreGagnants(2);
    assertThat(service.simuler(r).getNombreTours()).isEqualTo(5);

    r.setRythmeIntervalle(null);
    r.setRythmeUnite(null);
    r.setFrequence("MENSUEL");
    assertThat(service.simuler(r).getPremiereEcheance()).isEqualTo(utc(2026, 12, 11, 23, 59));
  }

  @Test
  @DisplayName(
      "événementielle : s'arrête sur la date de l'événement, 400 échéances listées au plus")
  void evenementielle() throws Exception {
    SimulationCalendrierRequest r = requete();
    r.setTontineType(TontineType.EVENEMENTIELLE);
    r.setNombreMembres(null);
    r.setRythmeIntervalle(1);
    r.setDateEcheance(LocalDate.of(2028, 1, 1));
    SimulationCalendrierResponse s = service.simuler(r);
    assertThat(s.getNombreTours()).isGreaterThan(400);
    assertThat(s.getEcheances()).hasSize(400);
    assertThat(s.getDerniereEcheance()).isEqualTo(utc(2028, 1, 1, 23, 59));
  }

  @Test
  @DisplayName("refus : rythme invalide, membres manquants, date d'événement absente ou passée")
  void refus() {
    SimulationCalendrierRequest invalide = requete();
    invalide.setRythmeIntervalle(500);
    assertThatThrownBy(() -> service.simuler(invalide))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.RYTHME_INVALIDE));

    SimulationCalendrierRequest sansMembres = requete();
    sansMembres.setNombreMembres(null);
    assertThatThrownBy(() -> service.simuler(sansMembres)).isInstanceOf(BadRequestException.class);

    SimulationCalendrierRequest evenement = requete();
    evenement.setTontineType(TontineType.EVENEMENTIELLE);
    assertThatThrownBy(() -> service.simuler(evenement)).isInstanceOf(BadRequestException.class);
    evenement.setDateEcheance(evenement.getDateDebut());
    assertThatThrownBy(() -> service.simuler(evenement)).isInstanceOf(BadRequestException.class);
  }
}
