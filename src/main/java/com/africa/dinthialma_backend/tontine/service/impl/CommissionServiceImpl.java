package com.africa.dinthialma_backend.tontine.service.impl;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.tontine.dto.CommissionResponse;
import com.africa.dinthialma_backend.tontine.dto.CreateCommissionRequest;
import com.africa.dinthialma_backend.tontine.dto.UpdateCommissionRequest;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.entity.TontineCommission;
import com.africa.dinthialma_backend.tontine.repository.TontineCommissionRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import com.africa.dinthialma_backend.tontine.service.interfaces.CommissionService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implémentation du service de gestion des commissions de tontine. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommissionServiceImpl implements CommissionService {

  private final TontineCommissionRepository commissionRepository;
  private final TontineRepository tontineRepository;
  private final TontineMembreRepository membreRepository;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository roleAssignmentRepository;
  private final AuditService auditService;

  // ─── Création ────────────────────────────────────────────────────────────

  @Override
  public CommissionResponse createCommission(
      String keycloakId, UUID tontineId, CreateCommissionRequest request) throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    // Un seul type de commission par tontine (contrainte UNIQUE en base)
    boolean exists =
        commissionRepository.findByTontine_IdAndDeletedAtIsNull(tontineId).stream()
            .anyMatch(c -> c.getType() == request.getType());
    if (exists) {
      throw new ConflictException(ResponseMessageConstants.COMMISSION_ALREADY_EXISTS);
    }

    TontineCommission commission =
        TontineCommission.builder()
            .tontine(tontine)
            .type(request.getType())
            .valeur(request.getValeur())
            .description(request.getDescription())
            .build();

    TontineCommission saved = commissionRepository.save(commission);
    auditService.logCreate(caller, "tontine_commissions", saved.getId());
    log.info(
        "Commission créée – tontineId={} type={} valeur={}",
        tontineId,
        saved.getType(),
        saved.getValeur());
    return CommissionResponse.from(saved);
  }

  // ─── Lecture ─────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<CommissionResponse> listCommissions(
      String keycloakId, UUID tontineId, Pageable pageable) throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertCanAccess(caller, tontine);

    return commissionRepository
        .findByTontine_IdAndDeletedAtIsNull(tontineId, pageable)
        .map(CommissionResponse::from);
  }

  // ─── Mise à jour ─────────────────────────────────────────────────────────

  @Override
  public CommissionResponse updateCommission(
      String keycloakId, UUID tontineId, UUID commissionId, UpdateCommissionRequest request)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    TontineCommission commission = findCommissionById(tontineId, commissionId);

    if (request.getValeur() != null) {
      auditService.log(
          caller,
          "tontine_commissions",
          commissionId,
          "valeur",
          commission.getValeur().toPlainString(),
          request.getValeur().toPlainString());
      commission.setValeur(request.getValeur());
    }
    if (request.getDescription() != null) {
      auditService.log(
          caller,
          "tontine_commissions",
          commissionId,
          "description",
          commission.getDescription(),
          request.getDescription());
      commission.setDescription(request.getDescription());
    }

    TontineCommission saved = commissionRepository.save(commission);
    log.info("Commission mise à jour – id={} tontineId={}", commissionId, tontineId);
    return CommissionResponse.from(saved);
  }

  // ─── Suppression ─────────────────────────────────────────────────────────

  @Override
  public void deleteCommission(String keycloakId, UUID tontineId, UUID commissionId)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    TontineCommission commission = findCommissionById(tontineId, commissionId);
    commission.setDeletedAt(LocalDateTime.now());
    commissionRepository.save(commission);
    auditService.logDelete(caller, "tontine_commissions", commissionId);
    log.info("Commission supprimée (soft delete) – id={} tontineId={}", commissionId, tontineId);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

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

  private TontineCommission findCommissionById(UUID tontineId, UUID commissionId)
      throws CustomException {
    return commissionRepository
        .findByIdAndTontine_IdAndDeletedAtIsNull(commissionId, tontineId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.COMMISSION_NOT_FOUND));
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
