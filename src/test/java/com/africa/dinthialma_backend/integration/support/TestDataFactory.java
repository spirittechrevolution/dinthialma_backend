package com.africa.dinthialma_backend.integration.support;

import com.africa.dinthialma_backend.auth.repository.OtpVerificationRepository;
import com.africa.dinthialma_backend.common.constants.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Création de données de test en passant par les vrais endpoints (inscription OTP comprise) : les
 * scénarios reproduisent l'usage réel de l'application.
 */
@Component
public class TestDataFactory {

  /** Numéros fictifs uniques (+22176xxxxxxx), propres à l'exécution en cours. */
  private static final AtomicLong COMPTEUR =
      new AtomicLong(System.currentTimeMillis() % 9_000_000L + 1_000_000L);

  public static final String MOT_DE_PASSE = "Test@Integration2026";

  private final OtpVerificationRepository otpRepository;
  private final String superAdminPhone;
  private final String superAdminPassword;
  private ApiClient api;

  public TestDataFactory(
      OtpVerificationRepository otpRepository,
      @Value("${dinthialma.bootstrap.phone}") String superAdminPhone,
      @Value("${dinthialma.bootstrap.password}") String superAdminPassword) {
    this.otpRepository = otpRepository;
    this.superAdminPhone = superAdminPhone;
    this.superAdminPassword = superAdminPassword;
  }

  void utiliser(ApiClient api) {
    this.api = api;
  }

  /** Utilisateur connecté (jetons issus d'une vraie authentification Keycloak). */
  public record Utilisateur(
      String phone, String jeton, String refreshToken, String userId, String prenom) {

    /** Téléphone normalisé (sans « + »), tel que stocké et renvoyé par l'API. */
    public String phoneNormalise() {
      return phone.startsWith("+") ? phone.substring(1) : phone;
    }
  }

  // ─── Utilisateurs ────────────────────────────────────────────────────────

  public static String nouveauTelephone() {
    return "+22176" + COMPTEUR.incrementAndGet();
  }

  /** Code OTP en attente pour un numéro et un usage (lu en base : le SMS est simulé). */
  public String otp(String phoneInternational, String usage) {
    String phone = phoneInternational.replace("+", "");
    return otpRepository
        .findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(phone, usage)
        .orElseThrow(() -> new AssertionError("Aucun OTP " + usage + " pour " + phone))
        .getCode();
  }

  /** Étapes 1 et 2 de l'inscription (envoi + vérification de l'OTP). */
  public void verifierTelephone(String phone) {
    attendre(api.post("/v1/auth/register/send-otp", null, Map.of("phone", phone)), 200);
    attendre(
        api.post(
            "/v1/auth/register/verify-otp",
            null,
            Map.of("phone", phone, "code", otp(phone, Constants.OtpPurpose.REGISTRATION))),
        200);
  }

  /** Inscription complète avec connexion automatique (B13). */
  public Utilisateur inscrire(String prenom) {
    String phone = nouveauTelephone();
    verifierTelephone(phone);
    JsonNode d =
        attendre(
                api.post(
                    "/v1/auth/register/complete",
                    null,
                    Map.of(
                        "phone",
                        phone,
                        "firstName",
                        prenom,
                        "lastName",
                        "Test",
                        "password",
                        MOT_DE_PASSE,
                        "clientType",
                        "WEB",
                        "deviceInfo",
                        "Tests d'intégration")),
                201)
            .data();
    return new Utilisateur(
        phone,
        d.path("access_token").asText(),
        d.path("refresh_token").asText(),
        d.path("userId").asText(),
        prenom);
  }

  public Utilisateur inscrire() {
    return inscrire("Membre");
  }

  /** Super-admin créé par le bootstrap au démarrage. */
  public Utilisateur superAdmin() {
    return connecter(superAdminPhone, superAdminPassword, "WEB");
  }

  public Utilisateur connecter(String phone, String motDePasse, String clientType) {
    JsonNode d =
        attendre(
                api.post(
                    "/v1/auth/login",
                    null,
                    Map.of("username", phone, "password", motDePasse, "clientType", clientType)),
                200)
            .data();
    return new Utilisateur(
        phone, d.path("access_token").asText(), d.path("refresh_token").asText(), null, null);
  }

  // ─── Tontines ────────────────────────────────────────────────────────────

  /** Tontine ROTATIVE MANUEL (cycles ouverts à la main), ordre ROTATION, 1000 F / mois. */
  public JsonNode creerTontine(String jeton, String nom, Map<String, Object> surcharges) {
    Map<String, Object> corps = new HashMap<>();
    corps.put("tontineType", "ROTATIVE");
    corps.put("nom", nom);
    corps.put("montant", 1000);
    corps.put("frequence", "MENSUEL");
    corps.put("ordreBeneficiaire", "ROTATION");
    corps.put("modeCycle", "MANUEL");
    corps.put("dateDebut", LocalDate.now().toString());
    corps.put("nombreMembres", 5);
    corps.put("nombreGagnants", 1);
    corps.putAll(surcharges);
    return attendre(api.post("/v1/tontines", jeton, corps), 201).data();
  }

  public JsonNode creerTontine(String jeton, String nom) {
    return creerTontine(jeton, nom, Map.of());
  }

  public JsonNode ajouterMembre(String jetonGestionnaire, String tontineId, String phone) {
    return attendre(
            api.post(
                "/v1/tontines/" + tontineId + "/membres",
                jetonGestionnaire,
                Map.of("phone", phone)),
            201)
        .data();
  }

  public void activer(String jeton, String tontineId) {
    attendre(api.put("/v1/tontines/" + tontineId + "/activer", jeton, null), 200);
  }

  /** Ouvre un cycle MANUEL de 30 jours à partir d'aujourd'hui. */
  public JsonNode ouvrirCycle(String jeton, String tontineId) {
    LocalDate debut = LocalDate.now();
    return attendre(
            api.post(
                "/v1/tontines/" + tontineId + "/cycles",
                jeton,
                Map.of("dateDebut", debut.toString(), "dateFin", debut.plusDays(30).toString())),
            201)
        .data();
  }

  /** Déclaration d'un paiement par le membre (statut EN_ATTENTE). */
  public JsonNode declarer(String jetonMembre, String tontineId, String cycleId, int montant) {
    return attendre(
            api.post(
                "/v1/tontines/" + tontineId + "/cotisations",
                jetonMembre,
                Map.of(
                    "cycleId",
                    cycleId,
                    "montant",
                    montant,
                    "methodePaiement",
                    "WAVE",
                    "referenceTransaction",
                    "REF-" + COMPTEUR.incrementAndGet())),
            201)
        .data();
  }

  /**
   * Scénario prêt à l'emploi : un gestionnaire, une tontine MANUEL active avec un membre et un
   * cycle ouvert.
   */
  public record TontineEnCours(
      Utilisateur gestionnaire,
      Utilisateur membre,
      String tontineId,
      String membreId,
      String cycleId) {}

  public TontineEnCours tontineEnCours(String nom) {
    Utilisateur gestionnaire = inscrire("Gestionnaire");
    Utilisateur membre = inscrire("Membre");
    String tontineId = creerTontine(gestionnaire.jeton(), nom).path("id").asText();
    String membreId =
        ajouterMembre(gestionnaire.jeton(), tontineId, membre.phone()).path("id").asText();
    activer(gestionnaire.jeton(), tontineId);
    String cycleId = ouvrirCycle(gestionnaire.jeton(), tontineId).path("id").asText();
    return new TontineEnCours(gestionnaire, membre, tontineId, membreId, cycleId);
  }

  // ─── Assertions ──────────────────────────────────────────────────────────

  /** Vérifie le statut attendu, sinon échoue avec le corps de la réponse. */
  public static ApiClient.Reponse attendre(ApiClient.Reponse reponse, int statut) {
    if (reponse.statut() != statut) {
      throw new AssertionError("Statut " + statut + " attendu, reçu : " + reponse);
    }
    return reponse;
  }
}
