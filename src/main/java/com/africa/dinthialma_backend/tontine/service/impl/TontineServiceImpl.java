package com.africa.dinthialma_backend.tontine.service.impl;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserRoleAssignment;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.dto.CreateTontineRequest;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import com.africa.dinthialma_backend.tontine.dto.UpdateTontineRequest;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import com.africa.dinthialma_backend.tontine.service.interfaces.TontineService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service tontine.
 *
 * <p>Règles d'accès :
 *
 * <ul>
 *   <li>Création → tout utilisateur authentifié
 *   <li>Modification / suppression / activation → créateur ou SUPER_ADMIN
 *   <li>Lecture → membre de la tontine, créateur, SUPER_ADMIN
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TontineServiceImpl implements TontineService {

  private final TontineRepository tontineRepository;
  private final CycleTontineRepository cycleRepository;
  private final TontineMembreRepository membreRepository;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository roleAssignmentRepository;
  private final KeycloakAuthService keycloakAuthService;

  // ─── Création ────────────────────────────────────────────────────────────

  @Override
  public TontineResponse createTontine(String keycloakId, CreateTontineRequest request)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);

    Tontine tontine =
        Tontine.builder()
            .nom(request.getNom().trim())
            .description(request.getDescription())
            .montant(request.getMontant())
            .frequence(request.getFrequence())
            .ordreBeneficiaire(request.getOrdreBeneficiaire())
            .modeCycle(request.getModeCycle())
            .dateDebut(request.getDateDebut())
            .nombreMembres(request.getNombreMembres())
            .statut(TontineStatut.BROUILLON)
            .creePar(caller)
            .build();

    Tontine saved = tontineRepository.save(tontine);

    // Attribuer le rôle ADMIN si l'utilisateur ne l'a pas encore
    assignRoleIfAbsent(caller, UserRole.ADMIN);

    log.info("Tontine créée – id={} par userId={}", saved.getId(), caller.getId());
    return TontineResponse.from(saved, 0);
  }

  // ─── Lecture ─────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public TontineResponse getTontine(String keycloakId, UUID id) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(id);

    assertCanAccess(caller, tontine);

    int nombreMembres = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(id);
    return TontineResponse.from(tontine, nombreMembres);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<TontineResponse> listTontines(String keycloakId, Pageable pageable)
      throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);

    Page<Tontine> tontines;
    if (isSuperAdmin(caller)) {
      tontines = tontineRepository.findByDeletedAtIsNull(pageable);
    } else {
      tontines = tontineRepository.findVisibleByUserId(caller.getId(), pageable);
    }

    return tontines.map(
        t -> {
          int count = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(t.getId());
          return TontineResponse.from(t, count);
        });
  }

  // ─── Mise à jour ─────────────────────────────────────────────────────────

  @Override
  public TontineResponse updateTontine(String keycloakId, UUID id, UpdateTontineRequest request)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(id);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    if (tontine.getStatut() != TontineStatut.BROUILLON) {
      throw new BadRequestException("La tontine ne peut être modifiée qu'en statut BROUILLON");
    }

    if (request.getNom() != null) tontine.setNom(request.getNom().trim());
    if (request.getDescription() != null) tontine.setDescription(request.getDescription());
    if (request.getMontant() != null) tontine.setMontant(request.getMontant());
    if (request.getFrequence() != null) tontine.setFrequence(request.getFrequence());
    if (request.getOrdreBeneficiaire() != null)
      tontine.setOrdreBeneficiaire(request.getOrdreBeneficiaire());
    if (request.getDateDebut() != null) tontine.setDateDebut(request.getDateDebut());
    if (request.getNombreMembres() != null) tontine.setNombreMembres(request.getNombreMembres());

    Tontine saved = tontineRepository.save(tontine);
    int nombreMembres = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(id);
    return TontineResponse.from(saved, nombreMembres);
  }

  // ─── Suppression ─────────────────────────────────────────────────────────

  @Override
  public void deleteTontine(String keycloakId, UUID id) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(id);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    if (tontine.getStatut() != TontineStatut.BROUILLON) {
      throw new BadRequestException("Seule une tontine en BROUILLON peut être supprimée");
    }

    tontine.setDeletedAt(LocalDateTime.now());
    tontineRepository.save(tontine);
    log.info("Tontine supprimée (soft delete) – id={}", id);
  }

  // ─── Activation ──────────────────────────────────────────────────────────

  @Override
  public TontineResponse activateTontine(String keycloakId, UUID id) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(id);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    if (tontine.getStatut() != TontineStatut.BROUILLON
        && tontine.getStatut() != TontineStatut.SUSPENDUE) {
      throw new BadRequestException(
          "La tontine doit être en BROUILLON ou SUSPENDUE pour être activée");
    }

    tontine.setStatut(TontineStatut.ACTIVE);

    // En mode AUTOMATIQUE, générer les cycles si pas encore générés
    if (tontine.getModeCycle() == ModeCycle.AUTOMATIQUE
        && !cycleRepository.existsByTontine_IdAndDeletedAtIsNull(id)) {
      generateCycles(tontine);
    }

    Tontine saved = tontineRepository.save(tontine);
    int nombreMembres = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(id);
    log.info("Tontine activée – id={}", id);
    return TontineResponse.from(saved, nombreMembres);
  }

  // ─── Suspension ──────────────────────────────────────────────────────────

  @Override
  public TontineResponse suspendTontine(String keycloakId, UUID id) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(id);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    if (tontine.getStatut() != TontineStatut.ACTIVE) {
      throw new BadRequestException("Seule une tontine ACTIVE peut être suspendue");
    }

    tontine.setStatut(TontineStatut.SUSPENDUE);
    Tontine saved = tontineRepository.save(tontine);
    int nombreMembres = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(id);
    log.info("Tontine suspendue – id={}", id);
    return TontineResponse.from(saved, nombreMembres);
  }

  // ─── Génération automatique des cycles ──────────────────────────────────

  /**
   * Génère tous les cycles d'une tontine (mode {@code AUTOMATIQUE}).
   *
   * <p>Ordre des bénéficiaires :
   *
   * <ul>
   *   <li>{@code FIXE} : membres triés par {@code ordre_jackpot}
   *   <li>{@code ALEATOIRE} : mélange aléatoire, puis attribution des {@code ordre_jackpot}
   * </ul>
   */
  private void generateCycles(Tontine tontine) {
    List<TontineMembre> membres =
        new ArrayList<>(
            membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
                tontine.getId(), MembreStatut.ACTIF));

    if ("ALEATOIRE".equals(tontine.getOrdreBeneficiaire())) {
      Collections.shuffle(membres);
      for (int i = 0; i < membres.size(); i++) {
        membres.get(i).setOrdreJackpot(i + 1);
      }
      membreRepository.saveAll(membres);
    } else {
      membres.sort(
          Comparator.comparingInt(
              m -> (m.getOrdreJackpot() != null ? m.getOrdreJackpot() : Integer.MAX_VALUE)));
    }

    LocalDate current = tontine.getDateDebut();

    for (int i = 0; i < membres.size(); i++) {
      LocalDate fin = calculateEndDate(current, tontine.getFrequence());
      boolean isFirst = (i == 0);

      CycleTontine cycle =
          CycleTontine.builder()
              .tontine(tontine)
              .numeroCycle(i + 1)
              .beneficiaire(membres.get(i))
              .dateDebut(current)
              .dateFin(fin)
              .statut(isFirst ? CycleStatut.EN_COURS : CycleStatut.EN_ATTENTE)
              .build();

      cycleRepository.save(cycle);
      current = fin.plusDays(1);
    }

    log.info("Cycles générés – tontineId={} nbCycles={}", tontine.getId(), membres.size());
  }

  /**
   * Calcule la date de fin d'un cycle selon la fréquence.
   *
   * @param start date de début
   * @param frequence code fréquence (HEBDOMADAIRE, BIMENSUEL, MENSUEL, TRIMESTRIEL…)
   * @return date de fin (inclusive)
   */
  private LocalDate calculateEndDate(LocalDate start, String frequence) {
    return switch (frequence.toUpperCase()) {
      case "HEBDOMADAIRE" -> start.plusDays(6);
      case "BIMENSUEL" -> start.plusDays(13);
      case "TRIMESTRIEL" -> start.plusMonths(3).minusDays(1);
      default -> start.plusMonths(1).minusDays(1); // MENSUEL par défaut
    };
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

  /** Vérifie que l'appelant est le créateur de la tontine ou SUPER_ADMIN. */
  private void assertIsCreatorOrSuperAdmin(User caller, Tontine tontine) throws CustomException {
    if (isSuperAdmin(caller)) return;
    if (!tontine.getCreePar().getId().equals(caller.getId())) {
      throw new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED);
    }
  }

  /** Vérifie que l'appelant peut accéder à la tontine (est membre, créateur ou SUPER_ADMIN). */
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

  /**
   * Assigne un rôle Keycloak à un utilisateur s'il ne l'a pas déjà.
   *
   * <p>En cas d'échec Keycloak (indisponibilité), enregistre tout de même l'attribution en base
   * avec {@code syncedToKeycloak = false} pour synchronisation ultérieure.
   */
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
