package com.africa.dinthialma_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.africa.dinthialma_backend.support.AbstractIntegrationTest;
import com.africa.dinthialma_backend.support.ApiClient.Reponse;
import com.africa.dinthialma_backend.support.TestDataFactory;
import com.africa.dinthialma_backend.support.TestDataFactory.Utilisateur;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exceptions à la règle convenue : priorité de jackpot pour un membre dans le besoin et changement
 * de l'ordre des bénéficiaires en cours de tontine.
 */
@DisplayName("Priorités de jackpot et ordre des bénéficiaires en cours")
class PrioriteJackpotIT extends AbstractIntegrationTest {

  private String base(String tontineId) {
    return "/v1/tontines/" + tontineId + "/priorites";
  }

  private JsonNode cloturer(String jeton, String tontineId, String cycleId) {
    return TestDataFactory.attendre(
            api.put("/v1/tontines/" + tontineId + "/cycles/" + cycleId + "/cloturer", jeton, null),
            200)
        .data();
  }

  private String gagnantUnique(JsonNode cycle) {
    assertThat(cycle.path("gagnants")).hasSize(1);
    return cycle.path("gagnants").get(0).path("membreId").asText();
  }

  private boolean notificationRecue(Utilisateur u, String type) {
    JsonNode notifs = api.get("/v1/notifications?size=50", u.jeton()).data().path("content");
    return StreamSupport.stream(notifs.spliterator(), false)
        .anyMatch(n -> n.path("type").asText().equals(type));
  }

  @Test
  @DisplayName(
      "tirage convenu : un membre dans le besoin demande, le gestionnaire accepte, il gagne le tour"
          + " suivant ; puis attribution directe, refus, annulation")
  void prioriteSurTontineParTirage() {
    Utilisateur g = donnees.inscrire("Gestionnaire");
    Utilisateur besoin = donnees.inscrire("Besoin");
    Utilisateur autre = donnees.inscrire("Autre");
    Utilisateur troisieme = donnees.inscrire("Troisieme");
    String tid =
        donnees
            .creerTontine(g.jeton(), "Tirage", Map.of("ordreBeneficiaire", "ALEATOIRE"))
            .path("id")
            .asText();
    String besoinId = donnees.ajouterMembre(g.jeton(), tid, besoin.phone()).path("id").asText();
    String autreId = donnees.ajouterMembre(g.jeton(), tid, autre.phone()).path("id").asText();
    donnees.ajouterMembre(g.jeton(), tid, troisieme.phone());

    // Tontine pas encore démarrée : pas de priorité
    Reponse brouillon = api.post(base(tid), besoin.jeton(), Map.of());
    assertThat(brouillon.statut()).isEqualTo(400);
    assertThat(brouillon.code()).isEqualTo("PRIORITE_NON_DISPONIBLE");
    donnees.activer(g.jeton(), tid);

    // Demande du membre, puis doublon refusé
    Reponse demande =
        TestDataFactory.attendre(
            api.post(base(tid), besoin.jeton(), Map.of("motif", "Frais d'hospitalisation")), 201);
    String prioriteId = demande.data().path("id").asText();
    assertThat(demande.data().path("statut").asText()).isEqualTo("EN_ATTENTE");
    assertThat(api.post(base(tid), besoin.jeton(), Map.of()).code())
        .isEqualTo("PRIORITE_DEJA_ACTIVE");
    assertThat(notificationRecue(g, "PRIORITE_JACKPOT_DEMANDEE")).isTrue();

    // Motif visible du gestionnaire, masqué aux autres membres
    assertThat(api.get(base(tid), g.jeton()).data().path("content").get(0).path("motif").asText())
        .isEqualTo("Frais d'hospitalisation");
    assertThat(api.get(base(tid), autre.jeton()).data().path("content").get(0).hasNonNull("motif"))
        .isFalse();
    assertThat(
            api.get("/v1/admin/my-dashboard", g.jeton())
                .data()
                .path("tontines")
                .path("content")
                .get(0)
                .path("prioritesEnAttente")
                .asLong())
        .isEqualTo(1);

    // Acceptation : tous les membres actifs sont informés
    TestDataFactory.attendre(
        api.put(base(tid) + "/" + prioriteId + "/accepter", g.jeton(), null), 200);
    assertThat(notificationRecue(autre, "PRIORITE_JACKPOT_ACCORDEE")).isTrue();

    // Tour 1 : malgré le tirage convenu, le membre dans le besoin gagne ; priorité SERVIE
    String tour1 = donnees.ouvrirCycle(g.jeton(), tid).path("id").asText();
    assertThat(gagnantUnique(cloturer(g.jeton(), tid, tour1))).isEqualTo(besoinId);
    JsonNode servie = api.get(base(tid) + "?statut=SERVIE", g.jeton()).data().path("content");
    assertThat(servie).hasSize(1);
    assertThat(servie.get(0).path("cycleId").asText()).isEqualTo(tour1);
    assertThat(api.post(base(tid), besoin.jeton(), Map.of()).code())
        .isEqualTo("MEMBRE_NON_ELIGIBLE_JACKPOT");

    // Attribution directe (accord pris en réunion) : gagne le tour 2
    TestDataFactory.attendre(
        api.post(base(tid) + "/attribuer", g.jeton(), Map.of("membreId", autreId)), 201);
    String tour2 = donnees.ouvrirCycle(g.jeton(), tid).path("id").asText();
    assertThat(gagnantUnique(cloturer(g.jeton(), tid, tour2))).isEqualTo(autreId);

    // Refus avec motif, puis nouvelle demande annulée par le membre lui-même
    String refusee =
        TestDataFactory.attendre(api.post(base(tid), troisieme.jeton(), Map.of()), 201)
            .data()
            .path("id")
            .asText();
    Reponse refus =
        TestDataFactory.attendre(
            api.put(
                base(tid) + "/" + refusee + "/refuser", g.jeton(), Map.of("motif", "Pas ce mois")),
            200);
    assertThat(refus.data().path("statut").asText()).isEqualTo("REFUSEE");
    assertThat(notificationRecue(troisieme, "PRIORITE_JACKPOT_REFUSEE")).isTrue();
    String annulee =
        TestDataFactory.attendre(api.post(base(tid), troisieme.jeton(), Map.of()), 201)
            .data()
            .path("id")
            .asText();
    assertThat(api.put(base(tid) + "/" + refusee + "/accepter", g.jeton(), null).code())
        .isEqualTo("PRIORITE_NOT_PENDING");
    assertThat(
            TestDataFactory.attendre(api.delete(base(tid) + "/" + annulee, troisieme.jeton()), 200)
                .data()
                .path("statut")
                .asText())
        .isEqualTo("ANNULEE");
    assertThat(api.put(base(tid) + "/" + annulee + "/accepter", autre.jeton(), null).statut())
        .isEqualTo(403);
  }

  @Test
  @DisplayName(
      "ordre changé en cours : ROTATION -> HYBRIDE, gagnants à désigner, retour en automatique"
          + " bloqué tant que le tour attend")
  void changementOrdreEnCours() {
    Utilisateur g = donnees.inscrire("Gestionnaire");
    Utilisateur m1 = donnees.inscrire("Un");
    Utilisateur m2 = donnees.inscrire("Deux");
    String tid = donnees.creerTontine(g.jeton(), "Rotation").path("id").asText();
    donnees.ajouterMembre(g.jeton(), tid, m1.phone());
    String m2Id = donnees.ajouterMembre(g.jeton(), tid, m2.phone()).path("id").asText();
    donnees.activer(g.jeton(), tid);
    String chemin = "/v1/tontines/" + tid + "/ordre-beneficiaire";

    Reponse hybride =
        TestDataFactory.attendre(
            api.patch(chemin, g.jeton(), Map.of("ordreBeneficiaire", "HYBRIDE")), 200);
    assertThat(hybride.data().path("ordreBeneficiaire").asText()).isEqualTo("HYBRIDE");
    assertThat(notificationRecue(m1, "ORDRE_BENEFICIAIRE_MODIFIE")).isTrue();
    assertThat(api.patch(chemin, m1.jeton(), Map.of("ordreBeneficiaire", "ROTATION")).statut())
        .isEqualTo(403);

    String tour1 = donnees.ouvrirCycle(g.jeton(), tid).path("id").asText();
    assertThat(cloturer(g.jeton(), tid, tour1).path("gagnants")).isEmpty();
    assertThat(notificationRecue(g, "GAGNANTS_A_DESIGNER")).isTrue();

    Reponse bloque = api.patch(chemin, g.jeton(), Map.of("ordreBeneficiaire", "ROTATION"));
    assertThat(bloque.statut()).isEqualTo(409);
    assertThat(bloque.code()).isEqualTo("DESIGNATION_EN_ATTENTE");

    TestDataFactory.attendre(
        api.patch(
            "/v1/tontines/" + tid + "/cycles/" + tour1 + "/beneficiaire",
            g.jeton(),
            Map.of("membreIds", List.of(m2Id))),
        200);
    TestDataFactory.attendre(
        api.patch(chemin, g.jeton(), Map.of("ordreBeneficiaire", "ROTATION")), 200);

    // Retour en ROTATION : le membre restant gagne automatiquement le tour suivant
    String tour2 = donnees.ouvrirCycle(g.jeton(), tid).path("id").asText();
    assertThat(gagnantUnique(cloturer(g.jeton(), tid, tour2))).isNotEqualTo(m2Id);
  }
}
