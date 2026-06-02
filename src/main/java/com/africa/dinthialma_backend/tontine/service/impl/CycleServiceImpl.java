package com.africa.dinthialma_backend.tontine.service.impl;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.service.SchedulerService;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.dto.BeneficiaireHistoriqueResponse;
import com.africa.dinthialma_backend.tontine.dto.CycleResponse;
import com.africa.dinthialma_backend.tontine.dto.OpenCycleRequest;
import com.africa.dinthialma_backend.tontine.dto.SelectionnerBeneficiaireRequest;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.entity.TontineCommission;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineCommissionRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import com.africa.dinthialma_backend.tontine.service.interfaces.CycleService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implémentation du service de gestion des cycles de tontine. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CycleServiceImpl implements CycleService {

  private final CycleTontineRepository cycleRepository;
  private final TontineRepository tontineRepository;
  private final TontineMembreRepository membreRepository;
  private final CotisationRepository cotisationRepository;
  private final TontineCommissionRepository commissionRepository;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository roleAssignmentRepository;
  private final AuditService auditService;
  private final SchedulerService schedulerService;
  private final WhatsappService whatsappService;

  // ─── Lecture ─────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<CycleResponse> listCycles(String keycloakId, UUID tontineId, Pageable pageable)
      throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertCanAccess(caller, tontine);

    return cycleRepository
        .findByTontine_IdAndDeletedAtIsNull(tontineId, pageable)
        .map(CycleResponse::from);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BeneficiaireHistoriqueResponse> listBeneficiaires(
      String keycloakId, UUID tontineId, Pageable pageable) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertCanAccess(caller, tontine);

    return cycleRepository
        .findByTontine_IdAndStatutAndBeneficiaireIsNotNullAndDeletedAtIsNull(
            tontineId, CycleStatut.TERMINE, pageable)
        .map(BeneficiaireHistoriqueResponse::from);
  }

  @Override
  @Transactional(readOnly = true)
  public CycleResponse getCycle(String keycloakId, UUID tontineId, UUID cycleId)
      throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertCanAccess(caller, tontine);

    CycleTontine cycle = findCycleById(tontineId, cycleId);
    return CycleResponse.from(cycle);
  }

  // ─── Ouverture manuelle ──────────────────────────────────────────────────

  @Override
  public CycleResponse openCycle(String keycloakId, UUID tontineId, OpenCycleRequest request)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    if (tontine.getModeCycle() != ModeCycle.MANUEL) {
      throw new BadRequestException("L'ouverture manuelle n'est possible qu'en mode MANUEL");
    }
    if (tontine.getStatut() != TontineStatut.ACTIVE) {
      throw new BadRequestException("La tontine doit être ACTIVE pour ouvrir un cycle");
    }
    if (request.getDateFin().isBefore(request.getDateDebut())) {
      throw new BadRequestException("La date de fin doit être postérieure à la date de début");
    }

    // Vérifier qu'il n'y a pas déjà un cycle EN_COURS
    if (cycleRepository
        .findByTontine_IdAndStatutAndDeletedAtIsNull(tontineId, CycleStatut.EN_COURS)
        .isPresent()) {
      throw new BadRequestException(
          "Un cycle est déjà en cours. Clôturez-le avant d'en ouvrir un nouveau");
    }

    int nextNum =
        cycleRepository
            .findTopByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleDesc(tontineId)
            .map(c -> c.getNumeroCycle() + 1)
            .orElse(1);

    // Résoudre le bénéficiaire si fourni
    TontineMembre beneficiaire = null;
    if (request.getBeneficiaireId() != null) {
      beneficiaire =
          membreRepository
              .findByIdAndTontine_IdAndDeletedAtIsNull(request.getBeneficiaireId(), tontineId)
              .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.MEMBER_NOT_FOUND));
    }

    CycleTontine cycle =
        CycleTontine.builder()
            .tontine(tontine)
            .numeroCycle(nextNum)
            .beneficiaire(beneficiaire)
            .dateDebut(request.getDateDebut())
            .dateFin(request.getDateFin())
            .statut(CycleStatut.EN_COURS)
            .build();

    CycleTontine saved = cycleRepository.save(cycle);
    auditService.logCreate(caller, "cycles_tontine", saved.getId());
    schedulerService.annoncerBeneficiaire(saved);
    log.info(
        "Cycle manuel ouvert – tontineId={} cycleId={} num={}", tontineId, saved.getId(), nextNum);
    return CycleResponse.from(saved);
  }

  // ─── Clôture ─────────────────────────────────────────────────────────────

  @Override
  public CycleResponse closeCycle(String keycloakId, UUID tontineId, UUID cycleId)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    CycleTontine cycle = findCycleById(tontineId, cycleId);

    if (cycle.getStatut() != CycleStatut.EN_COURS) {
      throw new BadRequestException("Seul un cycle EN_COURS peut être clôturé");
    }

    // 1. Jackpot brut = somme des cotisations VALIDEES du cycle
    BigDecimal jackpot =
        cotisationRepository
            .findByCycle_IdAndStatutAndDeletedAtIsNull(cycleId, CotisationStatut.VALIDE)
            .stream()
            .map(c -> c.getMontant() != null ? c.getMontant() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 2. Calculer les commissions actives de la tontine
    List<TontineCommission> commissions =
        commissionRepository.findByTontine_IdAndDeletedAtIsNull(tontineId);

    BigDecimal totalCommission = BigDecimal.ZERO;
    for (TontineCommission commission : commissions) {
      BigDecimal part =
          switch (commission.getType()) {
            case POURCENTAGE_JACKPOT ->
                jackpot
                    .multiply(commission.getValeur())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FRAIS_FIXES_PAR_CYCLE -> commission.getValeur();
            case FRAIS_ADHESION -> BigDecimal.ZERO; // prélevé à l'adhésion, pas sur le jackpot
          };
      totalCommission = totalCommission.add(part);
    }

    // 3. Stocker les 3 montants et clôturer
    cycle.setStatut(CycleStatut.TERMINE);
    cycle.setMontantJackpot(jackpot);
    cycle.setMontantCommission(totalCommission);
    cycle.setMontantNet(jackpot.subtract(totalCommission));
    cycle.setDateRemise(LocalDate.now());

    // 4. Auto-assigner le bénéficiaire selon l'ordre configuré
    TontineMembre beneficiaire = resolveBeneficiaire(tontine, tontineId);
    if (beneficiaire != null) {
      cycle.setBeneficiaire(beneficiaire);
      beneficiaire.setARecuJackpot(true);
      beneficiaire.setDateJackpot(LocalDateTime.now());
      membreRepository.save(beneficiaire);
    }

    cycleRepository.save(cycle);
    auditService.log(
        caller,
        "cycles_tontine",
        cycleId,
        "statut",
        CycleStatut.EN_COURS.name(),
        CycleStatut.TERMINE.name());

    // Notifier le bénéficiaire auto-assigné
    if (beneficiaire != null) {
      try {
        String montantNetStr =
            cycle.getMontantNet() != null
                ? cycle.getMontantNet().toPlainString() + " FCFA"
                : "montant en cours de calcul";
        whatsappService.send(
            beneficiaire.getUser().getPhone(),
            "🎉 *Félicitations !* Vous êtes le(la) bénéficiaire du jackpot du cycle "
                + cycle.getNumeroCycle()
                + " de la tontine *"
                + tontine.getNom()
                + "*."
                + "\nMontant net : *"
                + montantNetStr
                + "*."
                + "\nVotre gestionnaire vous contactera pour la remise.");
      } catch (Exception e) {
        log.warn("Notif WA bénéficiaire jackpot (clôture) non envoyée : {}", e.getMessage());
      }
    }

    // Passer les cotisations EN_ATTENTE restantes à EN_RETARD
    cotisationRepository
        .findByCycle_IdAndStatutAndDeletedAtIsNull(cycleId, CotisationStatut.EN_ATTENTE)
        .forEach(
            c -> {
              c.setStatut(CotisationStatut.EN_RETARD);
              cotisationRepository.save(c);
            });

    // En mode AUTOMATIQUE, passer le cycle suivant EN_ATTENTE → EN_COURS
    if (tontine.getModeCycle() == ModeCycle.AUTOMATIQUE) {
      activateNextCycle(tontineId, cycle.getNumeroCycle() + 1);
    }

    log.info(
        "Cycle clôturé – tontineId={} cycleId={} jackpot={} commission={} net={}",
        tontineId,
        cycleId,
        jackpot,
        totalCommission,
        jackpot.subtract(totalCommission));
    return CycleResponse.from(cycle);
  }

  // ─── Sélection bénéficiaire jackpot ──────────────────────────────────────

  @Override
  public CycleResponse selectionnerBeneficiaire(
      String keycloakId, UUID tontineId, UUID cycleId, SelectionnerBeneficiaireRequest request)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    CycleTontine cycle = findCycleById(tontineId, cycleId);

    if (cycle.getStatut() != CycleStatut.TERMINE) {
      throw new BadRequestException(
          "La sélection du bénéficiaire n'est possible que sur un cycle TERMINE");
    }
    if (!"MANUEL".equals(tontine.getOrdreBeneficiaire())) {
      throw new BadRequestException(
          "La désignation manuelle du bénéficiaire n'est disponible que pour les tontines"
              + " avec ordre bénéficiaire MANUEL");
    }
    if (cycle.getBeneficiaire() != null) {
      throw new BadRequestException(
          "Un bénéficiaire a déjà été désigné pour ce cycle : "
              + cycle.getBeneficiaire().getUser().getFirstName());
    }

    TontineMembre beneficiaire;

    if (request.getMembreId() != null) {
      // Sélection manuelle
      beneficiaire =
          membreRepository
              .findByIdAndTontine_IdAndDeletedAtIsNull(request.getMembreId(), tontineId)
              .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.MEMBER_NOT_FOUND));

      if (beneficiaire.isARecuJackpot()) {
        throw new BadRequestException(
            beneficiaire.getUser().getFirstName()
                + " a déjà reçu un jackpot dans cette tontine et n'est pas éligible");
      }
    } else {
      // Sélection aléatoire parmi les membres ACTIF sans jackpot
      List<TontineMembre> eligibles =
          membreRepository
              .findByTontine_IdAndStatutAndDeletedAtIsNull(tontineId, MembreStatut.ACTIF)
              .stream()
              .filter(m -> !m.isARecuJackpot())
              .toList();

      if (eligibles.isEmpty()) {
        throw new BadRequestException(
            "Aucun membre éligible : tous les membres actifs ont déjà reçu un jackpot");
      }
      beneficiaire = eligibles.get((int) (Math.random() * eligibles.size()));
    }

    // Marquer le cycle
    cycle.setBeneficiaire(beneficiaire);
    cycleRepository.save(cycle);

    // Marquer le membre comme ayant reçu le jackpot
    beneficiaire.setARecuJackpot(true);
    beneficiaire.setDateJackpot(LocalDateTime.now());
    membreRepository.save(beneficiaire);

    auditService.log(
        caller,
        "cycles_tontine",
        cycleId,
        "beneficiaire_id",
        null,
        beneficiaire.getId().toString());

    log.info(
        "Bénéficiaire jackpot sélectionné – cycleId={} membreId={} tontineId={}",
        cycleId,
        beneficiaire.getId(),
        tontineId);

    // Notification WhatsApp au bénéficiaire
    try {
      String montantNet =
          cycle.getMontantNet() != null
              ? cycle.getMontantNet().toPlainString() + " FCFA"
              : "montant en cours de calcul";
      whatsappService.send(
          beneficiaire.getUser().getPhone(),
          "🎉 *Félicitations !* Vous êtes le(la) bénéficiaire du jackpot du cycle "
              + cycle.getNumeroCycle()
              + " de la tontine *"
              + tontine.getNom()
              + "*."
              + "\nMontant net : *"
              + montantNet
              + "*."
              + "\nVotre gestionnaire vous contactera pour la remise.");
    } catch (Exception e) {
      log.warn("Notif WA bénéficiaire jackpot non envoyée : {}", e.getMessage());
    }

    // Notification WhatsApp à l'admin — confirmation
    try {
      whatsappService.send(
          tontine.getCreePar().getPhone(),
          "✅ *Dinthialma* – Vous avez désigné *"
              + beneficiaire.getUser().getFirstName()
              + " "
              + beneficiaire.getUser().getLastName()
              + "* comme bénéficiaire du jackpot (cycle "
              + cycle.getNumeroCycle()
              + ", tontine *"
              + tontine.getNom()
              + "*).");
    } catch (Exception e) {
      log.warn("Notif WA admin (jackpot) non envoyée : {}", e.getMessage());
    }

    return CycleResponse.from(cycle);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  /**
   * Détermine automatiquement le bénéficiaire du jackpot selon l'ordre configuré sur la tontine.
   *
   * <ul>
   *   <li>{@code ROTATION} → membre ACTIF sans jackpot avec le plus petit {@code ordreJackpot}
   *       (non-null) ; si tous null, le plus ancien membre.
   *   <li>{@code ALEATOIRE} → tirage au sort parmi les membres ACTIF sans jackpot.
   *   <li>{@code MANUEL} → retourne {@code null} ; l'admin désigne via PATCH.
   * </ul>
   *
   * <p>Si un seul membre éligible reste, il est retourné quelle que soit la règle.
   */
  private TontineMembre resolveBeneficiaire(Tontine tontine, UUID tontineId) {
    List<TontineMembre> eligibles =
        membreRepository
            .findByTontine_IdAndStatutAndDeletedAtIsNull(tontineId, MembreStatut.ACTIF)
            .stream()
            .filter(m -> !m.isARecuJackpot())
            .toList();

    if (eligibles.isEmpty()) return null;
    if (eligibles.size() == 1) return eligibles.get(0);

    return switch (tontine.getOrdreBeneficiaire()) {
      case "ROTATION" ->
          eligibles.stream()
              .filter(m -> m.getOrdreJackpot() != null)
              .min(Comparator.comparingInt(TontineMembre::getOrdreJackpot))
              .orElseGet(
                  () ->
                      eligibles.stream()
                          .min(Comparator.comparing(TontineMembre::getCreatedAt))
                          .orElse(eligibles.get(0)));
      case "ALEATOIRE" -> eligibles.get((int) (Math.random() * eligibles.size()));
      default -> null; // MANUEL
    };
  }

  private void activateNextCycle(UUID tontineId, int nextNum) {
    cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(tontineId).stream()
        .filter(c -> c.getNumeroCycle() == nextNum && c.getStatut() == CycleStatut.EN_ATTENTE)
        .findFirst()
        .ifPresent(
            next -> {
              next.setStatut(CycleStatut.EN_COURS);
              cycleRepository.save(next);
              schedulerService.annoncerBeneficiaire(next);
              log.info("Cycle suivant activé – cycleId={} num={}", next.getId(), nextNum);
            });
  }

  private User findUserByKeycloakId(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }

  private Tontine findTontineById(UUID id) throws CustomException {
    return tontineRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.TONTINE_NOT_FOUND));
  }

  private CycleTontine findCycleById(UUID tontineId, UUID cycleId) throws CustomException {
    return cycleRepository
        .findByIdAndTontine_IdAndDeletedAtIsNull(cycleId, tontineId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.CYCLE_NOT_FOUND));
  }

  private void assertIsCreatorOrSuperAdmin(User caller, Tontine tontine) throws CustomException {
    if (isSuperAdmin(caller)) return;
    if (!tontine.getCreePar().getId().equals(caller.getId())) {
      throw new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED);
    }
  }

  private void assertCanAccess(User caller, Tontine tontine) throws CustomException {
    if (isSuperAdmin(caller)) return;
    if (tontine.getCreePar().getId().equals(caller.getId())) return;
    if (membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
        tontine.getId(), caller.getId())) return;
    throw new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED);
  }

  private boolean isSuperAdmin(User user) {
    return roleAssignmentRepository.existsByUserIdAndRole(user.getId(), UserRole.SUPER_ADMIN);
  }
}
