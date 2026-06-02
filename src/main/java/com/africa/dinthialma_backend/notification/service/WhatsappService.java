package com.africa.dinthialma_backend.notification.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service WhatsApp – OpenWA (wa-automate).
 *
 * <p>Canal principal de notifications : OTP, invitations tontine, rappels cotisation, jackpot. Mode
 * mock activé par défaut en développement via {@code openwa.mock=true}.
 */
@Slf4j
@Service
public class WhatsappService {

  @Value("${openwa.api-url}")
  private String apiUrl;

  @Value("${openwa.mock}")
  private boolean mock;

  private final RestClient restClient = RestClient.create();

  /**
   * Envoie un message WhatsApp via l'API OpenWA. En mode mock, affiche le message dans les logs.
   *
   * @param phone numéro sans le «+» (ex : 221781234567)
   * @param message contenu du message (supporte le formatage WhatsApp : *gras*, _italique_)
   */
  public void send(String phone, String message) {
    if (mock) {
      log.info("╔══════════════════════════════════════════╗");
      log.info("║      WHATSAPP MOCK (non envoyé)          ║");
      log.info("║  À      : {}", phone);
      log.info("║  Message: {}", message);
      log.info("╚══════════════════════════════════════════╝");
      return;
    }

    try {
      String chatId = phone + "@c.us";
      restClient
          .post()
          .uri(apiUrl + "/message/sendText/" + chatId)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("text", message))
          .retrieve()
          .toBodilessEntity();
      log.info("WhatsApp envoyé à {} – message : {}", phone, message);
    } catch (Exception e) {
      log.error("Échec envoi WhatsApp à {} : {}", phone, e.getMessage());
    }
  }

  /** Envoie un OTP par WhatsApp. */
  public void sendOtp(String phone, String otp) {
    String message =
        "🔐 *Dinthialma* – Votre code de vérification : *"
            + otp
            + "*\nValable 10 minutes. Ne le partagez pas.";
    send(phone, message);
  }

  /** Envoie un rappel de cotisation par WhatsApp. */
  public void sendContributionReminder(String phone, String tontineName, String amount) {
    String message =
        "💰 *Dinthialma* – Rappel : votre cotisation de "
            + amount
            + " pour la tontine *"
            + tontineName
            + "* est en attente. Connectez-vous sur l'application pour régulariser.";
    send(phone, message);
  }

  /** Informe un nouveau membre pré-inscrit de son ajout à une tontine. */
  public void sendTontineInvite(String phone, String tontineName) {
    String message =
        "👋 Vous avez été ajouté(e) à la tontine *"
            + tontineName
            + "* sur Dinthialma.\nInscrivez-vous sur l'application pour accéder à votre compte et suivre vos cotisations.";
    send(phone, message);
  }

  /** Notifie le bénéficiaire du prochain jackpot. */
  public void sendJackpotNotification(String phone, String tontineName, String amount) {
    String message =
        "🎉 *Félicitations !* Vous êtes le(la) bénéficiaire du prochain jackpot de la tontine *"
            + tontineName
            + "*.\nMontant prévu : *"
            + amount
            + "*.";
    send(phone, message);
  }
}
