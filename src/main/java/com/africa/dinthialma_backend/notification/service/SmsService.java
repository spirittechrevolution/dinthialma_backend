package com.africa.dinthialma_backend.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service SMS – LAfricaMobile (LAMPUSH).
 *
 * <p>Utilisé pour l'envoi des OTP d'inscription, reset mot de passe et notifications tontine.
 */
@Slf4j
@Service
public class SmsService {

  @Value("${lam.sms.api-url}")
  private String apiUrl;

  @Value("${lam.sms.account-id}")
  private String accountId;

  @Value("${lam.sms.password}")
  private String password;

  @Value("${lam.sms.sender}")
  private String sender;

  /**
   * Mode mock – si {@code true}, le SMS n'est pas envoyé : le contenu est simplement affiché dans
   * les logs (niveau INFO). À activer en développement via {@code lam.sms.mock=true}.
   */
  @Value("${lam.sms.mock:false}")
  private boolean mock;

  private final RestClient restClient = RestClient.create();

  /**
   * Envoie un SMS via LAfricaMobile. En mode mock ({@code lam.sms.mock=true}), affiche le message
   * dans les logs.
   *
   * @param to numéro de téléphone destinataire (format international, ex: +221700000000)
   * @param message contenu du SMS (max 160 caractères pour 1 SMS)
   */
  public void send(String to, String message) {
    if (mock) {
      log.info("╔══════════════════════════════════════════╗");
      log.info("║            SMS MOCK (non envoyé)         ║");
      log.info("║  À      : {}", to);
      log.info("║  Message: {}", message);
      log.info("╚══════════════════════════════════════════╝");
      return;
    }

    try {
      String url =
          apiUrl
              + "/push/send"
              + "?account_id="
              + accountId
              + "&password="
              + password
              + "&to="
              + to
              + "&from="
              + sender
              + "&msg="
              + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);

      restClient.get().uri(url).retrieve().toBodilessEntity();
      log.info("SMS envoyé à {} – message : {}", to, message);

    } catch (Exception e) {
      // SMS non bloquant – on log l'erreur sans la propager
      log.error("Échec envoi SMS à {} : {}", to, e.getMessage());
    }
  }

  /** Envoie un OTP par SMS. */
  public void sendOtp(String phone, String otp) {
    String message = "Votre code Dinthialma : " + otp + ". Valable 10 minutes.";
    send(phone, message);
  }

  /** Envoie un rappel de cotisation. */
  public void sendContributionReminder(String phone, String tontineName, String amount) {
    String message =
        "Rappel Dinthialma : votre cotisation de "
            + amount
            + " pour la tontine "
            + tontineName
            + " est due. Merci de régler rapidement.";
    send(phone, message);
  }

  /** Informe un nouveau membre pré-inscrit qu'il a été ajouté à une tontine. */
  public void sendTontineInvite(String phone, String tontineName) {
    String message =
        "Vous avez été ajouté à la tontine \""
            + tontineName
            + "\" sur Dinthialma. Inscrivez-vous sur l'application pour suivre vos cotisations.";
    send(phone, message);
  }

  /** Notifie le bénéficiaire du prochain jackpot. */
  public void sendJackpotNotification(String phone, String tontineName, String amount) {
    String message =
        "Félicitations ! Vous êtes le prochain bénéficiaire de la tontine "
            + tontineName
            + ". Montant prévu : "
            + amount
            + ".";
    send(phone, message);
  }
}
