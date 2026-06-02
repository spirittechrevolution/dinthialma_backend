package com.africa.dinthialma_backend.notification.service;

import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
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
 *   <li>Annonce bénéficiaire → déclenché par {@code CycleServiceImpl} à l'ouverture d'un cycle
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

  private final WhatsappService whatsappService;
  private final CycleTontineRepository cycleRepository;
  private final CotisationRepository cotisationRepository;

  // ─── Rappel quotidien ─────────────────────────────────────────────────────

  /**
   * Rappel quotidien à 8h : envoie un SMS à chaque cotisant dont la cotisation est EN_ATTENTE sur
   * un cycle EN_COURS.
   *
   * <p>Seules les cotisations déjà enregistrées mais non validées sont ciblées.
   */
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

  // ─── Annonce bénéficiaire ─────────────────────────────────────────────────

  /**
   * Notifie par SMS le bénéficiaire désigné d'un cycle qui vient d'être ouvert / activé.
   *
   * <p>Méthode appelée directement par {@code CycleServiceImpl} lors de l'ouverture manuelle ou de
   * l'activation automatique du cycle suivant. S'exécute dans la transaction du service appelant.
   */
  public void annoncerBeneficiaire(CycleTontine cycle) {
    if (cycle.getBeneficiaire() == null) {
      log.debug(
          "Annonce bénéficiaire ignorée – aucun bénéficiaire désigné sur cycleId={}",
          cycle.getId());
      return;
    }

    String phone = cycle.getBeneficiaire().getUser().getPhone();
    String tontineName = cycle.getTontine().getNom();

    BigDecimal montantPrevu =
        cycle
            .getTontine()
            .getMontant()
            .multiply(BigDecimal.valueOf(cycle.getTontine().getNombreMembres()))
            .setScale(0, RoundingMode.HALF_UP);
    String amount = montantPrevu.toPlainString() + " FCFA";

    whatsappService.sendJackpotNotification(phone, tontineName, amount);
    log.info(
        "Bénéficiaire annoncé – cycleId={} tontine={} phone={}", cycle.getId(), tontineName, phone);
  }
}
