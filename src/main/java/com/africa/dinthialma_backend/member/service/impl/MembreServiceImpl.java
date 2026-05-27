package com.africa.dinthialma_backend.member.service.impl;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserRoleAssignment;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.dto.AddMembreRequest;
import com.africa.dinthialma_backend.member.dto.MembreResponse;
import com.africa.dinthialma_backend.member.dto.UpdateMembreStatutRequest;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.member.service.interfaces.MembreService;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implémentation du service de gestion des cotisants d'une tontine. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MembreServiceImpl implements MembreService {

  private final TontineMembreRepository membreRepository;
  private final TontineRepository tontineRepository;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository roleAssignmentRepository;
  private final KeycloakAuthService keycloakAuthService;

  // ─── Ajout d'un membre ───────────────────────────────────────────────────

  @Override
  public MembreResponse addMembre(String keycloakId, UUID tontineId, AddMembreRequest request)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    User targetUser =
        userRepository
            .findById(request.getUserId())
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));

    // Vérifier unicité (un utilisateur ne peut cotiser qu'une fois par tontine)
    if (membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
        tontineId, targetUser.getId())) {
      throw new ConflictException(ResponseMessageConstants.MEMBER_ALREADY_EXISTS);
    }

    // Résoudre l'ordre jackpot
    int ordreJackpot;
    if (request.getOrdreJackpot() != null) {
      if (membreRepository.existsByTontine_IdAndOrdreJackpotAndDeletedAtIsNull(
          tontineId, request.getOrdreJackpot())) {
        throw new ConflictException(
            "La position " + request.getOrdreJackpot() + " est déjà occupée dans cette tontine");
      }
      ordreJackpot = request.getOrdreJackpot();
    } else {
      ordreJackpot = membreRepository.findMaxOrdreJackpot(tontineId) + 1;
    }

    TontineMembre membre =
        TontineMembre.builder()
            .tontine(tontine)
            .user(targetUser)
            .ordreJackpot(ordreJackpot)
            .statut(MembreStatut.ACTIF)
            .dateAdhesion(LocalDate.now())
            .build();

    TontineMembre saved = membreRepository.save(membre);

    // Attribuer le rôle MEMBER si pas encore assigné
    assignRoleIfAbsent(targetUser, UserRole.MEMBER);

    log.info(
        "Membre ajouté – tontineId={} userId={} membreId={}",
        tontineId,
        targetUser.getId(),
        saved.getId());
    return MembreResponse.from(saved);
  }

  // ─── Retrait d'un membre ─────────────────────────────────────────────────

  @Override
  public void removeMembre(String keycloakId, UUID tontineId, UUID membreId)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    TontineMembre membre =
        membreRepository
            .findByIdAndTontine_IdAndDeletedAtIsNull(membreId, tontineId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.MEMBER_NOT_FOUND));

    membre.setDeletedAt(LocalDateTime.now());
    membre.setStatut(MembreStatut.SORTI);
    membreRepository.save(membre);

    log.info("Membre retiré – tontineId={} membreId={}", tontineId, membreId);
  }

  // ─── Liste des membres ───────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<MembreResponse> listMembres(String keycloakId, UUID tontineId)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertCanAccess(caller, tontine);

    return membreRepository
        .findByTontine_IdAndDeletedAtIsNullOrderByOrdreJackpotAscCreatedAtAsc(tontineId)
        .stream()
        .map(MembreResponse::from)
        .toList();
  }

  // ─── Modification du statut ──────────────────────────────────────────────

  @Override
  public MembreResponse updateStatut(
      String keycloakId, UUID tontineId, UUID membreId, UpdateMembreStatutRequest request)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    TontineMembre membre =
        membreRepository
            .findByIdAndTontine_IdAndDeletedAtIsNull(membreId, tontineId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.MEMBER_NOT_FOUND));

    if (membre.getStatut() == MembreStatut.SORTI && request.getStatut() != MembreStatut.ACTIF) {
      throw new BadRequestException("Un membre SORTI ne peut être que réactivé");
    }

    membre.setStatut(request.getStatut());
    TontineMembre saved = membreRepository.save(membre);

    log.info("Statut membre modifié – membreId={} statut={}", membreId, request.getStatut());
    return MembreResponse.from(saved);
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

  private void assignRoleIfAbsent(User user, UserRole role) {
    if (roleAssignmentRepository.existsByUserIdAndRole(user.getId(), role)) return;

    boolean synced = true;
    try {
      keycloakAuthService.assignRole(user.getKeycloakId(), role.getKeycloakRole());
    } catch (Exception e) {
      log.warn(
          "Impossible d'assigner le rôle {} à user {} dans Keycloak : {}",
          role,
          user.getId(),
          e.getMessage());
      synced = false;
    }

    UserRoleAssignment assignment =
        UserRoleAssignment.builder().user(user).role(role).syncedToKeycloak(synced).build();
    roleAssignmentRepository.save(assignment);
  }
}
