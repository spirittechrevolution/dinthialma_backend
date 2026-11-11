package com.africa.dinthialma_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.africa.dinthialma_backend.integration.support.AbstractIntegrationTest;
import com.africa.dinthialma_backend.integration.support.ApiClient.Reponse;
import com.africa.dinthialma_backend.integration.support.TestDataFactory.TontineEnCours;
import com.africa.dinthialma_backend.integration.support.TestDataFactory.Utilisateur;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Invitation par code (B16), demandes d'adhésion (B17) et QR membre (B15). */
@DisplayName("Invitations, adhésion et QR")
class AdhesionEtQrIT extends AbstractIntegrationTest {

  @Test
  @DisplayName("invitation : code stable, aperçu public minimal, QR t_, révocation (B16)")
  void invitation() {
    Utilisateur g = donnees.inscrire("Gérante");
    String tid =
        donnees
            .creerTontine(g.jeton(), "Invitation", Map.of("nombreMembres", 2))
            .path("id")
            .asText();

    JsonNode inv = api.post("/v1/tontines/" + tid + "/invitation", g.jeton(), null).data();
    String code = inv.path("code").asText();
    assertThat(code).matches("[A-Z2-9]{4}-[A-Z2-9]{4}");
    assertThat(inv.path("qrToken").asText()).isEqualTo("t_" + code);
    assertThat(inv.path("url").asText()).endsWith("/q/t/" + code);
    assertThat(
            api.post("/v1/tontines/" + tid + "/invitation", g.jeton(), null)
                .data()
                .path("code")
                .asText())
        .isEqualTo(code);

    // Aperçu public, sans JWT, insensible à la casse et au tiret
    Reponse apercu = api.get("/v1/public/invitations/" + code.replace("-", "").toLowerCase(), null);
    assertThat(apercu.statut()).isEqualTo(200);
    assertThat(apercu.data().path("nom").asText()).isEqualTo("Invitation");
    assertThat(apercu.data().path("gestionnairePrenom").asText()).isEqualTo("Gérante");
    assertThat(apercu.data().path("placesRestantes").asInt()).isEqualTo(2);
    assertThat(apercu.data().path("ouverte").asBoolean()).isTrue();
    assertThat(apercu.texte()).doesNotContain(g.phoneNormalise());

    Utilisateur autre = donnees.inscrire();
    JsonNode qr = api.get("/v1/qr/t_" + code, autre.jeton()).data();
    assertThat(qr.path("type").asText()).isEqualTo("TONTINE");
    assertThat(qr.path("invitationCode").asText()).isEqualTo(code);

    // Un tiers ne peut pas gérer l'invitation ; révocation -> 404
    assertThat(api.post("/v1/tontines/" + tid + "/invitation", autre.jeton(), null).statut())
        .isEqualTo(403);
    assertThat(api.delete("/v1/tontines/" + tid + "/invitation", g.jeton()).statut())
        .isEqualTo(200);
    assertThat(api.get("/v1/public/invitations/" + code, null).code()).isEqualTo("NOT_FOUND");
    assertThat(api.get("/v1/qr/t_" + code, autre.jeton()).statut()).isEqualTo(404);
  }

  @Test
  @DisplayName("demande, doublon, refus, nouvelle demande, acceptation, complet (B17)")
  void demandesAdhesion() {
    Utilisateur g = donnees.inscrire("Gestionnaire");
    String tid =
        donnees.creerTontine(g.jeton(), "Adhésion", Map.of("nombreMembres", 2)).path("id").asText();
    donnees.ajouterMembre(g.jeton(), tid, g.phone()); // 1 place restante
    String code =
        api.post("/v1/tontines/" + tid + "/invitation", g.jeton(), null)
            .data()
            .path("code")
            .asText();
    Utilisateur demandeur = donnees.inscrire("Demandeur");
    String demandes = "/v1/invitations/" + code + "/demandes";

    assertThat(api.post(demandes, g.jeton(), Map.of()).statut()).isEqualTo(400);

    Reponse d1 = api.post(demandes, demandeur.jeton(), Map.of("message", "Bonjour"));
    assertThat(d1.statut()).isEqualTo(201);
    assertThat(d1.data().path("statut").asText()).isEqualTo("EN_ATTENTE");
    assertThat(d1.data().has("phone")).isFalse();
    assertThat(api.post(demandes, demandeur.jeton(), Map.of()).code())
        .isEqualTo("DEMANDE_DEJA_EN_ATTENTE");

    JsonNode recues =
        api.get("/v1/me/gestion/demandes?statut=EN_ATTENTE&tontineId=" + tid, g.jeton())
            .data()
            .path("content");
    assertThat(recues).hasSize(1);
    assertThat(recues.get(0).path("phone").asText()).isEqualTo(demandeur.phoneNormalise());
    assertThat(notificationRecue(g, "DEMANDE_ADHESION")).isTrue();

    String d1Id = d1.data().path("id").asText();
    assertThat(
            api.put(
                    "/v1/tontines/" + tid + "/demandes/" + d1Id + "/refuser",
                    g.jeton(),
                    Map.of("motif", "Pas maintenant"))
                .data()
                .path("statut")
                .asText())
        .isEqualTo("REFUSEE");
    assertThat(
            api.put("/v1/tontines/" + tid + "/demandes/" + d1Id + "/accepter", g.jeton(), Map.of())
                .code())
        .isEqualTo("DEMANDE_NOT_PENDING");
    assertThat(notificationRecue(demandeur, "ADHESION_REFUSEE")).isTrue();

    String d2Id = api.post(demandes, demandeur.jeton(), Map.of()).data().path("id").asText();
    JsonNode acceptee =
        api.put(
                "/v1/tontines/" + tid + "/demandes/" + d2Id + "/accepter",
                g.jeton(),
                Map.of("ordreJackpot", 2))
            .data();
    assertThat(acceptee.path("statut").asText()).isEqualTo("ACCEPTEE");
    assertThat(acceptee.path("membreId").asText()).isNotBlank();
    assertThat(api.get("/v1/tontines/" + tid, demandeur.jeton()).data().path("monRole").asText())
        .isEqualTo("MEMBRE");
    assertThat(api.post(demandes, demandeur.jeton(), Map.of()).code())
        .isEqualTo("MEMBER_ALREADY_IN_TONTINE");

    // Tontine complète
    assertThat(api.get("/v1/public/invitations/" + code, null).data().path("ouverte").asBoolean())
        .isFalse();
    Utilisateur retardataire = donnees.inscrire();
    Reponse complet = api.post(demandes, retardataire.jeton(), Map.of());
    assertThat(complet.statut()).isEqualTo(409);
    assertThat(complet.code()).isEqualTo("TONTINE_FULL");
  }

  @Test
  @DisplayName(
      "QR membre : jeton stable sans téléphone, résolution limitée aux tontines gérées, régénération (B15)")
  void qrMembre() {
    TontineEnCours t = donnees.tontineEnCours("QR");
    JsonNode monQr = api.get("/v1/me/qr", t.membre().jeton()).data();
    String jeton = monQr.path("qrToken").asText();
    assertThat(jeton).startsWith("m_").doesNotContain(t.membre().phoneNormalise().substring(3));
    assertThat(api.get("/v1/me/qr", t.membre().jeton()).data().path("qrToken").asText())
        .isEqualTo(jeton);

    Reponse vuParGestionnaire = api.get("/v1/qr/" + jeton, t.gestionnaire().jeton());
    assertThat(vuParGestionnaire.texte()).doesNotContain(t.membre().phoneNormalise());
    JsonNode situation = vuParGestionnaire.data().path("dansMesTontines").get(0);
    assertThat(situation.path("membreId").asText()).isEqualTo(t.membreId());
    assertThat(situation.path("cycleEnCoursId").asText()).isEqualTo(t.cycleId());
    assertThat(situation.path("aPayeCycleEnCours").asBoolean()).isFalse();

    Utilisateur etranger = donnees.inscrire();
    assertThat(api.get("/v1/qr/" + jeton, etranger.jeton()).data().path("dansMesTontines"))
        .isEmpty();

    String nouveau =
        api.post("/v1/me/qr/regenerer", t.membre().jeton(), null).data().path("qrToken").asText();
    assertThat(nouveau).isNotEqualTo(jeton);
    assertThat(api.get("/v1/qr/" + jeton, t.gestionnaire().jeton()).code()).isEqualTo("NOT_FOUND");
    assertThat(api.get("/v1/qr/" + nouveau, t.gestionnaire().jeton()).statut()).isEqualTo(200);
  }

  private boolean notificationRecue(Utilisateur u, String type) {
    JsonNode notifs = api.get("/v1/notifications?size=20", u.jeton()).data().path("content");
    return StreamSupport.stream(notifs.spliterator(), false)
        .anyMatch(n -> n.path("type").asText().equals(type));
  }
}
