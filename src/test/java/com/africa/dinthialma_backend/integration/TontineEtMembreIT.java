package com.africa.dinthialma_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.africa.dinthialma_backend.support.AbstractIntegrationTest;
import com.africa.dinthialma_backend.support.TestDataFactory.TontineEnCours;
import com.africa.dinthialma_backend.support.TestDataFactory.Utilisateur;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Relation de l'appelant avec une tontine (B01), recherche et filtres serveur (B11), membres à
 * encaisser (aPaye), jackpots à venir (B10).
 */
@DisplayName("Tontines, membres et jackpots")
class TontineEtMembreIT extends AbstractIntegrationTest {

  private static List<String> noms(JsonNode page) {
    return StreamSupport.stream(page.path("content").spliterator(), false)
        .map(x -> x.path("nom").asText())
        .toList();
  }

  @Test
  @DisplayName("rythme libre (B26) : création, simulation, échéances des tours, refus")
  void rythmeEtEcheances() {
    Utilisateur g = donnees.inscrire();
    Utilisateur m = donnees.inscrire();
    LocalDate mercredi = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
    LocalDate dimanche = mercredi.plusDays(4);
    Map<String, Object> rythme = new HashMap<>();
    rythme.put("frequence", null);
    rythme.put("rythmeIntervalle", 1);
    rythme.put("rythmeUnite", "SEMAINE");
    rythme.put("jourEcheance", "DIMANCHE");
    rythme.put("heureEcheance", "20:00");
    rythme.put("rappels", Map.of("veille", false, "heuresAvant", 2));
    rythme.put("modeCycle", "AUTOMATIQUE");
    rythme.put("dateDebut", mercredi.toString());

    JsonNode tontine = donnees.creerTontine(g.jeton(), "Hebdo", rythme);
    assertThat(tontine.path("frequence").asText()).isEqualTo("HEBDOMADAIRE");
    assertThat(tontine.path("jourEcheance").asText()).isEqualTo("DIMANCHE");
    assertThat(tontine.path("heureEcheance").asText()).isEqualTo("20:00");
    assertThat(tontine.path("rappels").path("heuresAvant").asInt()).isEqualTo(2);
    String tid = tontine.path("id").asText();

    JsonNode simulation =
        api.get(
                "/v1/tontines/simulation?rythmeIntervalle=1&rythmeUnite=SEMAINE"
                    + "&jourEcheance=DIMANCHE&heureEcheance=20:00&nombreMembres=2&dateDebut="
                    + mercredi,
                g.jeton())
            .data();
    assertThat(simulation.path("nombreTours").asInt()).isEqualTo(2);
    assertThat(simulation.path("premiereEcheance").asText()).isEqualTo(dimanche + "T20:00:00Z");

    donnees.ajouterMembre(g.jeton(), tid, m.phone());
    donnees.ajouterMembre(g.jeton(), tid, donnees.inscrire().phone());
    donnees.activer(g.jeton(), tid);
    JsonNode tours = api.get("/v1/tontines/" + tid + "/cycles", g.jeton()).data().path("content");
    assertThat(tours).hasSize(2);
    assertThat(tours.get(0).path("echeanceAt").asText()).isEqualTo(dimanche + "T20:00:00Z");
    assertThat(tours.get(1).path("echeanceAt").asText())
        .isEqualTo(dimanche.plusWeeks(1) + "T20:00:00Z");

    var nonModifiable = api.put("/v1/tontines/" + tid, g.jeton(), Map.of("rythmeIntervalle", 2));
    assertThat(nonModifiable.code()).isEqualTo("RYTHME_NON_MODIFIABLE");

    Map<String, Object> invalide = new HashMap<>(rythme);
    invalide.put("rythmeUnite", "JOUR");
    var refus = api.post("/v1/tontines", g.jeton(), corpsTontine("Invalide", invalide));
    assertThat(refus.statut()).isEqualTo(400);
    assertThat(refus.code()).isEqualTo("JOUR_ECHEANCE_INVALIDE");
  }

  /** Corps complet d'une tontine rotative (les surcharges priment). */
  private static Map<String, Object> corpsTontine(String nom, Map<String, Object> surcharges) {
    Map<String, Object> corps = new HashMap<>();
    corps.put("tontineType", "ROTATIVE");
    corps.put("nom", nom);
    corps.put("montant", 1000);
    corps.put("ordreBeneficiaire", "ROTATION");
    corps.put("nombreMembres", 5);
    corps.putAll(surcharges);
    return corps;
  }

  @Test
  @DisplayName("commissions à la création : enregistrées avec la tontine, refus = rien créé (B30)")
  void commissionsALaCreation() {
    Utilisateur g = donnees.inscrire();
    String tid =
        donnees
            .creerTontine(
                g.jeton(),
                "Avec commissions",
                Map.of(
                    "commissions",
                    List.of(
                        Map.of("type", "POURCENTAGE_JACKPOT", "valeur", 5),
                        Map.of("type", "FRAIS_ADHESION", "valeur", 1000, "description", "Carnet"))))
            .path("id")
            .asText();
    JsonNode commissions = api.get("/v1/tontines/" + tid + "/commissions", g.jeton()).data();
    assertThat(commissions.path("content")).hasSize(2);

    Map<String, Object> base =
        Map.of(
            "tontineType", "ROTATIVE",
            "nom", "Refusée",
            "montant", 1000,
            "frequence", "MENSUEL",
            "ordreBeneficiaire", "ROTATION",
            "modeCycle", "MANUEL",
            "dateDebut", LocalDate.now().toString(),
            "nombreMembres", 5);
    Map<String, Object> doublon = new HashMap<>(base);
    doublon.put(
        "commissions",
        List.of(
            Map.of("type", "FRAIS_ADHESION", "valeur", 500),
            Map.of("type", "FRAIS_ADHESION", "valeur", 800)));
    var reponse = api.post("/v1/tontines", g.jeton(), doublon);
    assertThat(reponse.statut()).isEqualTo(409);
    assertThat(reponse.code()).isEqualTo("COMMISSION_ALREADY_EXISTS");

    Map<String, Object> tropEleve = new HashMap<>(base);
    tropEleve.put("commissions", List.of(Map.of("type", "POURCENTAGE_JACKPOT", "valeur", 120)));
    assertThat(api.post("/v1/tontines", g.jeton(), tropEleve).code())
        .isEqualTo("COMMISSION_PERCENTAGE_TOO_HIGH");

    Map<String, Object> negative = new HashMap<>(base);
    negative.put("commissions", List.of(Map.of("type", "FRAIS_ADHESION", "valeur", -1)));
    assertThat(api.post("/v1/tontines", g.jeton(), negative).code()).isEqualTo("VALIDATION_ERROR");

    assertThat(noms(api.get("/v1/tontines?role=GERE", g.jeton()).data()))
        .containsExactly("Avec commissions");
  }

  @Test
  @DisplayName("monRole, monMembreId et filtre role=GERE|COTISE (B01)")
  void relationAvecLaTontine() {
    TontineEnCours t = donnees.tontineEnCours("Relation");
    JsonNode vueGestionnaire =
        api.get("/v1/tontines/" + t.tontineId(), t.gestionnaire().jeton()).data();
    assertThat(vueGestionnaire.path("monRole").asText()).isEqualTo("GESTIONNAIRE");
    assertThat(vueGestionnaire.has("monMembreId")).isFalse();

    JsonNode vueMembre = api.get("/v1/tontines/" + t.tontineId(), t.membre().jeton()).data();
    assertThat(vueMembre.path("monRole").asText()).isEqualTo("MEMBRE");
    assertThat(vueMembre.path("monMembreId").asText()).isEqualTo(t.membreId());
    assertThat(vueMembre.path("monStatutMembre").asText()).isEqualTo("ACTIF");

    assertThat(noms(api.get("/v1/tontines?role=GERE", t.gestionnaire().jeton()).data()))
        .containsExactly("Relation");
    assertThat(noms(api.get("/v1/tontines?role=COTISE", t.gestionnaire().jeton()).data()))
        .isEmpty();
    assertThat(noms(api.get("/v1/tontines?role=COTISE", t.membre().jeton()).data()))
        .containsExactly("Relation");
    assertThat(
            api.get("/v1/tontines/" + t.tontineId(), donnees.superAdmin().jeton())
                .data()
                .path("monRole")
                .asText())
        .isEqualTo("LECTURE");
  }

  @Test
  @DisplayName("recherche q (casse, jokers échappés) et statut sur les tontines (B11)")
  void rechercheTontines() {
    Utilisateur g = donnees.inscrire();
    String suffixe = UUID.randomUUID().toString().substring(0, 6);
    donnees.creerTontine(g.jeton(), "Famille " + suffixe);
    donnees.creerTontine(g.jeton(), "Bureau 100 " + suffixe);

    assertThat(noms(api.get("/v1/tontines?q=FAMILLE", g.jeton()).data()))
        .containsExactly("Famille " + suffixe);
    assertThat(
            noms(
                api.get(
                        "/v1/tontines?q=" + URLEncoder.encode("100%", StandardCharsets.UTF_8),
                        g.jeton())
                    .data()))
        .isEmpty();
    assertThat(noms(api.get("/v1/tontines?statut=ACTIVE", g.jeton()).data())).isEmpty();
    assertThat(api.get("/v1/tontines?statut=FOO", g.jeton()).code()).isEqualTo("INVALID_PARAMETER");
  }

  @Test
  @DisplayName("membres : recherche, aPaye sur le tour, cycle d'une autre tontine -> 404 (B11)")
  void filtresMembres() {
    TontineEnCours t = donnees.tontineEnCours("Encaisser");
    String g = t.gestionnaire().jeton();
    String membres = "/v1/tontines/" + t.tontineId() + "/membres";

    assertThat(api.get(membres + "?q=MEMBRE", g).data().path("content")).hasSize(1);
    assertThat(
            api.get(membres + "?cycleId=" + t.cycleId() + "&aPaye=false", g).data().path("content"))
        .hasSize(1);
    assertThat(
            api.get(membres + "?cycleId=" + t.cycleId() + "&aPaye=true", g).data().path("content"))
        .isEmpty();

    donnees.declarer(t.membre().jeton(), t.tontineId(), t.cycleId(), 1000);
    assertThat(
            api.get(membres + "?cycleId=" + t.cycleId() + "&aPaye=true", g).data().path("content"))
        .hasSize(1);
    assertThat(api.get(membres + "?cycleId=" + UUID.randomUUID() + "&aPaye=false", g).code())
        .isEqualTo("NOT_FOUND");
  }

  @Test
  @DisplayName("console : recherche d'utilisateurs réservée au SUPER_ADMIN (B11)")
  void consoleUtilisateurs() {
    Utilisateur u = donnees.inscrire("Introuvable" + UUID.randomUUID().toString().substring(0, 4));
    Utilisateur superAdmin = donnees.superAdmin();
    JsonNode trouves =
        api.get("/v1/admin/dashboard/users?q=" + u.phoneNormalise(), superAdmin.jeton())
            .data()
            .path("content");
    assertThat(trouves).hasSize(1);
    assertThat(trouves.get(0).path("firstName").asText()).isEqualTo(u.prenom());
    assertThat(api.get("/v1/admin/dashboard/users?q=x", u.jeton()).statut()).isEqualTo(403);
  }

  @Test
  @DisplayName("jackpots à venir : rotation estimée, choix manuel sans estimation (B10)")
  void jackpotsAVenir() {
    Utilisateur g = donnees.inscrire();
    Utilisateur m = donnees.inscrire();
    String rotation =
        donnees
            .creerTontine(
                g.jeton(),
                "Rotation",
                Map.of(
                    "modeCycle",
                    "AUTOMATIQUE",
                    "ordreBeneficiaire",
                    "ROTATION",
                    "montant",
                    2000,
                    "dateDebut",
                    LocalDate.now().toString()))
            .path("id")
            .asText();
    donnees.ajouterMembre(g.jeton(), rotation, m.phone());
    donnees.activer(g.jeton(), rotation);
    String manuel =
        donnees
            .creerTontine(g.jeton(), "Manuel", Map.of("ordreBeneficiaire", "MANUEL"))
            .path("id")
            .asText();
    donnees.ajouterMembre(g.jeton(), manuel, m.phone());
    donnees.activer(g.jeton(), manuel);

    JsonNode jackpots = api.get("/v1/me/jackpots", m.jeton()).data();
    assertThat(jackpots.has("aVenir")).isTrue();
    assertThat(jackpots.path("recus")).isEmpty();
    JsonNode r = trouver(jackpots.path("aVenir"), rotation);
    assertThat(r.path("modeAttribution").asText()).isEqualTo("ORDRE");
    assertThat(r.path("position").asInt()).isEqualTo(1);
    assertThat(r.path("numeroCycleEstime").asInt()).isEqualTo(1);
    assertThat(r.path("dateEstimee").asText()).isNotBlank();
    assertThat(r.path("montantEstime").asDouble()).isEqualTo(2000);
    JsonNode c = trouver(jackpots.path("aVenir"), manuel);
    assertThat(c.path("modeAttribution").asText()).isEqualTo("CHOIX");
    assertThat(c.has("dateEstimee")).isFalse();
  }

  private static JsonNode trouver(JsonNode liste, String tontineId) {
    return StreamSupport.stream(liste.spliterator(), false)
        .filter(x -> x.path("tontineId").asText().equals(tontineId))
        .findFirst()
        .orElseThrow();
  }
}
