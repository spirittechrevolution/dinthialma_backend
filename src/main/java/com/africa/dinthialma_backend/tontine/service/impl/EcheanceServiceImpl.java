package com.africa.dinthialma_backend.tontine.service.impl;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.tontine.calendrier.CalendrierTours;
import com.africa.dinthialma_backend.tontine.calendrier.CalendrierTours.PeriodeTour;
import com.africa.dinthialma_backend.tontine.calendrier.Rythme;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.dto.ModifierEcheanceRequest;
import com.africa.dinthialma_backend.tontine.dto.RappelsEcheance;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import com.africa.dinthialma_backend.tontine.service.interfaces.EcheanceService;
import com.africa.dinthialma_backend.tontine.service.interfaces.TontineService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EcheanceServiceImpl implements EcheanceService {

  private final TontineRepository tontineRepository;
  private final CycleTontineRepository cycleRepository;
  private final TontineMembreRepository membreRepository;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository roleAssignmentRepository;
  private final AuditService auditService;
  private final NotificationService notificationService;
  private final TontineService tontineService;

  @Override
  public TontineResponse modifierEcheance(
      String keycloakId, UUID tontineId, ModifierEcheanceRequest request) throws CustomException {
    User caller =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
    Tontine tontine =
        tontineRepository
            .findByIdAndDeletedAtIsNull(tontineId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.TONTINE_NOT_FOUND));
    assertIsCreatorOrSuperAdmin(caller, tontine);
    if (tontine.getStatut() == TontineStatut.TERMINEE) {
      throw new BadRequestException(
          "L'échéance d'une tontine terminée ne se modifie plus",
          ErrorCode.ECHEANCE_NON_MODIFIABLE);
    }

    Rythme actuel = tontine.getRythme();
    Rythme nouveau =
        actuel.avecEcheance(
            request.getJourEcheance() != null ? request.getJourEcheance() : actuel.jourEcheance(),
            request.getHeureEcheance() != null
                ? request.getHeureEcheance()
                : Rythme.formaterHeure(actuel.heureEcheance()));
    if (!nouveau.equals(actuel)) {
      tontine.appliquerRythme(nouveau);
      auditService.log(
          caller, "tontines", tontineId, "echeance", actuel.libelle(), nouveau.libelle());
      Integer premierTour = replanifierToursAVenir(tontine);
      notifierReglesModifiees(tontine, nouveau, premierTour);
      log.info(
          "Échéance modifiée – tontineId={} « {} » → « {} »",
          tontineId,
          actuel.libelle(),
          nouveau.libelle());
    }
    appliquerRappels(caller, tontine, request.getRappels());

    tontineRepository.save(tontine);
    return tontineService.getTontine(keycloakId, tontineId);
  }

  // ─── Replanification des tours à venir ───────────────────────────────────

  /**
   * Replace les tours EN_ATTENTE à la suite du dernier tour commencé.
   *
   * @return numéro du premier tour replanifié (null si aucun tour à venir : mode manuel, brouillon)
   */
  private Integer replanifierToursAVenir(Tontine tontine) {
    List<CycleTontine> tours =
        cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(tontine.getId());
    List<CycleTontine> aVenir =
        tours.stream().filter(c -> c.getStatut() == CycleStatut.EN_ATTENTE).toList();
    if (aVenir.isEmpty()) {
      return null;
    }
    CycleTontine dernierCommence =
        tours.stream()
            .filter(c -> c.getStatut() != CycleStatut.EN_ATTENTE)
            .reduce((a, b) -> b)
            .orElse(null);
    LocalDate precedente = dernierCommence != null ? dernierCommence.getDateFin() : null;
    LocalDate debut = precedente != null ? precedente.plusDays(1) : tontine.getDateDebut();

    boolean evenementielle = tontine.getTontineType() == TontineType.EVENEMENTIELLE;
    int parTour = tontine.getCotisationsParTourEffectif();
    List<List<PeriodeTour>> periodes =
        CalendrierTours.grouper(
            CalendrierTours.generer(
                tontine.getRythme(),
                debut,
                evenementielle ? Integer.MAX_VALUE : aVenir.size() * parTour,
                evenementielle ? tontine.getDateEcheance() : null,
                precedente),
            parTour);
    appliquerPeriodes(tontine, aVenir, periodes);
    return aVenir.get(0).getNumeroCycle();
  }

  /**
   * Déplace les tours existants ; les périodes en plus deviennent de nouveaux tours, les tours en
   * trop (événementielle) sont retirés (suppression logique, ils n'ont aucune cotisation).
   */
  private void appliquerPeriodes(
      Tontine tontine, List<CycleTontine> aVenir, List<List<PeriodeTour>> periodes) {
    int numero = aVenir.get(0).getNumeroCycle();
    for (int i = 0; i < Math.max(aVenir.size(), periodes.size()); i++) {
      if (i >= periodes.size()) {
        retirer(aVenir.get(i));
      } else if (i >= aVenir.size()) {
        cycleRepository.save(
            CalendrierTours.tour(tontine, numero + i, periodes.get(i), CycleStatut.EN_ATTENTE));
      } else {
        CalendrierTours.deplacer(aVenir.get(i), periodes.get(i));
        cycleRepository.save(aVenir.get(i));
      }
    }
  }

  private void retirer(CycleTontine tour) {
    LocalDateTime maintenant = LocalDateTime.now();
    tour.setDeletedAt(maintenant);
    tour.getEcheances().forEach(e -> e.setDeletedAt(maintenant));
    cycleRepository.save(tour);
  }

  // ─── Rappels et notification ─────────────────────────────────────────────

  private void appliquerRappels(User caller, Tontine tontine, RappelsEcheance rappels) {
    if (rappels == null) {
      return;
    }
    if (rappels.getVeille() != null && rappels.getVeille() != tontine.isRappelVeille()) {
      auditService.log(
          caller,
          "tontines",
          tontine.getId(),
          "rappelVeille",
          String.valueOf(tontine.isRappelVeille()),
          String.valueOf(rappels.getVeille()));
      tontine.setRappelVeille(rappels.getVeille());
    }
    if (rappels.getHeuresAvant() != null
        && rappels.getHeuresAvant() != tontine.getRappelHeuresAvant()) {
      auditService.log(
          caller,
          "tontines",
          tontine.getId(),
          "rappelHeuresAvant",
          String.valueOf(tontine.getRappelHeuresAvant()),
          String.valueOf(rappels.getHeuresAvant()));
      tontine.setRappelHeuresAvant(rappels.getHeuresAvant());
    }
  }

  /** Membres actifs prévenus, sauf en brouillon (les règles ne s'appliquent pas encore). */
  private void notifierReglesModifiees(Tontine tontine, Rythme rythme, Integer premierTour) {
    if (tontine.getStatut() == TontineStatut.BROUILLON) {
      return;
    }
    String aPartirDe =
        premierTour != null ? "à partir du tour n°" + premierTour : "à partir du prochain tour";
    String message =
        "Nouvelle échéance pour "
            + tontine.getNom()
            + " : "
            + rythme.libelle()
            + ", "
            + aPartirDe
            + ". Le tour en cours ne change pas.";
    membreRepository
        .findByTontine_IdAndStatutAndDeletedAtIsNull(tontine.getId(), MembreStatut.ACTIF)
        .forEach(
            m ->
                notificationService.notify(
                    m.getUser().getId(),
                    NotificationType.REGLES_MODIFIEES,
                    "Échéance modifiée",
                    message,
                    tontine.getId()));
  }

  private void assertIsCreatorOrSuperAdmin(User caller, Tontine tontine) throws CustomException {
    if (roleAssignmentRepository.existsByUserIdAndRole(caller.getId(), UserRole.SUPER_ADMIN)) {
      return;
    }
    if (!tontine.getCreePar().getId().equals(caller.getId())) {
      throw new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED);
    }
  }
}
