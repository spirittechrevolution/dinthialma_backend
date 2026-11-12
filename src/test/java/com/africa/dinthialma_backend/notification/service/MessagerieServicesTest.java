package com.africa.dinthialma_backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * WhatsApp (open-wa) et SMS (LAfricaMobile) : contenu des messages et appel HTTP réel vers un faux
 * serveur local ; mode mock et panne réseau sans effet sur l'appelant.
 */
@DisplayName("Messagerie : WhatsappService et SmsService")
class MessagerieServicesTest {

  private HttpServer serveur;
  private final List<String> requetes = new ArrayList<>();
  private String base;

  @BeforeEach
  void demarrer() throws IOException {
    serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    serveur.createContext(
        "/",
        echange -> {
          String corps =
              new String(echange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          requetes.add(
              echange.getRequestMethod()
                  + " "
                  + URLDecoder.decode(echange.getRequestURI().toString(), StandardCharsets.UTF_8)
                  + " "
                  + corps);
          echange.sendResponseHeaders(200, -1);
          echange.close();
        });
    serveur.start();
    base = "http://127.0.0.1:" + serveur.getAddress().getPort();
  }

  @AfterEach
  void arreter() {
    serveur.stop(0);
  }

  @Nested
  @DisplayName("WhatsappService")
  class Whatsapp {

    private WhatsappService whatsapp(boolean mock, String url) {
      WhatsappService s = new WhatsappService();
      ReflectionTestUtils.setField(s, "apiUrl", url);
      ReflectionTestUtils.setField(s, "mock", mock);
      ReflectionTestUtils.setField(s, "appUrl", "https://app.test");
      return s;
    }

    @Test
    @DisplayName("envoi : POST /message/sendText/<tel>@c.us avec le texte JSON")
    void envoi() {
      whatsapp(false, base).sendOtp("221770000001", "123456");
      assertThat(requetes)
          .singleElement()
          .satisfies(
              r -> {
                assertThat(r).startsWith("POST /message/sendText/221770000001@c.us ");
                assertThat(r).contains("\"text\"").contains("*123456*").contains("10 minutes");
              });
    }

    @Test
    @DisplayName("chaque modèle de message porte ses données et le lien de l'application")
    void modeles() {
      WhatsappService s = whatsapp(false, base);
      s.sendContributionReminder("1", "Tontine A", "1000 FCFA");
      s.sendTontineInvite("2", "Tontine B");
      s.sendJackpotNotification("3", "Tontine C", "5000 FCFA");
      s.sendEvenementielleRappelAttente("4", "Tontine D", "Tabaski", 12, "700 FCFA");
      s.sendEvenementielleRappelPeriode("5", "Tontine E", "Korité", 9, "2100 FCFA");
      s.sendDistributionFinale("6", "Tontine F", "9000", "10000");
      s.sendDistributionFinaleAdmin("7", "Tontine G", "50000", 5);
      s.sendJackpotManuelAdmin("8", "Awa Diop", 3, "Tontine H");

      assertThat(requetes).hasSize(8);
      assertThat(requetes.get(0)).contains("1000 FCFA", "Tontine A", "https://app.test");
      assertThat(requetes.get(1)).contains("Tontine B");
      assertThat(requetes.get(2)).contains("5000 FCFA");
      assertThat(requetes.get(3)).contains("Tabaski", "J-12", "700 FCFA");
      assertThat(requetes.get(4)).contains("Korité", "2100 FCFA");
      assertThat(requetes.get(5)).contains("9000", "10000");
      assertThat(requetes.get(6)).contains("50000", "5");
      assertThat(requetes.get(7)).contains("Awa Diop", "Tontine H");
    }

    @Test
    @DisplayName("message urgent : formulation selon J-1, J-3, J-7 ou autre")
    void urgent() {
      WhatsappService s = whatsapp(false, base);
      s.sendEvenementielleUrgent("1", "T", "Tabaski", 1, "0");
      s.sendEvenementielleUrgent("1", "T", "Tabaski", 3, "0");
      s.sendEvenementielleUrgent("1", "T", "Tabaski", 7, "0");
      s.sendEvenementielleUrgent("1", "T", "Tabaski", 30, "0");
      assertThat(requetes.get(0)).contains("c'est demain");
      assertThat(requetes.get(1)).contains("3 jours");
      assertThat(requetes.get(2)).contains("7 jours");
      assertThat(requetes.get(3)).contains("30 jours");
    }

    @Test
    @DisplayName("mode mock : aucun appel ; serveur injoignable : aucune exception")
    void mockEtPanne() {
      whatsapp(true, base).send("221770000001", "x");
      assertThat(requetes).isEmpty();
      assertThatCode(() -> whatsapp(false, "http://127.0.0.1:1").send("221770000001", "x"))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("SmsService")
  class Sms {

    private SmsService sms(boolean mock, String url) {
      SmsService s = new SmsService();
      ReflectionTestUtils.setField(s, "apiUrl", url);
      ReflectionTestUtils.setField(s, "accountId", "compte");
      ReflectionTestUtils.setField(s, "password", "secret");
      ReflectionTestUtils.setField(s, "sender", "DINTHIALMA");
      ReflectionTestUtils.setField(s, "mock", mock);
      return s;
    }

    @Test
    @DisplayName("envoi : GET /push/send avec compte, destinataire, émetteur et texte encodé")
    void envoi() {
      sms(false, base).sendOtp("221770000001", "654321");
      assertThat(requetes)
          .singleElement()
          .satisfies(
              r ->
                  assertThat(r)
                      .startsWith("GET /push/send?")
                      .contains(
                          "account_id=compte",
                          "to=221770000001",
                          "DINTHIALMA",
                          "Votre code Dinthialma : 654321"));
    }

    @Test
    @DisplayName("modèles : rappel, invitation, jackpot")
    void modeles() {
      SmsService s = sms(false, base);
      s.sendContributionReminder("1", "Tontine A", "1000 FCFA");
      s.sendTontineInvite("2", "Tontine B");
      s.sendJackpotNotification("3", "Tontine C", "5000 FCFA");
      assertThat(requetes).hasSize(3);
      assertThat(requetes.get(0)).contains("1000 FCFA", "Tontine A");
      assertThat(requetes.get(1)).contains("Tontine B");
      assertThat(requetes.get(2)).contains("5000 FCFA", "Tontine C");
    }

    @Test
    @DisplayName("mode mock : aucun appel ; serveur injoignable : aucune exception")
    void mockEtPanne() {
      sms(true, base).send("1", "x");
      assertThat(requetes).isEmpty();
      assertThatCode(() -> sms(false, "http://127.0.0.1:1").send("1", "x"))
          .doesNotThrowAnyException();
    }
  }
}
