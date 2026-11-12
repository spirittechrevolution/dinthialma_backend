package com.africa.dinthialma_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.africa.dinthialma_backend.support.AbstractIntegrationTest;
import com.africa.dinthialma_backend.support.ApiClient.Reponse;
import com.africa.dinthialma_backend.support.TestDataFactory;
import com.africa.dinthialma_backend.support.TestDataFactory.TontineEnCours;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cycle de vie d'un paiement : déclaration, refus et nouvelle déclaration (B18), validation groupée
 * (B14), filtres (B07), résumé cotisant (B04), collecte du tour (B05), file « À valider » (B06),
 * fil d'activité (B09), liens des notifications (B19) et export CSV (B24).
 */
@DisplayName("Parcours d'une cotisation")
class CotisationParcoursIT extends AbstractIntegrationTest {

  private static final int MONTANT = 1000;

  private String cotisations(TontineEnCours t) {
    return "/v1/tontines/" + t.tontineId() + "/cotisations";
  }

  private JsonNode ligneSummary(TontineEnCours t) {
    JsonNode tontines = api.get("/v1/me/summary", t.membre().jeton()).data().path("tontines");
    return StreamSupport.stream(tontines.spliterator(), false)
        .filter(x -> x.path("tontineId").asText().equals(t.tontineId()))
        .findFirst()
        .orElseThrow();
  }

  private static List<String> ids(JsonNode page) {
    return StreamSupport.stream(page.path("content").spliterator(), false)
        .map(x -> x.path("id").asText())
        .toList();
  }

  @Test
  @DisplayName(
      "ponctualité (S01) : NON_DECLAREE avant paiement ; encaissement en main propre daté ->"
          + " A_TEMPS, datePaiement renvoyée")
  void ponctualite() {
    TontineEnCours t = donnees.tontineEnCours("Ponctualité");
    String g = t.gestionnaire().jeton();
    JsonNode avant =
        api.get(cotisations(t) + "/recap/" + t.cycleId(), g).data().path("content").get(0);
    assertThat(avant.path("ponctualite").asText()).isEqualTo("NON_DECLAREE");
    assertThat(ligneSummary(t).path("cotisationDue").path("ponctualite").asText())
        .isEqualTo("NON_DECLAREE");

    String hier = LocalDate.now().atTime(7, 30).toString();
    JsonNode encaissee =
        TestDataFactory.attendre(
                api.post(
                    cotisations(t) + "/admin",
                    g,
                    Map.of(
                        "membreId",
                        t.membreId(),
                        "cycleId",
                        t.cycleId(),
                        "montant",
                        MONTANT,
                        "methodePaiement",
                        "CASH",
                        "datePaiement",
                        hier)),
                201)
            .data();
    assertThat(encaissee.path("ponctualite").asText()).isEqualTo("A_TEMPS");
    assertThat(encaissee.path("datePaiement").asText()).startsWith(hier.substring(0, 16));
    assertThat(ligneSummary(t).path("cotisationDue").path("ponctualite").asText())
        .isEqualTo("A_TEMPS");
  }

  @Test
  @DisplayName("ouvrir un cycle MANUEL renvoie le cycle EN_COURS (régression du 500)")
  void ouvertureCycleManuel() {
    TontineEnCours t = donnees.tontineEnCours("Cycle manuel");
    JsonNode cycle =
        api.get(
                "/v1/tontines/" + t.tontineId() + "/cycles/" + t.cycleId(),
                t.gestionnaire().jeton())
            .data();
    assertThat(cycle.path("statut").asText()).isEqualTo("EN_COURS");
    assertThat(cycle.path("numeroCycle").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("déclarer, refuser avec motif, redéclarer, valider (B18)")
  void refusPuisRedeclaration() {
    TontineEnCours t = donnees.tontineEnCours("Refus");
    String g = t.gestionnaire().jeton();

    JsonNode c1 = donnees.declarer(t.membre().jeton(), t.tontineId(), t.cycleId(), MONTANT);
    assertThat(c1.path("statut").asText()).isEqualTo("EN_ATTENTE");
    assertThat(ligneSummary(t).path("cotisationDue").path("statut").asText())
        .isEqualTo("EN_ATTENTE");

    Reponse refus =
        api.put(
            cotisations(t) + "/" + c1.path("id").asText() + "/refuser",
            g,
            Map.of("motif", "  Transfert non reçu  "));
    assertThat(refus.statut()).isEqualTo(200);
    assertThat(refus.data().path("statut").asText()).isEqualTo("REFUSE");
    assertThat(refus.data().path("motifRefus").asText()).isEqualTo("Transfert non reçu");
    assertThat(refus.data().path("refusePar").path("id").asText()).isNotBlank();

    // Un refus est final
    String c1Id = c1.path("id").asText();
    assertThat(api.put(cotisations(t) + "/" + c1Id + "/refuser", g, Map.of("motif", "x")).code())
        .isEqualTo("COTISATION_NOT_PENDING");
    assertThat(api.put(cotisations(t) + "/" + c1Id + "/valider", g, null).code())
        .isEqualTo("COTISATION_NOT_PENDING");
    assertThat(api.patch(cotisations(t) + "/" + c1Id, g, Map.of("note", "x")).code())
        .isEqualTo("COTISATION_NOT_EDITABLE");

    // Côté membre : à payer à nouveau, avec le motif ; le refus n'entre dans aucun total
    JsonNode ligne = ligneSummary(t);
    assertThat(ligne.path("cotisationDue").path("statut").asText()).isEqualTo("A_PAYER");
    assertThat(ligne.path("cotisationDue").path("motifDernierRefus").asText())
        .isEqualTo("Transfert non reçu");
    assertThat(ligne.path("totalCotiseValide").asDouble()).isZero();
    assertThat(ligne.path("totalEnAttente").asDouble()).isZero();

    // Le refus ne bloque pas une nouvelle déclaration ; une 2e déclaration active est refusée
    JsonNode c2 = donnees.declarer(t.membre().jeton(), t.tontineId(), t.cycleId(), MONTANT);
    assertThat(c2.path("id").asText()).isNotEqualTo(c1Id);
    Reponse doublon =
        api.post(
            cotisations(t), t.membre().jeton(), Map.of("cycleId", t.cycleId(), "montant", MONTANT));
    assertThat(doublon.statut()).isEqualTo(409);
    assertThat(doublon.code()).isEqualTo("COTISATION_ALREADY_EXISTS");

    // Le récapitulatif du tour montre la déclaration active, pas le refus
    JsonNode recap = api.get(cotisations(t) + "/recap/" + t.cycleId(), g).data().path("content");
    JsonNode ligneRecap =
        StreamSupport.stream(recap.spliterator(), false)
            .filter(x -> x.path("membreId").asText().equals(t.membreId()))
            .findFirst()
            .orElseThrow();
    assertThat(ligneRecap.path("cotisationId").asText()).isEqualTo(c2.path("id").asText());
    assertThat(ligneRecap.path("statutCotisation").asText()).isEqualTo("EN_ATTENTE");
    assertThat(ligneRecap.path("ponctualite").asText()).isEqualTo("A_TEMPS");

    // Filtres (B07)
    assertThat(ids(api.get(cotisations(t) + "?statut=REFUSE", g).data())).containsExactly(c1Id);
    assertThat(
            ids(
                api.get(
                        "/v1/me/gestion/cotisations?statut=EN_ATTENTE&tontineId=" + t.tontineId(),
                        g)
                    .data()))
        .containsExactly(c2.path("id").asText());
  }

  @Test
  @DisplayName(
      "validation groupée : succès et échecs indépendants (B14) ; résumé et collecte (B04, B05)")
  void validationGroupeeEtTableauxDeBord() {
    TontineEnCours t = donnees.tontineEnCours("Validation groupée");
    String g = t.gestionnaire().jeton();
    JsonNode c = donnees.declarer(t.membre().jeton(), t.tontineId(), t.cycleId(), MONTANT);
    String inconnu = UUID.randomUUID().toString();

    Reponse lot =
        api.post(
            "/v1/me/gestion/cotisations/valider",
            g,
            Map.of(
                "cotisationIds", List.of(c.path("id").asText(), inconnu, c.path("id").asText())));
    assertThat(lot.statut()).isEqualTo(200);
    JsonNode d = lot.data();
    assertThat(d.path("total").asInt()).isEqualTo(2); // doublon ignoré
    assertThat(d.path("validees").asInt()).isEqualTo(1);
    assertThat(d.path("resultats").get(0).path("statut").asText()).isEqualTo("VALIDE");
    assertThat(d.path("resultats").get(1).path("code").asText()).isEqualTo("NOT_FOUND");

    // Résumé cotisant (B04)
    JsonNode ligne = ligneSummary(t);
    assertThat(ligne.path("cotisationDue").path("statut").asText()).isEqualTo("VALIDE");
    assertThat(ligne.path("totalCotiseValide").asDouble()).isEqualTo(MONTANT);
    assertThat(ligne.path("toursPayes").asInt()).isEqualTo(1);
    assertThat(ligne.path("monMembreId").asText()).isEqualTo(t.membreId());

    // Collecte du tour côté gestionnaire (B05)
    JsonNode stats =
        StreamSupport.stream(
                api.get("/v1/admin/my-dashboard?size=50", g)
                    .data()
                    .path("tontines")
                    .path("content")
                    .spliterator(),
                false)
            .filter(x -> x.path("tontineId").asText().equals(t.tontineId()))
            .findFirst()
            .orElseThrow();
    JsonNode tour = stats.path("cycleEnCours");
    assertThat(tour.path("montantCollecte").asDouble()).isEqualTo(MONTANT);
    assertThat(tour.path("membresAyantPaye").asInt()).isEqualTo(1);
    assertThat(tour.path("membresActifs").asInt()).isEqualTo(1);
    assertThat(tour.path("montantAttendu").asDouble()).isEqualTo(MONTANT);

    // Validation groupée : liste vide refusée
    assertThat(
            api.post("/v1/me/gestion/cotisations/valider", g, Map.of("cotisationIds", List.of()))
                .code())
        .isEqualTo("VALIDATION_ERROR");
  }

  @Test
  @DisplayName("un cycleId d'une autre tontine ne fuit pas (B07)")
  void etancheiteEntreTontines() {
    TontineEnCours a = donnees.tontineEnCours("Tontine A");
    TontineEnCours b = donnees.tontineEnCours("Tontine B");
    donnees.declarer(b.membre().jeton(), b.tontineId(), b.cycleId(), MONTANT);

    // Le gestionnaire de A demande le cycle de B via sa propre tontine
    Reponse r = api.get(cotisations(a) + "?cycleId=" + b.cycleId(), a.gestionnaire().jeton());
    assertThat(r.statut()).isEqualTo(200);
    assertThat(r.data().path("page").path("totalElements").asLong()).isZero();
  }

  @Test
  @DisplayName("notifications avec liens précis (B19) et fil d'activité (B09)")
  void notificationsEtActivite() {
    TontineEnCours t = donnees.tontineEnCours("Liens");
    JsonNode c = donnees.declarer(t.membre().jeton(), t.tontineId(), t.cycleId(), MONTANT);
    api.put(
        cotisations(t) + "/" + c.path("id").asText() + "/refuser",
        t.gestionnaire().jeton(),
        Map.of("motif", "Montant incorrect"));

    JsonNode refusee =
        StreamSupport.stream(
                api.get("/v1/notifications?size=20", t.membre().jeton())
                    .data()
                    .path("content")
                    .spliterator(),
                false)
            .filter(n -> n.path("type").asText().equals("COTISATION_REFUSEE"))
            .findFirst()
            .orElseThrow();
    assertThat(refusee.path("cotisationId").asText()).isEqualTo(c.path("id").asText());
    assertThat(refusee.path("cycleId").asText()).isEqualTo(t.cycleId());
    assertThat(refusee.path("membreId").asText()).isEqualTo(t.membreId());
    assertThat(refusee.has("read")).isTrue();

    JsonNode activite =
        api.get("/v1/me/activite?mode=GERE&tontineId=" + t.tontineId(), t.gestionnaire().jeton())
            .data();
    assertThat(activite.path("content").get(0).path("type").asText())
        .isEqualTo("COTISATION_REFUSEE");
    assertThat(activite.path("content").get(0).path("membreNom").asText()).contains("Membre");
  }

  @Test
  @DisplayName("export CSV : en-tête, BOM, une ligne par cotisation ; format inconnu refusé (B24)")
  void exportCsv() {
    TontineEnCours t = donnees.tontineEnCours("Export");
    donnees.declarer(t.membre().jeton(), t.tontineId(), t.cycleId(), MONTANT);

    Reponse r = api.get(cotisations(t) + "/export", t.gestionnaire().jeton());
    assertThat(r.statut()).isEqualTo(200);
    assertThat(r.entete("Content-Type")).startsWith("text/csv");
    assertThat(r.entete("Content-Disposition")).contains("attachment").contains(".csv");
    byte[] octets = r.octets();
    assertThat(new byte[] {octets[0], octets[1], octets[2]}).containsExactly(0xEF, 0xBB, 0xBF);
    String[] lignes = new String(octets, StandardCharsets.UTF_8).substring(1).split("\r\n");
    assertThat(lignes[0]).startsWith("Tour;Membre;Téléphone");
    assertThat(lignes).hasSize(2);

    assertThat(api.get(cotisations(t) + "/export?format=xlsx", t.gestionnaire().jeton()).code())
        .isEqualTo("INVALID_PARAMETER");
    assertThat(api.get(cotisations(t) + "/export", t.membre().jeton()).statut()).isEqualTo(403);
  }
}
