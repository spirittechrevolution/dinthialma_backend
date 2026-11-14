package com.africa.dinthialma_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.africa.dinthialma_backend.common.util.Fuseau;
import com.africa.dinthialma_backend.notification.service.RappelsEcheanceService;
import com.africa.dinthialma_backend.notification.service.SchedulerService;
import com.africa.dinthialma_backend.support.AbstractIntegrationTest;
import com.africa.dinthialma_backend.support.TestDataFactory.TontineEnCours;
import com.africa.dinthialma_backend.support.TestDataFactory.Utilisateur;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Rappels d'échéance et cotisations manquantes (B26, lot 3) sur la vraie base. */
@DisplayName("Rappels d'échéance")
class RappelsEcheanceIT extends AbstractIntegrationTest {

  @Autowired private RappelsEcheanceService rappels;
  @Autowired private SchedulerService planificateur;

  private List<String> types(Utilisateur u) {
    return api.get("/v1/notifications?size=100", u.jeton())
        .data()
        .path("content")
        .findValuesAsText("type");
  }

  private LocalDateTime echeance(TontineEnCours t) {
    JsonNode cycle =
        api.get(
                "/v1/tontines/" + t.tontineId() + "/cycles/" + t.cycleId(),
                t.gestionnaire().jeton())
            .data();
    return OffsetDateTime.parse(cycle.path("echeanceAt").asText())
        .atZoneSameInstant(Fuseau.DAKAR)
        .toLocalDateTime();
  }

  @Test
  @DisplayName("rappel envoyé une seule fois, puis cotisations manquantes au gestionnaire")
  void rappelUniqueEtManquantes() {
    TontineEnCours t = donnees.tontineEnCours("Rappels");
    LocalDateTime limite = echeance(t);

    rappels.traiter(limite.minusHours(1));
    rappels.traiter(limite.minusMinutes(30)); // second passage : l'index unique bloque le doublon
    assertThat(types(t.membre())).filteredOn("RAPPEL_COTISATION"::equals).hasSize(1);

    rappels.traiter(limite.plusMinutes(10));
    rappels.traiter(limite.plusHours(2));
    assertThat(types(t.gestionnaire())).filteredOn("COTISATIONS_MANQUANTES"::equals).hasSize(1);
  }

  @Test
  @DisplayName("membre ayant déclaré : aucun rappel ni signalement")
  void declarantEpargne() {
    TontineEnCours t = donnees.tontineEnCours("Déclarée");
    donnees.declarer(t.membre().jeton(), t.tontineId(), t.cycleId(), 1000);
    LocalDateTime limite = echeance(t);

    rappels.traiter(limite.minusHours(1));
    rappels.traiter(limite.plusHours(1));
    assertThat(types(t.membre())).doesNotContain("RAPPEL_COTISATION");
    assertThat(types(t.gestionnaire())).doesNotContain("COTISATIONS_MANQUANTES");
  }

  @Test
  @DisplayName("rappel quotidien de 8h (planificateur existant) : notification in-app enregistrée")
  void rappelQuotidienEnregistre() {
    TontineEnCours t = donnees.tontineEnCours("Quotidien");
    donnees.declarer(t.membre().jeton(), t.tontineId(), t.cycleId(), 1000);

    planificateur.rappelCotisationsEnAttente();

    assertThat(types(t.membre())).contains("RAPPEL_COTISATION");
  }
}
