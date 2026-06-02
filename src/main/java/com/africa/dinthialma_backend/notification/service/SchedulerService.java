package com.africa.dinthialma_backend.notification.service;

import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleGagnant;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.repository.CycleGagnantRepository;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Planificateur de notifications SMS liées aux tontines.
 *
 * <ul>
 *   <li>Rappel quotidien à 8h → cotisations EN_ATTENTE sur cycles EN_COURS
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

  // ─── Rappel quotidien ─────────────────────────────────────────────────────

  @Scheduled(cron = "0 0 8 * * *")
  @Transactional(readOnly = true)
  public void rappelCotisationsEnAttente() {
    List<CycleTontine> cyclesEnCours =
        cycleRepository.findByStatutAndDeletedAtIsNull(CycleStatut.EN_COURS);
    log.info("Rappel cotisations – {} cycle(s) EN_COURS", cyclesEnCours.size());

    for (CycleTontine cycle : cyclesEnCours) {
      cotisationRepository
          .findByCycle_IdAndStatutAndDeletedAtIsNull(cycle.getId(), CotisationStatut.EN_ATTENTE)
          .forEach(
              cotisation -> {
                String phone = cotisation.getMembre().getUser().getPhone();
                String tontineName = cycle.getTontine().getNom();
                String amount = cotisation.getMontant().toPlainString() + " FCFA";
                whatsappService.sendContributionReminder(phone, tontineName, amount);
              });
    }
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
