package com.africa.dinthialma_backend.admin.service.impl;

import com.africa.dinthialma_backend.admin.dto.AdminUserResponse;
import com.africa.dinthialma_backend.admin.dto.GlobalDashboardResponse;
import com.africa.dinthialma_backend.admin.dto.MyDashboardResponse;
import com.africa.dinthialma_backend.admin.dto.UpdateUserRolesRequest;
import com.africa.dinthialma_backend.admin.service.interfaces.AdminDashboardService;
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
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implémentation du service d'administration et de dashboard. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminDashboardServiceImpl implements AdminDashboardService {

  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository roleRepository;
  private final TontineRepository tontineRepository;
  private final CycleTontineRepository cycleRepository;
  private final TontineMembreRepository membreRepository;
  private final CotisationRepository cotisationRepository;
  private final KeycloakAuthService keycloakAuthService;

  // ─── Dashboard global SUPER_ADMIN ─────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public GlobalDashboardResponse getGlobalDashboard(String keycloakId) throws CustomException {
    assertSuperAdmin(keycloakId);

    LocalDateTime debutMois =
        LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime hier = LocalDateTime.now().minusHours(24);

    // ─── Users ────────────────────────────────────────────────────────────
    long totalUsers = userRepository.countByDeletedAtIsNull();
    long actifsUsers = userRepository.countByActiveTrueAndDeletedAtIsNull();
    long desactivesUsers = userRepository.countByActiveFalseAndDeletedAtIsNull();
    long nouveauxCeMois =
        userRepository.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(debutMois);

    // ─── Tontines ─────────────────────────────────────────────────────────
    long totalTontines = tontineRepository.countByDeletedAtIsNull();
    long brouillon = tontineRepository.countByStatutAndDeletedAtIsNull(TontineStatut.BROUILLON);
    long actives = tontineRepository.countByStatutAndDeletedAtIsNull(TontineStatut.ACTIVE);
    long suspendues = tontineRepository.countByStatutAndDeletedAtIsNull(TontineStatut.SUSPENDUE);
    long terminees = tontineRepository.countByStatutAndDeletedAtIsNull(TontineStatut.TERMINEE);

    // ─── Finances ─────────────────────────────────────────────────────────
    long enAttente =
        cotisationRepository.countByStatutAndDeletedAtIsNull(CotisationStatut.EN_ATTENTE);
    long enRetard =
        cotisationRepository.countByStatutAndDeletedAtIsNull(CotisationStatut.EN_RETARD);
    var montantMois = cotisationRepository.sumMontantValideSince(debutMois);

    // ─── Activité 24h ─────────────────────────────────────────────────────
    long nouveauxInscrits24h =
        userRepository.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(hier);
    long cotisations24h =
        cotisationRepository.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(hier);

    return GlobalDashboardResponse.builder()
        .users(
            GlobalDashboardResponse.UsersStats.builder()
                .total(totalUsers)
                .actifs(actifsUsers)
                .desactives(desactivesUsers)
                .nouveauxCeMois(nouveauxCeMois)
                .build())
        .tontines(
            GlobalDashboardResponse.TontinesStats.builder()
                .total(totalTontines)
                .brouillon(brouillon)
                .actives(actives)
                .suspendues(suspendues)
                .terminees(terminees)
                .build())
        .finances(
            GlobalDashboardResponse.FinancesStats.builder()
                .cotisationsEnAttente(enAttente)
                .cotisationsEnRetard(enRetard)
                .montantCotisationsCeMois(montantMois)
                .build())
        .activite24h(
            GlobalDashboardResponse.ActiviteRecente.builder()
                .nouveauxInscrits(nouveauxInscrits24h)
                .cotisationsEnregistrees(cotisations24h)
                .build())
        .build();
  }

  // ─── Gestion des utilisateurs SUPER_ADMIN ─────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<AdminUserResponse> listUsers(String keycloakId, Pageable pageable)
      throws CustomException {
    assertSuperAdmin(keycloakId);
    return userRepository.findByDeletedAtIsNull(pageable).map(AdminUserResponse::from);
  }

  @Override
  @Transactional(readOnly = true)
  public AdminUserResponse getUserDetail(String keycloakId, UUID userId) throws CustomException {
    assertSuperAdmin(keycloakId);
    User user = findUserById(userId);
    return AdminUserResponse.from(user);
  }

  @Override
  public AdminUserResponse disableUser(String keycloakId, UUID userId) throws CustomException {
    User caller = assertSuperAdmin(keycloakId);
    User target = findUserById(userId);

    if (caller.getId().equals(target.getId())) {
      throw new BadRequestException("Un SUPER_ADMIN ne peut pas désactiver son propre compte");
    }
    if (!target.isActive()) {
      throw new BadRequestException("Ce compte est déjà désactivé");
    }

    target.setActive(false);
    userRepository.save(target);

    try {
      keycloakAuthService.disableUser(target.getKeycloakId());
    } catch (Exception e) {
      log.warn("Keycloak indisponible lors de la désactivation de {} : {}", userId, e.getMessage());
    }

    log.info("Compte désactivé – userId={} par superAdmin={}", userId, caller.getId());
    return AdminUserResponse.from(target);
  }

  @Override
  public AdminUserResponse enableUser(String keycloakId, UUID userId) throws CustomException {
    assertSuperAdmin(keycloakId);
    User target = findUserById(userId);

    if (target.isActive()) {
      throw new BadRequestException("Ce compte est déjà actif");
    }

    target.setActive(true);
    userRepository.save(target);

    try {
      keycloakAuthService.enableUser(target.getKeycloakId());
    } catch (Exception e) {
      log.warn("Keycloak indisponible lors de la réactivation de {} : {}", userId, e.getMessage());
    }

    log.info("Compte réactivé – userId={}", userId);
    return AdminUserResponse.from(target);
  }

  @Override
  public AdminUserResponse updateUserRoles(
      String keycloakId, UUID userId, UpdateUserRolesRequest request) throws CustomException {

    User caller = assertSuperAdmin(keycloakId);
    User target = findUserById(userId);

    Set<UserRole> newRoles = request.getRoles();

    // USER est le rôle de base – il ne peut jamais être retiré
    if (!newRoles.contains(UserRole.USER)) {
      newRoles.add(UserRole.USER);
    }

    List<UserRoleAssignment> currentAssignments = roleRepository.findAllByUserId(target.getId());

    // Ajouter les rôles manquants
    for (UserRole role : newRoles) {
      if (!roleRepository.existsByUserIdAndRole(target.getId(), role)) {
        boolean synced = true;
        try {
          keycloakAuthService.assignRole(target.getKeycloakId(), role.getKeycloakRole());
        } catch (Exception e) {
          log.warn("Keycloak indisponible, rôle {} non synchronisé : {}", role, e.getMessage());
          synced = false;
        }
        roleRepository.save(
            UserRoleAssignment.builder().user(target).role(role).syncedToKeycloak(synced).build());
        log.info("Rôle {} ajouté à userId={}", role, userId);
      }
    }

    // Retirer les rôles non souhaités (sauf USER)
    for (UserRoleAssignment assignment : currentAssignments) {
      UserRole role = assignment.getRole();
      if (role == UserRole.USER) continue; // jamais retiré
      if (!newRoles.contains(role)) {
        try {
          keycloakAuthService.removeRole(target.getKeycloakId(), role.getKeycloakRole());
        } catch (Exception e) {
          log.warn("Keycloak indisponible, rôle {} non révoqué : {}", role, e.getMessage());
        }
        roleRepository.delete(assignment);
        log.info("Rôle {} retiré de userId={}", role, userId);
      }
    }

    // Recharger l'entité pour avoir les rôles à jour
    User refreshed =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
    return AdminUserResponse.from(refreshed);
  }

  // ─── Dashboard ADMIN (créateur tontines) ─────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public MyDashboardResponse getMyDashboard(String keycloakId) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);

    List<Tontine> mesTontines =
        tontineRepository.findByCreePar_IdAndDeletedAtIsNull(caller.getId());

    List<MyDashboardResponse.TontineStats> statsList = new ArrayList<>();

    for (Tontine tontine : mesTontines) {
      UUID tid = tontine.getId();

      int nombreMembres = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(tid);
      long enAttente =
          cotisationRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(
              tid, CotisationStatut.EN_ATTENTE);
      long enRetard =
          cotisationRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(
              tid, CotisationStatut.EN_RETARD);
      var montantValide = cotisationRepository.sumMontantValideByTontine(tid);

      // Cycle EN_COURS
      MyDashboardResponse.CycleEnCoursInfo cycleInfo = null;
      Optional<CycleTontine> cycleOpt =
          cycleRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(tid, CycleStatut.EN_COURS);
      if (cycleOpt.isPresent()) {
        CycleTontine cycle = cycleOpt.get();
        String benefNom = null;
        UUID benefUserId = null;
        if (cycle.getBeneficiaire() != null && cycle.getBeneficiaire().getUser() != null) {
          User benefUser = cycle.getBeneficiaire().getUser();
          benefNom = benefUser.getFirstName() + " " + benefUser.getLastName();
          benefUserId = benefUser.getId();
        }
        cycleInfo =
            MyDashboardResponse.CycleEnCoursInfo.builder()
                .cycleId(cycle.getId())
                .numeroCycle(cycle.getNumeroCycle())
                .dateDebut(cycle.getDateDebut())
                .dateFin(cycle.getDateFin())
                .beneficiaireNom(benefNom)
                .beneficiaireUserId(benefUserId)
                .build();
      }

      statsList.add(
          MyDashboardResponse.TontineStats.builder()
              .tontineId(tid)
              .nom(tontine.getNom())
              .statut(tontine.getStatut())
              .nombreMembres(nombreMembres)
              .cotisationsEnAttente(enAttente)
              .cotisationsEnRetard(enRetard)
              .montantTotalValide(montantValide)
              .cycleEnCours(cycleInfo)
              .build());
    }

    return MyDashboardResponse.builder()
        .nombreTontinesGerees(mesTontines.size())
        .tontines(statsList)
        .build();
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  /**
   * Vérifie que l'appelant est SUPER_ADMIN et retourne l'entité User correspondante.
   *
   * @throws ForbiddenException si l'utilisateur n'est pas SUPER_ADMIN
   */
  private User assertSuperAdmin(String keycloakId) throws CustomException {
    User user = findUserByKeycloakId(keycloakId);
    if (!roleRepository.existsByUserIdAndRole(user.getId(), UserRole.SUPER_ADMIN)) {
      throw new ForbiddenException(ResponseMessageConstants.ADMIN_ACCESS_DENIED);
    }
    return user;
  }

  private User findUserByKeycloakId(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }

  private User findUserById(UUID userId) throws CustomException {
    return userRepository
        .findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }
}
