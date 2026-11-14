package com.africa.dinthialma_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.africa.dinthialma_backend.support.AbstractIntegrationTest;
import com.africa.dinthialma_backend.support.ApiClient.Reponse;
import com.africa.dinthialma_backend.support.TestDataFactory;
import com.africa.dinthialma_backend.support.TestDataFactory.Utilisateur;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Cotisations par échéance (B32, lot 2) : un tour de 3 échéances quotidiennes reculé d'un jour
 * (échéance 1 échue, 2 en cours, 3 à venir) ; déclaration, régularisation, récap, filtre aPaye et
 * résumé.
 */
@DisplayName("Cotisations par échéance")
class CotisationParEcheanceIT extends AbstractIntegrationTest {

  private static final int MONTANT = 1000;

  @Autowired private JdbcTemplate jdbc;

  private Map<String, Object> declaration(String cycleId, String echeanceId) {
    Map<String, Object> corps = new HashMap<>();
    corps.put("cycleId", cycleId);
    corps.put("montant", MONTANT);
    corps.put("methodePaiement", "WAVE");
    if (echeanceId != null) {
      corps.put("echeanceId", echeanceId);
    }
    return corps;
  }

  private static List<String> membreIds(JsonNode page) {
    return StreamSupport.stream(page.path("content").spliterator(), false)
        .map(x -> x.path("id").asText())
        .toList();
  }

  @Test
  @DisplayName(
      "échéance en cours par défaut, 409 sur la même échéance, future refusée, passée"
          + " régularisable (EN_RETARD), récap, aPaye et résumé par échéance")
  void parcours() {
    Utilisateur gestionnaire = donnees.inscrire("Gestionnaire");
    Utilisateur membre = donnees.inscrire("Membre");
    String g = gestionnaire.jeton();
    String tontineId =
        donnees
            .creerTontine(
                g,
                "Marché",
                Map.of("rythmeIntervalle", 1, "rythmeUnite", "JOUR", "cotisationsParTour", 3))
            .path("id")
            .asText();
    String membreId = donnees.ajouterMembre(g, tontineId, membre.phone()).path("id").asText();
    donnees.activer(g, tontineId);
    JsonNode tour =
        TestDataFactory.attendre(
                api.post(
                    "/v1/tontines/" + tontineId + "/cycles",
                    g,
                    Map.of("dateDebut", LocalDate.now().toString())),
                201)
            .data();
    assertThat(tour.path("nombreEcheances").asInt()).isEqualTo(3);
    String cycleId = tour.path("id").asText();
    // ouverture dans le passé refusée par l'API : on recule le tour d'un jour en base
    jdbc.update(
        "UPDATE dinthialma.echeances_cycle SET date_debut = date_debut - 1,"
            + " echeance_at = echeance_at - INTERVAL '1 day' WHERE cycle_id = ?::uuid",
        cycleId);
    jdbc.update(
        "UPDATE dinthialma.cycles_tontine SET date_debut = date_debut - 1, date_fin = date_fin - 1,"
            + " echeance_at = echeance_at - INTERVAL '1 day' WHERE id = ?::uuid",
        cycleId);
    String e1 = tour.path("echeances").get(0).path("id").asText();
    String e2 = tour.path("echeances").get(1).path("id").asText();
    String e3 = tour.path("echeances").get(2).path("id").asText();
    String cotisations = "/v1/tontines/" + tontineId + "/cotisations";

    // sans echeanceId : échéance en cours (2)
    JsonNode courante =
        TestDataFactory.attendre(
                api.post(cotisations, membre.jeton(), declaration(cycleId, null)), 201)
            .data();
    assertThat(courante.path("echeanceId").asText()).isEqualTo(e2);
    assertThat(courante.path("numeroEcheance").asInt()).isEqualTo(2);
    assertThat(courante.path("ponctualite").asText()).isEqualTo("A_TEMPS");

    Reponse doublon = api.post(cotisations, membre.jeton(), declaration(cycleId, e2));
    assertThat(doublon.statut()).isEqualTo(409);
    assertThat(doublon.code()).isEqualTo("COTISATION_ALREADY_EXISTS");

    Reponse future = api.post(cotisations, membre.jeton(), declaration(cycleId, e3));
    assertThat(future.statut()).isEqualTo(400);
    assertThat(future.code()).isEqualTo("ECHEANCE_NON_OUVERTE");

    Reponse inconnue =
        api.post(cotisations, membre.jeton(), declaration(cycleId, UUID.randomUUID().toString()));
    assertThat(inconnue.statut()).isEqualTo(404);
    assertThat(inconnue.code()).isEqualTo("ECHEANCE_INTROUVABLE");

    // régularisation de l'échéance 1 (échue hier) : acceptée, en retard
    JsonNode regularisee =
        TestDataFactory.attendre(
                api.post(cotisations, membre.jeton(), declaration(cycleId, e1)), 201)
            .data();
    assertThat(regularisee.path("numeroEcheance").asInt()).isEqualTo(1);
    assertThat(regularisee.path("ponctualite").asText()).isEqualTo("EN_RETARD");

    // récap du tour : échéance en cours par défaut, 2 dues, aucune validée
    JsonNode ligne = api.get(cotisations + "/recap/" + cycleId, g).data().path("content").get(0);
    assertThat(ligne.path("numeroEcheance").asInt()).isEqualTo(2);
    assertThat(ligne.path("statutCotisation").asText()).isEqualTo("EN_ATTENTE");
    assertThat(ligne.path("nombreEcheances").asInt()).isEqualTo(3);
    assertThat(ligne.path("echeancesDues").asInt()).isEqualTo(2);
    assertThat(ligne.path("echeancesPayees").asInt()).isZero();
    assertThat(ligne.path("echeances").get(2).path("ouverte").asBoolean()).isFalse();
    JsonNode filtre =
        api.get(cotisations + "/recap/" + cycleId + "?echeanceId=" + e1, g)
            .data()
            .path("content")
            .get(0);
    assertThat(filtre.path("cotisationId").asText()).isEqualTo(regularisee.path("id").asText());

    // membres : filtre aPaye par échéance
    String membres = "/v1/tontines/" + tontineId + "/membres?aPaye=";
    assertThat(membreIds(api.get(membres + "true&echeanceId=" + e2, g).data()))
        .containsExactly(membreId);
    assertThat(membreIds(api.get(membres + "false&echeanceId=" + e3, g).data()))
        .containsExactly(membreId);
    assertThat(api.get(membres + "true&echeanceId=" + UUID.randomUUID(), g).code())
        .isEqualTo("ECHEANCE_INTROUVABLE");

    // résumé « Je cotise » : due sur l'échéance 2, rien à régulariser
    JsonNode due =
        api.get("/v1/me/summary", membre.jeton())
            .data()
            .path("tontines")
            .get(0)
            .path("cotisationDue");
    assertThat(due.path("numeroEcheance").asInt()).isEqualTo(2);
    assertThat(due.path("nombreEcheances").asInt()).isEqualTo(3);
    assertThat(due.path("statut").asText()).isEqualTo("EN_ATTENTE");
    assertThat(due.path("echeancesEnRetard").asInt()).isZero();
  }
}
