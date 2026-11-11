package com.africa.dinthialma_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.africa.dinthialma_backend.integration.support.AbstractIntegrationTest;
import com.africa.dinthialma_backend.integration.support.ApiClient.Reponse;
import com.africa.dinthialma_backend.integration.support.TestDataFactory;
import com.africa.dinthialma_backend.integration.support.TestDataFactory.Utilisateur;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Contrat transverse : codes d'erreur (B02), sécurité, encodage (B03), configuration (B25). */
@DisplayName("Sécurité, codes d'erreur et conventions de réponse")
class SecuriteEtErreursIT extends AbstractIntegrationTest {

  @Nested
  @DisplayName("Codes d'erreur stables (B02)")
  class CodesErreur {

    @Test
    @DisplayName("un succès ne porte pas de champ code")
    void succesSansCode() {
      Utilisateur u = donnees.inscrire();
      Reponse r = api.get("/v1/profile", u.jeton());
      assertThat(r.statut()).isEqualTo(200);
      assertThat(r.json().has("code")).isFalse();
    }

    @Test
    @DisplayName("mauvais mot de passe ou numéro inconnu -> 401 INVALID_CREDENTIALS")
    void identifiantsIncorrects() {
      Utilisateur u = donnees.inscrire();
      Reponse mauvaisMdp =
          api.post(
              "/v1/auth/login",
              null,
              Map.of("username", u.phone(), "password", "faux", "clientType", "WEB"));
      Reponse inconnu =
          api.post(
              "/v1/auth/login",
              null,
              Map.of(
                  "username", TestDataFactory.nouveauTelephone(),
                  "password", "x",
                  "clientType", "WEB"));
      assertThat(mauvaisMdp.statut()).isEqualTo(401);
      assertThat(mauvaisMdp.code()).isEqualTo("INVALID_CREDENTIALS");
      assertThat(inconnu.code()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("sans JWT -> 401 UNAUTHORIZED")
    void sansJeton() {
      Reponse r = api.get("/v1/tontines", null);
      assertThat(r.statut()).isEqualTo(401);
      assertThat(r.code()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("route inconnue -> 404, méthode non supportée -> 405")
    void erreursSpring() {
      Utilisateur u = donnees.inscrire();
      assertThat(api.get("/v1/route-inexistante", u.jeton()).code()).isEqualTo("NOT_FOUND");
      Reponse r = api.delete("/v1/me/summary", u.jeton());
      assertThat(r.statut()).isEqualTo(405);
      assertThat(r.code()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    @DisplayName("JSON illisible, corps invalide, paramètre mal typé -> 400 avec code dédié")
    void erreursDeRequete() {
      Utilisateur u = donnees.inscrire();
      assertThat(api.postBrut("/v1/tontines", u.jeton(), "{pas du json").code())
          .isEqualTo("MALFORMED_REQUEST");
      assertThat(api.post("/v1/tontines", u.jeton(), Map.of()).code())
          .isEqualTo("VALIDATION_ERROR");
      assertThat(api.get("/v1/tontines?role=XYZ", u.jeton()).code()).isEqualTo("INVALID_PARAMETER");
      assertThat(api.get("/v1/tontines/pas-un-uuid", u.jeton()).code())
          .isEqualTo("INVALID_PARAMETER");
      assertThat(api.get("/v1/tontines/" + UUID.randomUUID(), u.jeton()).code())
          .isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("OTP inconnu -> OTP_INVALID ; numéro déjà inscrit -> PHONE_ALREADY_USED")
    void otpEtTelephone() {
      Utilisateur u = donnees.inscrire();
      Reponse otp =
          api.post(
              "/v1/auth/register/verify-otp",
              null,
              Map.of("phone", TestDataFactory.nouveauTelephone(), "code", "123456"));
      assertThat(otp.code()).isEqualTo("OTP_INVALID");
      Reponse deja = api.post("/v1/auth/register/send-otp", null, Map.of("phone", u.phone()));
      assertThat(deja.statut()).isEqualTo(409);
      assertThat(deja.code()).isEqualTo("PHONE_ALREADY_USED");
    }
  }

  @Nested
  @DisplayName("Sécurité")
  class Securite {

    @Test
    @DisplayName(
        "refresh public : fonctionne sans access token ; jeton invalide -> SESSION_EXPIRED")
    void refreshPublic() {
      Utilisateur u = donnees.inscrire();
      Reponse ok = api.post("/v1/auth/refresh", null, Map.of("refreshToken", u.refreshToken()));
      assertThat(ok.statut()).isEqualTo(200);
      assertThat(ok.data().path("access_token").asText()).isNotBlank();

      Reponse ko = api.post("/v1/auth/refresh", null, Map.of("refreshToken", "invalide"));
      assertThat(ko.statut()).isEqualTo(401);
      assertThat(ko.code()).isEqualTo("SESSION_EXPIRED");
    }

    @Test
    @DisplayName("reset PIN public : pas de 401 sans JWT")
    void resetPinPublic() {
      Reponse r =
          api.post(
              "/v1/auth/pin/reset/verify-otp",
              null,
              Map.of("phone", TestDataFactory.nouveauTelephone(), "code", "123456"));
      assertThat(r.code()).isEqualTo("OTP_INVALID");
    }

    @Test
    @DisplayName("rôle insuffisant -> 403 FORBIDDEN (et non 401)")
    void roleInsuffisant() {
      Utilisateur u = donnees.inscrire();
      Reponse r = api.get("/v1/admin/dashboard/users", u.jeton());
      assertThat(r.statut()).isEqualTo(403);
      assertThat(r.code()).isEqualTo("FORBIDDEN");
    }
  }

  @Nested
  @DisplayName("Conventions de réponse")
  class Conventions {

    @Test
    @DisplayName("toutes les réponses déclarent charset=UTF-8 (B03)")
    void charsetUtf8() {
      Utilisateur u = donnees.inscrire("Aïcha");
      assertThat(api.get("/v1/profile", u.jeton()).entete("Content-Type"))
          .containsIgnoringCase("charset=UTF-8");
      assertThat(api.get("/v1/tontines", null).entete("Content-Type"))
          .containsIgnoringCase("charset=UTF-8");
      assertThat(api.get("/v1/profile", u.jeton()).data().path("firstName").asText())
          .isEqualTo("Aïcha");
    }

    @Test
    @DisplayName("configuration publique sans JWT ; champs non configurés absents (B25)")
    void configurationPublique() {
      Reponse r = api.get("/v1/public/config", null);
      assertThat(r.statut()).isEqualTo(200);
      assertThat(r.data().path("versionMinimaleApp").asText()).isEqualTo("2.0.0");
      assertThat(r.data().has("supportWhatsapp")).isFalse();
      assertThat(r.data().has("supportEmail")).isFalse();
    }
  }
}
