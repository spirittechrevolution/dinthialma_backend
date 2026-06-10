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

  /**
   * Rappel cotisation EN_ATTENTE pour une tontine EVENEMENTIELLE.
   *
   * <p>Envoyé quotidiennement aux membres dont la cotisation est en attente de validation par
   * l'admin.
   */
  public void sendEvenementielleRappelAttente(
      String phone, String tontineName, String nomEvenement, long joursRestants, String amount) {
    String message =
        "💰 *Dinthialma* – Rappel cotisation\n"
            + "Votre versement de *"
            + amount
            + "* pour la tontine *"
            + tontineName
            + "* est en attente de validation.\n"
            + "📅 *"
            + nomEvenement
            + "* dans *J-"
            + joursRestants
            + "*\nContactez votre gestionnaire si besoin.";
    send(phone, message);
  }

  /**
   * Rappel de période non cotisée pour une tontine EVENEMENTIELLE.
   *
   * <p>Envoyé quotidiennement (hors jours clés) aux membres qui n'ont pas encore cotisé sur le
   * sous-cycle courant.
   */
  public void sendEvenementielleRappelPeriode(
      String phone,
      String tontineName,
      String nomEvenement,
      long joursRestants,
      String montantTotal) {
    String message =
        "⏰ *Dinthialma* – La tontine *"
            + tontineName
            + "* vous attend !\n"
            + "Vous n'avez pas encore cotisé pour cette période.\n"
            + "📅 *"
            + nomEvenement
            + "* dans *J-"
            + joursRestants
            + "*\n"
            + "💵 Votre épargne totale à ce jour : *"
            + montantTotal
            + "*\nOuvrez l'application pour enregistrer votre cotisation.";
    send(phone, message);
  }

  /**
   * Rappel urgent J-30 / J-7 / J-3 / J-1 pour une tontine EVENEMENTIELLE.
   *
   * <p>Envoyé à tous les membres actifs aux jalons clés avant la date de l'événement.
   */
  public void sendEvenementielleUrgent(
      String phone,
      String tontineName,
      String nomEvenement,
      long joursRestants,
      String montantTotal) {
    String emoji;
    String intro;
    String suffix;
    if (joursRestants == 1) {
      emoji = "🔔";
      intro = "*" + nomEvenement + "* c'est demain !";
      suffix = "La clôture et la distribution ont lieu demain. Bonne célébration ! 🎉";
    } else if (joursRestants == 3) {
      emoji = "🚨";
      intro = "*" + nomEvenement + "* dans *3 jours* !";
      suffix = "Dernière chance de cotiser avant la clôture et la distribution. Bon courage ! 💪";
    } else if (joursRestants == 7) {
      emoji = "⚠️";
      intro = "Plus que *7 jours* avant *" + nomEvenement + "* !";
      suffix = "Pensez à enregistrer votre cotisation si ce n'est pas encore fait 🙏";
    } else {
      emoji = "📢";
      intro = "Dans *" + joursRestants + " jours* : *" + nomEvenement + "* !";
      suffix = "Continuez à cotiser régulièrement pour atteindre votre objectif 💪";
    }
    String message =
        emoji
            + " *Dinthialma* – "
            + intro
            + "\nTontine : *"
            + tontineName
            + "*\n"
            + "💵 Votre épargne à ce jour : *"
            + montantTotal
            + "*\n"
            + suffix;
    send(phone, message);
  }
}
