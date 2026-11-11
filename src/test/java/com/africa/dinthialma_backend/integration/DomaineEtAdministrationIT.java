package com.africa.dinthialma_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.integration.support.AbstractIntegrationTest;
import com.africa.dinthialma_backend.integration.support.ApiClient.Reponse;
import com.africa.dinthialma_backend.integration.support.TestDataFactory;
import com.africa.dinthialma_backend.integration.support.TestDataFactory.TontineEnCours;
import com.africa.dinthialma_backend.integration.support.TestDataFactory.Utilisateur;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cœur du domaine (clôture de tour, jackpot, commissions, gagnants manuels, tontine événementielle,
 * statut des membres) et parcours d'administration et d'authentification (PIN, mot de passe oublié,
 * console SUPER_ADMIN).
 */
@DisplayName("Domaine, authentification et administration")
class DomaineEtAdministrationIT extends AbstractIntegrationTest {

  private String valider(String jeton, String tontineId, JsonNode cotisation) {
    TestDataFactory.attendre(
        api.put(
            "/v1/tontines/"
                + tontineId
                + "/cotisations/"
                + cotisation.path("id").asText()
                + "/valider",
            jeton,
            null),
        200);
    return cotisation.path("id").asText();
  }

  private JsonNode cloturer(String jeton, String tontineId, String cycleId) {
    return TestDataFactory.attendre(
            api.put("/v1/tontines/" + tontineId + "/cycles/" + cycleId + "/cloturer", jeton, null),
            200)
        .data();
  }

  @Test
  @DisplayName("clôture ROTATION : jackpot, commission 10 %, gagnant, retards, jackpot reçu")
  void clotureRotationAvecCommission() {
    Utilisateur g = donnees.inscrire("Gestionnaire");
    Utilisateur m1 = donnees.inscrire("Premier");
    Utilisateur m2 = donnees.inscrire("Second");
    Utilisateur m3 = donnees.inscrire("Retardataire");
    String tid = donnees.creerTontine(g.jeton(), "Jackpot").path("id").asText();
    TestDataFactory.attendre(
        api.post(
            "/v1/tontines/" + tid + "/commissions",
            g.jeton(),
            Map.of("type", "POURCENTAGE_JACKPOT", "valeur", 10)),
        201);
    String premierId = donnees.ajouterMembre(g.jeton(), tid, m1.phone()).path("id").asText();
    donnees.ajouterMembre(g.jeton(), tid, m2.phone());
    donnees.ajouterMembre(g.jeton(), tid, m3.phone());
    donnees.activer(g.jeton(), tid);
    String cycleId = donnees.ouvrirCycle(g.jeton(), tid).path("id").asText();

    valider(g.jeton(), tid, donnees.declarer(m1.jeton(), tid, cycleId, 1000));
    valider(g.jeton(), tid, donnees.declarer(m2.jeton(), tid, cycleId, 1000));
    String enAttente = donnees.declarer(m3.jeton(), tid, cycleId, 1000).path("id").asText();

    JsonNode cycle = cloturer(g.jeton(), tid, cycleId);
    assertThat(cycle.path("statut").asText()).isEqualTo("TERMINE");
    assertThat(cycle.path("montantJackpot").asDouble()).isEqualTo(2000);
    assertThat(cycle.path("montantCommission").asDouble()).isEqualTo(200);
    assertThat(cycle.path("montantNet").asDouble()).isEqualTo(1800);
    assertThat(cycle.path("gagnants")).hasSize(1);
    assertThat(cycle.path("gagnants").get(0).path("membreId").asText()).isEqualTo(premierId);

    // La déclaration non validée passe EN_RETARD
    assertThat(
            api.get("/v1/tontines/" + tid + "/cotisations/" + enAttente, g.jeton())
                .data()
                .path("statut")
                .asText())
        .isEqualTo("EN_RETARD");

    // Le gagnant voit son jackpot, son membership est marqué
    JsonNode jackpots = api.get("/v1/me/jackpots", m1.jeton()).data();
    assertThat(jackpots.path("totalRecu").asDouble()).isEqualTo(1800);
    assertThat(jackpots.path("recus").get(0).path("numeroCycle").asInt()).isEqualTo(1);
    JsonNode membres =
        api.get("/v1/tontines/" + tid + "/membres", g.jeton()).data().path("content");
    JsonNode premier =
        StreamSupport.stream(membres.spliterator(), false)
            .filter(x -> x.path("id").asText().equals(premierId))
            .findFirst()
            .orElseThrow();
    assertThat(premier.path("aRecuJackpot").asBoolean()).isTrue();
    assertThat(premier.has("arecuJackpot")).isFalse();
  }

  @Test
  @DisplayName("ordre MANUEL : pas de gagnant à la clôture, désignation, doublon refusé")
  void gagnantsManuels() {
    Utilisateur g = donnees.inscrire();
    Utilisateur m1 = donnees.inscrire();
    Utilisateur m2 = donnees.inscrire();
    String tid =
        donnees
            .creerTontine(g.jeton(), "Manuel", Map.of("ordreBeneficiaire", "MANUEL"))
            .path("id")
            .asText();
    String m1Id = donnees.ajouterMembre(g.jeton(), tid, m1.phone()).path("id").asText();
    String m2Id = donnees.ajouterMembre(g.jeton(), tid, m2.phone()).path("id").asText();
    donnees.activer(g.jeton(), tid);
    String cycleId = donnees.ouvrirCycle(g.jeton(), tid).path("id").asText();
    valider(g.jeton(), tid, donnees.declarer(m1.jeton(), tid, cycleId, 1000));

    assertThat(cloturer(g.jeton(), tid, cycleId).path("gagnants")).isEmpty();
    JsonNode stats =
        StreamSupport.stream(
                api.get("/v1/admin/my-dashboard", g.jeton())
                    .data()
                    .path("tontines")
                    .path("content")
                    .spliterator(),
                false)
            .filter(x -> x.path("tontineId").asText().equals(tid))
            .findFirst()
            .orElseThrow();
    assertThat(stats.path("gagnantsADesigner").asBoolean()).isTrue();

    String designer = "/v1/tontines/" + tid + "/cycles/" + cycleId + "/beneficiaire";
    assertThat(api.patch(designer, g.jeton(), Map.of("membreIds", List.of(m1Id, m1Id))).statut())
        .isEqualTo(400);
    Reponse ok = api.patch(designer, g.jeton(), Map.of("membreIds", List.of(m2Id)));
    assertThat(ok.statut()).isEqualTo(200);
    assertThat(ok.data().path("gagnants").get(0).path("membreId").asText()).isEqualTo(m2Id);
    assertThat(api.patch(designer, g.jeton(), Map.of("membreIds", List.of(m1Id))).statut())
        .isEqualTo(400);
  }

  @Test
  @DisplayName(
      "événementielle : sous-cycles quotidiens, clôture intermédiaire puis distribution finale")
  void tontineEvenementielle() {
    Utilisateur g = donnees.inscrire();
    Utilisateur m = donnees.inscrire();
    LocalDate aujourdHui = LocalDate.now();
    String tid =
        donnees
            .creerTontine(
                g.jeton(),
                "Tabaski test",
                Map.of(
                    "tontineType",
                    "EVENEMENTIELLE",
                    "frequence",
                    "JOURNALIERE",
                    "montant",
                    500,
                    "dateDebut",
                    aujourdHui.toString(),
                    "dateEcheance",
                    aujourdHui.plusDays(1).toString(),
                    "nomEvenement",
                    "Tabaski"))
            .path("id")
            .asText();
    donnees.ajouterMembre(g.jeton(), tid, m.phone());
    donnees.activer(g.jeton(), tid);

    JsonNode cycles = api.get("/v1/tontines/" + tid + "/cycles", g.jeton()).data().path("content");
    assertThat(cycles).hasSize(2);
    String c1 = cycles.get(0).path("id").asText();
    String c2 = cycles.get(1).path("id").asText();

    // Montant fixe imposé
    Reponse mauvaisMontant =
        api.post(
            "/v1/tontines/" + tid + "/cotisations",
            m.jeton(),
            Map.of("cycleId", c1, "montant", 400));
    assertThat(mauvaisMontant.code()).isEqualTo("AMOUNT_MISMATCH");

    valider(g.jeton(), tid, donnees.declarer(m.jeton(), tid, c1, 500));
    cloturer(g.jeton(), tid, c1);
    assertThat(
            api.get("/v1/tontines/" + tid + "/cycles/" + c2, g.jeton())
                .data()
                .path("statut")
                .asText())
        .isEqualTo("EN_COURS");

    valider(g.jeton(), tid, donnees.declarer(m.jeton(), tid, c2, 500));
    JsonNode finale = cloturer(g.jeton(), tid, c2);
    JsonNode distribution = finale.path("distributionParMembre");
    assertThat(distribution).hasSize(1);
    assertThat(distribution.get(0).path("montantCotise").asDouble()).isEqualTo(1000);
    assertThat(distribution.get(0).path("montantNet").asDouble()).isEqualTo(1000);
  }

  @Test
  @DisplayName("statut d'un membre : suspendu (plus de cotisation due), notifié")
  void statutMembre() {
    TontineEnCours t = donnees.tontineEnCours("Statut");
    Reponse r =
        api.patch(
            "/v1/tontines/" + t.tontineId() + "/membres/" + t.membreId() + "/statut",
            t.gestionnaire().jeton(),
            Map.of("statut", "SUSPENDU"));
    assertThat(r.statut()).isEqualTo(200);
    JsonNode ligne = api.get("/v1/me/summary", t.membre().jeton()).data().path("tontines").get(0);
    assertThat(ligne.path("monStatutMembre").asText()).isEqualTo("SUSPENDU");
    assertThat(ligne.has("cotisationDue")).isFalse();
    JsonNode notifs = api.get("/v1/notifications", t.membre().jeton()).data().path("content");
    assertThat(
            StreamSupport.stream(notifs.spliterator(), false)
                .anyMatch(n -> n.path("type").asText().equals("STATUT_MEMBRE")))
        .isTrue();
  }

  @Test
  @DisplayName("code PIN : création, refus des PIN simples, connexion par PIN, PIN faux")
  void codePin() {
    Utilisateur u = donnees.inscrire();
    assertThat(
            api.post(
                    "/v1/auth/pin/setup",
                    u.jeton(),
                    Map.of("pin", "123456", "confirmPin", "123456"))
                .code())
        .isEqualTo("PIN_TOO_SIMPLE");
    assertThat(
            api.post(
                    "/v1/auth/pin/setup",
                    u.jeton(),
                    Map.of("pin", "482915", "confirmPin", "482916"))
                .code())
        .isEqualTo("PIN_MISMATCH");
    assertThat(
            api.post(
                    "/v1/auth/pin/setup",
                    u.jeton(),
                    Map.of("pin", "482915", "confirmPin", "482915"))
                .statut())
        .isEqualTo(200);

    Reponse ok =
        api.post(
            "/v1/auth/login-pin",
            null,
            Map.of("phone", u.phone(), "pin", "482915", "clientType", "WEB"));
    assertThat(ok.statut()).isEqualTo(200);
    assertThat(ok.data().path("access_token").asText()).isNotBlank();
    Reponse faux =
        api.post(
            "/v1/auth/login-pin",
            null,
            Map.of("phone", u.phone(), "pin", "482916", "clientType", "WEB"));
    assertThat(faux.statut()).isEqualTo(401);
    assertThat(faux.code()).isEqualTo("PIN_INVALID");
  }

  @Test
  @DisplayName("mot de passe oublié : OTP, nouveau mot de passe, ancien refusé")
  void motDePasseOublie() {
    Utilisateur u = donnees.inscrire();
    TestDataFactory.attendre(
        api.post("/v1/auth/forgot-password/send-otp", null, Map.of("phone", u.phone())), 200);
    String code = donnees.otp(u.phone(), Constants.OtpPurpose.RESET_PASSWORD);
    TestDataFactory.attendre(
        api.post(
            "/v1/auth/forgot-password/verify-otp", null, Map.of("phone", u.phone(), "code", code)),
        200);
    TestDataFactory.attendre(
        api.post(
            "/v1/auth/forgot-password/reset",
            null,
            Map.of("phone", u.phone(), "code", code, "newPassword", "Reinit@2026")),
        200);

    assertThat(donnees.connecter(u.phone(), "Reinit@2026", "WEB").jeton()).isNotBlank();
    Reponse ancien =
        api.post(
            "/v1/auth/login",
            null,
            Map.of(
                "username",
                u.phone(),
                "password",
                TestDataFactory.MOT_DE_PASSE,
                "clientType",
                "WEB"));
    assertThat(ancien.code()).isEqualTo("INVALID_CREDENTIALS");
  }

  @Test
  @DisplayName("console SUPER_ADMIN : tableau de bord, désactivation puis réactivation d'un compte")
  void consoleAdministration() {
    Utilisateur superAdmin = donnees.superAdmin();
    Utilisateur u = donnees.inscrire();
    assertThat(api.get("/v1/admin/dashboard", superAdmin.jeton()).statut()).isEqualTo(200);

    TestDataFactory.attendre(
        api.post("/v1/admin/dashboard/users/" + u.userId() + "/disable", superAdmin.jeton(), null),
        200);
    Reponse bloque =
        api.post(
            "/v1/auth/login",
            null,
            Map.of(
                "username",
                u.phone(),
                "password",
                TestDataFactory.MOT_DE_PASSE,
                "clientType",
                "WEB"));
    assertThat(bloque.statut()).isEqualTo(403);
    assertThat(bloque.code()).isEqualTo("ACCOUNT_DISABLED");

    TestDataFactory.attendre(
        api.post("/v1/admin/dashboard/users/" + u.userId() + "/enable", superAdmin.jeton(), null),
        200);
    assertThat(donnees.connecter(u.phone(), TestDataFactory.MOT_DE_PASSE, "WEB").jeton())
        .isNotBlank();
  }
}
