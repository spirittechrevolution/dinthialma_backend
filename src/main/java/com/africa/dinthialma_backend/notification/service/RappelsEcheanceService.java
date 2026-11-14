package com.africa.dinthialma_backend.notification.service;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.util.Fuseau;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.CanalRappel;
import com.africa.dinthialma_backend.notification.codeList.NatureRappel;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.dto.NotificationLiens;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.EcheanceCycle;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.EcheanceCycleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rappels liés aux échéances de cotisation (B26), toutes les 5 minutes, heure de Dakar.
 *
 * <ul>
 *   <li>la veille de l'échéance à 18h00, puis N heures avant : aux membres actifs qui n'ont pas
 *       encore déclaré (déclaration refusée = non déclarée) ;
 *   <li>une fois l'échéance passée (dans les 24 h) : {@code COTISATIONS_MANQUANTES} au gestionnaire
 *       s'il reste des membres sans déclaration ; le statut des cotisations ne change pas (S01).
 * </ul>
 *
 * <p>Chaque rappel n'est envoyé qu'une fois par échéance, destinataire et canal ({@link
 * JournalRappels}). WhatsApp n'est pas envoyé entre 21h00 et 8h00 : il part au premier passage
 * après 8h00 si la fenêtre du rappel est encore ouverte. Les échéances sont lues par échéance (et
 * non par « tour en cours ») pour rester valables quand un tour en aura plusieurs (B32).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RappelsEcheanceService {

  /** Heure du rappel de la veille. */
  static final LocalTime HEURE_VEILLE = LocalTime.of(18, 0);

  /** Heures calmes WhatsApp : pas d'envoi avant 8h00 ni à partir de 21h00. */
  static final LocalTime DEBUT_WHATSAPP = LocalTime.of(8, 0);

  static final LocalTime FIN_WHATSAPP = LocalTime.of(21, 0);

  /** Durée pendant laquelle les cotisations manquantes sont signalées après l'échéance. */
  static final long HEURES_SIGNALEMENT = 24;

  /** Le rappel le plus précoce part la veille à 18h00, au plus 30 h avant l'échéance. */
  private static final long HEURES_AVANT_MAXIMUM = 30;

  private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH'h'mm");

  private final EcheanceCycleRepository echeanceRepository;
  private final TontineMembreRepository membreRepository;
  private final CotisationRepository cotisationRepository;
  private final NotificationService notificationService;
  private final WhatsappService whatsappService;
  private final JournalRappels journal;

  @Scheduled(cron = "0 */5 * * * *", zone = Fuseau.NOM)
  @Transactional
  public void planifie() {
    traiter(Fuseau.maintenant());
  }

  /** Un passage du traitement, à l'instant {@code maintenant} (heure de Dakar). */
  @Transactional
  public void traiter(LocalDateTime maintenant) {
    List<EcheanceCycle> echeances =
        echeanceRepository.findActivesEntre(
            maintenant.minusHours(HEURES_SIGNALEMENT),
            maintenant.plusHours(HEURES_AVANT_MAXIMUM),
            CycleStatut.EN_COURS,
            TontineStatut.ACTIVE);
    for (EcheanceCycle echeance : echeances) {
      try {
        traiterEcheance(echeance, maintenant);
      } catch (RuntimeException e) {
        log.error("Rappels de l'échéance {} en échec : {}", echeance.getId(), e.getMessage());
      }
    }
  }

  private void traiterEcheance(EcheanceCycle echeance, LocalDateTime maintenant) {
    LocalDateTime limite = echeance.getEcheanceAt();
    Tontine tontine = echeance.getCycle().getTontine();
    boolean avant = maintenant.isBefore(limite);
    boolean heuresAvant =
        avant
            && tontine.getRappelHeuresAvant() > 0
            && !maintenant.isBefore(limite.minusHours(tontine.getRappelHeuresAvant()));
    // Le rappel « N heures avant » remplace celui de la veille s'il n'est pas encore parti : jamais
    // deux messages presque identiques au même passage.
    boolean veille =
        avant
            && !heuresAvant
            && tontine.isRappelVeille()
            && !maintenant.isBefore(limite.toLocalDate().minusDays(1).atTime(HEURE_VEILLE));
    boolean manquantes = !avant && maintenant.isBefore(limite.plusHours(HEURES_SIGNALEMENT));
    if (!veille && !heuresAvant && !manquantes) {
      return;
    }

    List<TontineMembre> sansDeclaration = sansDeclaration(echeance.getCycle());
    if (sansDeclaration.isEmpty()) {
      return;
    }
    if (veille) {
      rappelerMembres(echeance, sansDeclaration, NatureRappel.VEILLE, maintenant);
    }
    if (heuresAvant) {
      rappelerMembres(echeance, sansDeclaration, NatureRappel.HEURES_AVANT, maintenant);
    }
    if (manquantes) {
      signalerAuGestionnaire(echeance, sansDeclaration);
    }
  }

  /** Membres actifs sans déclaration retenue (aucune, ou seulement refusée) sur le tour. */
  private List<TontineMembre> sansDeclaration(CycleTontine cycle) {
    Set<UUID> declarants =
        cotisationRepository.findByCycle_IdAndDeletedAtIsNull(cycle.getId()).stream()
            .filter(c -> c.getStatut() != CotisationStatut.REFUSE)
            .map(c -> c.getMembre().getId())
            .collect(Collectors.toSet());
    return membreRepository
        .findByTontine_IdAndStatutAndDeletedAtIsNull(cycle.getTontine().getId(), MembreStatut.ACTIF)
        .stream()
        .filter(m -> !declarants.contains(m.getId()))
        .toList();
  }

  // ─── Rappels aux membres ─────────────────────────────────────────────────

  private void rappelerMembres(
      EcheanceCycle echeance,
      List<TontineMembre> membres,
      NatureRappel nature,
      LocalDateTime maintenant) {
    String message = messageRappel(echeance, nature);
    boolean whatsappAutorise = whatsappAutorise(maintenant);
    for (TontineMembre membre : membres) {
      User user = membre.getUser();
      if (journal.reserver(echeance.getId(), user.getId(), nature, CanalRappel.IN_APP)) {
        notificationService.notify(
            user.getId(),
            NotificationType.RAPPEL_COTISATION,
            "Rappel de cotisation",
            message,
            echeance.getCycle().getTontine().getId(),
            NotificationLiens.cycle(echeance.getCycle().getId(), membre.getId()));
      }
      if (whatsappAutorise
          && user.getPhone() != null
          && journal.reserver(echeance.getId(), user.getId(), nature, CanalRappel.WHATSAPP)) {
        whatsappService.send(user.getPhone(), message);
      }
    }
  }

  private static String messageRappel(EcheanceCycle echeance, NatureRappel nature) {
    Tontine tontine = echeance.getCycle().getTontine();
    String heure = echeance.getEcheanceAt().format(FORMAT_HEURE);
    BigDecimal attendu = tontine.getMontantAttenduParMembre();
    String cotisation =
        attendu != null && attendu.signum() > 0
            ? "votre cotisation de " + francs(attendu)
            : "votre cotisation";
    if (nature == NatureRappel.VEILLE) {
      return "Rappel : "
          + cotisation
          + " pour "
          + tontine.getNom()
          + " est attendue demain avant "
          + heure
          + ".";
    }
    return "Plus que "
        + tontine.getRappelHeuresAvant()
        + " h : "
        + cotisation
        + " pour "
        + tontine.getNom()
        + " est attendue avant "
        + heure
        + ".";
  }

  // ─── Cotisations manquantes (gestionnaire) ───────────────────────────────

  private void signalerAuGestionnaire(EcheanceCycle echeance, List<TontineMembre> sansDeclaration) {
    CycleTontine cycle = echeance.getCycle();
    Tontine tontine = cycle.getTontine();
    User gestionnaire = tontine.getCreePar();
    if (gestionnaire == null
        || !journal.reserver(
            echeance.getId(),
            gestionnaire.getId(),
            NatureRappel.COTISATIONS_MANQUANTES,
            CanalRappel.IN_APP)) {
      return;
    }
    notificationService.notify(
        gestionnaire.getId(),
        NotificationType.COTISATIONS_MANQUANTES,
        "Cotisations manquantes",
        messageManquantes(cycle, sansDeclaration),
        tontine.getId(),
        NotificationLiens.cycle(cycle.getId(), null));
  }

  private static String messageManquantes(CycleTontine cycle, List<TontineMembre> sansDeclaration) {
    int n = sansDeclaration.size();
    String noms =
        sansDeclaration.stream()
            .limit(5)
            .map(m -> nomCourt(m.getUser()))
            .collect(Collectors.joining(", "));
    if (n > 5) {
      noms += "…";
    }
    return "Échéance du tour n°"
        + cycle.getNumeroCycle()
        + " de "
        + cycle.getTontine().getNom()
        + " passée : "
        + n
        + (n > 1 ? " membres n'ont" : " membre n'a")
        + " pas déclaré sa cotisation ("
        + noms
        + "). Le tour reste ouvert : à vous de relancer, d'attendre ou de clôturer.";
  }

  // ─── Utilitaires ─────────────────────────────────────────────────────────

  /** WhatsApp autorisé de 8h00 (inclus) à 21h00 (exclu), heure de Dakar. */
  public static boolean whatsappAutorise(LocalDateTime maintenant) {
    LocalTime heure = maintenant.toLocalTime();
    return !heure.isBefore(DEBUT_WHATSAPP) && heure.isBefore(FIN_WHATSAPP);
  }

  private static String nomCourt(User user) {
    String prenom = user.getFirstName() != null ? user.getFirstName() : "";
    String nom =
        user.getLastName() != null && !user.getLastName().isBlank()
            ? " " + user.getLastName().charAt(0) + "."
            : "";
    return (prenom + nom).isBlank() ? user.getPhone() : (prenom + nom).trim();
  }

  /** Montant en francs avec séparateur de milliers, ex. « 5 000 F ». */
  public static String francs(BigDecimal montant) {
    return String.format(
            Locale.FRANCE, "%,d F", montant.setScale(0, RoundingMode.HALF_UP).longValue())
        .replace(' ', ' ')
        .replace(' ', ' ');
  }
}
