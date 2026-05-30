package com.africa.dinthialma_backend.tontine.service.impl;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.dto.CycleResponse;
import com.africa.dinthialma_backend.tontine.dto.OpenCycleRequest;
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

    cycleRepository.save(cycle);

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

  // ─── Helpers ─────────────────────────────────────────────────────────────

  private void activateNextCycle(UUID tontineId, int nextNum) {
    cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(tontineId).stream()
        .filter(c -> c.getNumeroCycle() == nextNum && c.getStatut() == CycleStatut.EN_ATTENTE)
        .findFirst()
        .ifPresent(
            next -> {
              next.setStatut(CycleStatut.EN_COURS);
              cycleRepository.save(next);
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
