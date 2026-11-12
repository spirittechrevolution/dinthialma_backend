package com.africa.dinthialma_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.africa.dinthialma_backend.support.AbstractIntegrationTest;
import com.africa.dinthialma_backend.support.ApiClient.Reponse;
import com.africa.dinthialma_backend.support.TestDataFactory;
import com.africa.dinthialma_backend.support.TestDataFactory.Utilisateur;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Compte connecté : inscription avec connexion automatique (B13), mot de passe (B21), appareils
 * (B20), préférences (B22) ; et référentiel de valeurs (B23).
 */
@DisplayName("Compte et référentiel")
class CompteEtReferentielIT extends AbstractIntegrationTest {

  @Test
  @DisplayName("inscription sans clientType : pas de jetons, connexion manuelle ensuite (B13)")
  void inscriptionSansConnexionAuto() {
    String phone = TestDataFactory.nouveauTelephone();
    donnees.verifierTelephone(phone);
    Reponse r =
        api.post(
            "/v1/auth/register/complete",
            null,
            Map.of(
                "phone",
                phone,
                "firstName",
                "Sans",
                "lastName",
                "Auto",
                "password",
                TestDataFactory.MOT_DE_PASSE));
    assertThat(r.statut()).isEqualTo(201);
    assertThat(r.data().has("access_token")).isFalse();
    assertThat(donnees.connecter(phone, TestDataFactory.MOT_DE_PASSE, "WEB").jeton()).isNotBlank();
  }

  @Test
  @DisplayName("inscription avec clientType : jetons utilisables, PIN non configuré (B13)")
  void inscriptionAvecConnexionAuto() {
    Utilisateur u = donnees.inscrire("Auto");
    assertThat(api.get("/v1/profile", u.jeton()).statut()).isEqualTo(200);
    assertThat(api.get("/v1/me/summary", u.jeton()).data().path("tontines")).isEmpty();
  }

  @Test
  @DisplayName(
      "appareils connectés, changement de mot de passe et déconnexion à distance (B20, B21)")
  void sessionsEtMotDePasse() {
    Utilisateur web = donnees.inscrire("Sessions");
    Utilisateur mobile = donnees.connecter(web.phone(), TestDataFactory.MOT_DE_PASSE, "MOBILE");

    JsonNode sessions = api.get("/v1/me/sessions", web.jeton()).data();
    assertThat(sessions).hasSize(2);
    JsonNode courante = trouver(sessions, "WEB");
    assertThat(courante.path("courante").asBoolean()).isTrue();
    assertThat(trouver(sessions, "MOBILE").path("courante").asBoolean()).isFalse();
    assertThat(courante.path("deviceInfo").asText()).isEqualTo("Tests d'intégration");

    String nouveau = "Nouveau@Mdp2026";
    assertThat(
            api.post(
                    "/v1/me/password",
                    web.jeton(),
                    Map.of("ancienMotDePasse", "faux", "nouveauMotDePasse", nouveau))
                .code())
        .isEqualTo("CURRENT_PASSWORD_INVALID");
    assertThat(
            api.post(
                    "/v1/me/password",
                    web.jeton(),
                    Map.of(
                        "ancienMotDePasse",
                        TestDataFactory.MOT_DE_PASSE,
                        "nouveauMotDePasse",
                        TestDataFactory.MOT_DE_PASSE))
                .code())
        .isEqualTo("PASSWORD_UNCHANGED");
    Reponse change =
        api.post(
            "/v1/me/password",
            web.jeton(),
            Map.of(
                "ancienMotDePasse",
                TestDataFactory.MOT_DE_PASSE,
                "nouveauMotDePasse",
                nouveau,
                "deconnecterAutresAppareils",
                true));
    assertThat(change.statut()).isEqualTo(200);

    assertThat(api.get("/v1/me/sessions", web.jeton()).data()).hasSize(1);
    assertThat(
            api.post("/v1/auth/refresh", null, Map.of("refreshToken", mobile.refreshToken()))
                .code())
        .isEqualTo("SESSION_EXPIRED");
    assertThat(donnees.connecter(web.phone(), nouveau, "MOBILE").jeton()).isNotBlank();

    // La session d'un autre utilisateur est introuvable ; déconnexion à distance de la mienne
    Utilisateur autre = donnees.inscrire();
    String sessionAutre =
        api.get("/v1/me/sessions", autre.jeton()).data().get(0).path("id").asText();
    assertThat(api.delete("/v1/me/sessions/" + sessionAutre, web.jeton()).statut()).isEqualTo(404);
    String sessionWeb =
        trouver(api.get("/v1/me/sessions", web.jeton()).data(), "WEB").path("id").asText();
    assertThat(api.delete("/v1/me/sessions/" + sessionWeb, web.jeton()).statut()).isEqualTo(200);
    assertThat(
            api.post("/v1/auth/refresh", null, Map.of("refreshToken", web.refreshToken())).statut())
        .isEqualTo(401);
  }

  @Test
  @DisplayName("préférences synchronisées, isolées par utilisateur (B22)")
  void preferences() {
    Utilisateur u = donnees.inscrire();
    JsonNode defaut = api.get("/v1/me/preferences", u.jeton()).data();
    assertThat(defaut.path("tontinesFavorites")).isEmpty();
    assertThat(defaut.path("masquerMontant").asBoolean()).isFalse();

    String t1 = UUID.randomUUID().toString();
    String t2 = UUID.randomUUID().toString();
    Reponse put =
        api.put(
            "/v1/me/preferences",
            u.jeton(),
            Map.of(
                "tontinesFavorites",
                List.of(t1, t2, t1),
                "dernierMode",
                "GERE",
                "masquerMontant",
                true,
                "inconnu",
                "x"));
    assertThat(put.data().path("tontinesFavorites")).hasSize(2);
    JsonNode relu = api.get("/v1/me/preferences", u.jeton()).data();
    assertThat(relu.path("dernierMode").asText()).isEqualTo("GERE");
    assertThat(relu.has("inconnu")).isFalse();
    assertThat(
            api.get("/v1/me/preferences", donnees.inscrire().jeton())
                .data()
                .path("tontinesFavorites"))
        .isEmpty();
  }

  @Test
  @DisplayName(
      "référentiel : type restreint, suppression logique, valeur système protégée, restauration (B23)")
  void referentiel() {
    Utilisateur superAdmin = donnees.superAdmin();
    Utilisateur simple = donnees.inscrire();
    String type = "METHODE_PAIEMENT";
    String valeur = "TEST_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    assertThat(api.get("/v1/code-list/type/ROLE_INTERNE", null).statut()).isEqualTo(403);
    assertThat(
            api.post(
                    "/v1/code-list",
                    simple.jeton(),
                    Map.of("type", type, "value", valeur, "description", "x"))
                .statut())
        .isEqualTo(403);

    JsonNode cree =
        api.post(
                "/v1/code-list",
                superAdmin.jeton(),
                Map.of("type", type, "value", valeur, "description", "Test"))
            .data();
    String id = cree.path("id").asText();
    JsonNode systeme =
        StreamSupport.stream(
                api.get("/v1/code-list/type/" + type, null).data().spliterator(), false)
            .filter(x -> x.path("isSystemAssign").asBoolean() || x.path("systemAssign").asBoolean())
            .findFirst()
            .orElseThrow();
    assertThat(
            api.delete("/v1/code-list/" + systeme.path("id").asText(), superAdmin.jeton()).code())
        .isEqualTo("CODE_LIST_SYSTEM_VALUE");

    assertThat(api.delete("/v1/code-list/" + id, superAdmin.jeton()).statut()).isEqualTo(200);
    assertThat(api.get("/v1/code-list/" + id, superAdmin.jeton()).statut()).isEqualTo(404);
    assertThat(api.get("/v1/code-list/type/" + type, null).texte()).doesNotContain(valeur);

    JsonNode restauree =
        api.post(
                "/v1/code-list",
                superAdmin.jeton(),
                Map.of("type", type, "value", valeur, "description", "Restaurée"))
            .data();
    assertThat(restauree.path("id").asText()).isEqualTo(id);
    assertThat(
            api.post(
                    "/v1/code-list",
                    superAdmin.jeton(),
                    Map.of("type", type, "value", valeur, "description", "x"))
                .code())
        .isEqualTo("CONFLICT");
  }

  private static JsonNode trouver(JsonNode sessions, String clientType) {
    return StreamSupport.stream(sessions.spliterator(), false)
        .filter(s -> s.path("clientType").asText().equals(clientType))
        .findFirst()
        .orElseThrow();
  }
}
