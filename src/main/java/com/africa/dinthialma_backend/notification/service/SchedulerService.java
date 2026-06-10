package com.africa.dinthialma_backend.notification.service;

import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.CycleGagnant;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleGagnantRepository;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Planificateur de notifications WhatsApp liées aux tontines.
 *
 * <ul>
 *   <li>Rappel quotidien à 8h → cotisations EN_ATTENTE sur cycles EN_COURS
 *   <li>EVENEMENTIELLE → rappels personnalisés avec compte à rebours J-X vers dateEcheance
 *   <li>Annonce gagnants → déclenché par {@code CycleServiceImpl} à la clôture d'un cycle
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

  private final WhatsappService whatsappService;
  private final CycleTontineRepository cycleRepository;
  private final CotisationRepository cotisationRepository;
  private final CycleGagnantRepository cycleGagnantRepository;
  private final TontineMembreRepository membreRepository;
  private final NotificationService notificationService;

  // ─── Rappel quotidien ─────────────────────────────────────────────────────

  /**
   * Rappel quotidien 8h00 pour tous les cycles EN_COURS.
   *
   * <ul>
   *   <li>ROTATIVE : rappel générique pour chaque cotisation EN_ATTENTE
   *   <li>EVENEMENTIELLE : messages personnalisés avec compte à rebours J-X
   * </ul>
   */
  @Scheduled(cron = "0 0 8 * * *")
  @Transactional(readOnly = true)
  public void rappelCotisationsEnAttente() {
    List<CycleTontine> cyclesEnCours =
        cycleRepository.findByStatutAndDeletedAtIsNull(CycleStatut.EN_COURS);
    log.info("Rappel cotisations – {} cycle(s) EN_COURS", cyclesEnCours.size());

    for (CycleTontine cycle : cyclesEnCours) {
      Tontine tontine = cycle.getTontine();
      if (tontine.getTontineType() == TontineType.EVENEMENTIELLE) {
        processEvenementielleReminders(cycle, tontine);
      } else {
        cotisationRepository
            .findByCycle_IdAndStatutAndDeletedAtIsNull(cycle.getId(), CotisationStatut.EN_ATTENTE)
            .forEach(
                cotisation -> {
                  String phone = cotisation.getMembre().getUser().getPhone();
                  String amount = cotisation.getMontant().toPlainString() + " FCFA";
                  whatsappService.sendContributionReminder(phone, tontine.getNom(), amount);
                  notificationService.notify(
                      cotisation.getMembre().getUser().getId(),
                      NotificationType.RAPPEL_COTISATION,
                      "Rappel cotisation",
                      "Votre cotisation de "
                          + amount
                          + " pour "
                          + tontine.getNom()
                          + " est en attente de validation.",
                      tontine.getId());
                });
      }
    }
  }

  /**
   * Traite les rappels personnalisés pour une tontine EVENEMENTIELLE.
   *
   * <p>Stratégie par jour :
   *
   * <ul>
   *   <li>Tous les jours : rappel aux membres avec cotisation EN_ATTENTE (validation admin pending)
   *   <li>J-30, J-7, J-3, J-1 : notification urgente à TOUS les membres actifs
   *   <li>Autres jours : rappel aux membres qui n'ont pas encore cotisé dans le sous-cycle courant
   * </ul>
   */
  private void processEvenementielleReminders(CycleTontine cycle, Tontine tontine) {
    if (tontine.getDateEcheance() == null) return;

    long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), tontine.getDateEcheance());
    if (joursRestants < 0) return;

    String nomEvenement =
        tontine.getNomEvenement() != null ? tontine.getNomEvenement() : tontine.getNom();
    String tontineName = tontine.getNom();

    // Toujours : rappel pour cotisations EN_ATTENTE (validation admin pendante)
    cotisationRepository
        .findByCycle_IdAndStatutAndDeletedAtIsNull(cycle.getId(), CotisationStatut.EN_ATTENTE)
        .forEach(
            cot -> {
              String amount = cot.getMontant().toPlainString() + " FCFA";
              whatsappService.sendEvenementielleRappelAttente(
                  cot.getMembre().getUser().getPhone(),
                  tontineName,
                  nomEvenement,
                  joursRestants,
                  amount);
              notificationService.notify(
                  cot.getMembre().getUser().getId(),
                  NotificationType.RAPPEL_COTISATION,
                  "Cotisation en attente",
                  "Votre cotisation de "
                      + amount
                      + " pour "
                      + tontineName
                      + " est en attente de validation. J-"
                      + joursRestants
                      + " avant "
                      + nomEvenement
                      + ".",
                  tontine.getId());
            });

    List<TontineMembre> membresActifs =
        membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
            tontine.getId(), MembreStatut.ACTIF);

    boolean isJourCle =
        joursRestants == 30 || joursRestants == 7 || joursRestants == 3 || joursRestants == 1;

    if (isJourCle) {
      // Jour clé : notification urgente avec compte à rebours à tous les membres
      membresActifs.forEach(
          m -> {
            BigDecimal totalCotise =
                cotisationRepository.sumMontantValideByTontineAndMembre(tontine.getId(), m.getId());
            String totalStr = totalCotise.toPlainString() + " FCFA";
            whatsappService.sendEvenementielleUrgent(
                m.getUser().getPhone(), tontineName, nomEvenement, joursRestants, totalStr);
            notificationService.notify(
                m.getUser().getId(),
                NotificationType.RAPPEL_COTISATION,
                "J-" + joursRestants + " avant " + nomEvenement,
                "Il vous reste "
                    + joursRestants
                    + " jour(s) avant "
                    + nomEvenement
                    + ". Vous avez cotisé "
                    + totalStr
                    + " au total.",
                tontine.getId());
          });
    } else {
      // Rappel journalier : uniquement les membres qui n'ont pas cotisé dans ce sous-cycle
      membresActifs.stream()
          .filter(
              m ->
                  !cotisationRepository.existsByCycle_IdAndMembre_IdAndDeletedAtIsNull(
                      cycle.getId(), m.getId()))
          .forEach(
              m -> {
                BigDecimal totalCotise =
                    cotisationRepository.sumMontantValideByTontineAndMembre(
                        tontine.getId(), m.getId());
                String totalStr = totalCotise.toPlainString() + " FCFA";
                whatsappService.sendEvenementielleRappelPeriode(
                    m.getUser().getPhone(), tontineName, nomEvenement, joursRestants, totalStr);
                notificationService.notify(
                    m.getUser().getId(),
                    NotificationType.RAPPEL_COTISATION,
                    "Pensez à cotiser",
                    "Vous n'avez pas encore cotisé pour la période en cours de "
                        + tontineName
                        + ". J-"
                        + joursRestants
                        + " avant "
                        + nomEvenement
                        + ".",
                    tontine.getId());
              });
    }
  }

  // ─── Rappels cycles proches ──────────────────────────────────────────────

  /**
   * Vérifie chaque matin si des cycles se terminent dans 2 jours → notifie l'admin en in-app.
   * Vérifie aussi les tours de jackpot ROTATION dans 3 jours → notifie le futur bénéficiaire.
   */
  @Scheduled(cron = "0 0 8 * * *")
  @Transactional(readOnly = true)
  public void rappelsCyclesProches() {
    LocalDate dans2Jours = LocalDate.now().plusDays(2);
    LocalDate dans3Jours = LocalDate.now().plusDays(3);

    // Cycle bientôt clôturé (J-2) → notification in-app admin
    cycleRepository
        .findByStatutAndDateFinAndDeletedAtIsNull(CycleStatut.EN_COURS, dans2Jours)
        .forEach(
            cycle -> {
              Tontine tontine = cycle.getTontine();
              notificationService.notify(
                  tontine.getCreePar().getId(),
                  NotificationType.CYCLE_BIENTOT_CLOTURE,
                  "Cycle bientôt clôturé",
                  "Le cycle #"
                      + cycle.getNumeroCycle()
                      + " de "
                      + tontine.getNom()
                      + " se termine dans 2 jours",
                  tontine.getId());
            });

    // Prochain jackpot ROTATION dans 3 jours → notification in-app futur bénéficiaire
    cycleRepository
        .findByStatutAndDateFinAndDeletedAtIsNull(CycleStatut.EN_COURS, dans3Jours)
        .stream()
        .filter(
            cycle ->
                cycle.getTontine().getTontineType() == TontineType.ROTATIVE
                    && "ROTATION".equals(cycle.getTontine().getOrdreBeneficiaire()))
        .forEach(
            cycle -> {
              Tontine tontine = cycle.getTontine();
              membreRepository
                  .findByTontine_IdAndOrdreJackpotAndStatutAndDeletedAtIsNull(
                      tontine.getId(), cycle.getNumeroCycle(), MembreStatut.ACTIF)
                  .ifPresent(
                      membre ->
                          notificationService.notify(
                              membre.getUser().getId(),
                              NotificationType.TOUR_PROCHE,
                              "Votre tour arrive dans 3 jours",
                              "Vous recevrez le jackpot de " + tontine.getNom(),
                              tontine.getId()));
            });
  }

  // ─── Annonce gagnants ────────────────────────────────────────────────────

  /**
   * Notifie les gagnants d'un cycle clôturé. Appelé par {@code CycleServiceImpl.closeCycle} après
   * la désignation automatique des gagnants.
   */
  public void annoncerGagnants(CycleTontine cycle, List<CycleGagnant> gagnants) {
    if (gagnants.isEmpty()) {
      log.debug("Annonce gagnants ignorée – aucun gagnant sur cycleId={}", cycle.getId());
      return;
    }

    String tontineName = cycle.getTontine().getNom();

    for (CycleGagnant gagnant : gagnants) {
      try {
        String phone = gagnant.getMembre().getUser().getPhone();
        String montant =
            gagnant.getMontantRecu() != null
                ? gagnant.getMontantRecu().toPlainString() + " FCFA"
                : estimerMontant(cycle);
        whatsappService.sendJackpotNotification(phone, tontineName, montant);
        log.info(
            "Gagnant notifié – cycleId={} tontine={} phone={}", cycle.getId(), tontineName, phone);
      } catch (Exception e) {
        log.warn("Notif WA gagnant non envoyée – cycleId={} : {}", cycle.getId(), e.getMessage());
      }
    }
  }

  private String estimerMontant(CycleTontine cycle) {
    int n = cycle.getTontine().getNombreGagnants();
    BigDecimal brut =
        cycle
            .getTontine()
            .getMontant()
            .multiply(BigDecimal.valueOf(cycle.getTontine().getNombreMembres()));
    return brut.divide(BigDecimal.valueOf(n), 0, RoundingMode.HALF_UP).toPlainString() + " FCFA";
  }
}
