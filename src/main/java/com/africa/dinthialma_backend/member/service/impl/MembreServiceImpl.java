package com.africa.dinthialma_backend.member.service.impl;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserRoleAssignment;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.util.PhoneUtils;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.dto.AddMembreRequest;
import com.africa.dinthialma_backend.member.dto.MembreResponse;
import com.africa.dinthialma_backend.member.dto.UpdateMembreStatutRequest;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.member.service.interfaces.MembreService;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final AuditService auditService;
  private final WhatsappService whatsappService;

  // ─── Ajout d'un membre ───────────────────────────────────────────────────

  @Override
  public MembreResponse addMembre(String keycloakId, UUID tontineId, AddMembreRequest request)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    String phone = PhoneUtils.normalize(request.getPhone());

    // Résoudre ou créer l'utilisateur cible
    User targetUser;
    var existingUser = userRepository.findByPhoneAndDeletedAtIsNull(phone);
    if (existingUser.isPresent()) {
      targetUser = enrichPreEnrolledIfNeeded(existingUser.get(), request);
    } else {
      // Numéro inconnu : firstName et lastName obligatoires pour créer le compte PRE_ENROLLED
      if (request.getFirstName() == null
          || request.getFirstName().isBlank()
          || request.getLastName() == null
          || request.getLastName().isBlank()) {
        throw new BadRequestException(ResponseMessageConstants.MEMBER_NAME_REQUIRED);
      }
      targetUser =
          createPreEnrolledUser(
              phone, request.getFirstName().trim(), request.getLastName().trim(), tontine);
    }

    // Unicité : un utilisateur ne peut cotiser qu'une fois par tontine
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

    // Les comptes PRE_ENROLLED n'ont pas de Keycloak — on n'assigne pas de rôle maintenant.
    // Le rôle MEMBER sera attribué lors de l'activation du compte (completeRegistration).
    if (targetUser.getAccountStatus() != AccountStatus.PRE_ENROLLED) {
      assignRoleIfAbsent(targetUser, UserRole.MEMBER);
    }

    auditService.logCreate(caller, "tontine_membres", saved.getId());
    log.info(
        "Membre ajouté – tontineId={} userId={} membreId={} preEnrolled={}",
        tontineId,
        targetUser.getId(),
        saved.getId(),
        targetUser.getAccountStatus() == AccountStatus.PRE_ENROLLED);

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
  public Page<MembreResponse> listMembres(String keycloakId, UUID tontineId, Pageable pageable)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertCanAccess(caller, tontine);

    return membreRepository
        .findByTontine_IdAndDeletedAtIsNull(tontineId, pageable)
        .map(MembreResponse::from);
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

    String oldStatut = membre.getStatut().name();
    membre.setStatut(request.getStatut());
    TontineMembre saved = membreRepository.save(membre);

    auditService.log(
        caller, "tontine_membres", membreId, "statut", oldStatut, request.getStatut().name());
    log.info("Statut membre modifié – membreId={} statut={}", membreId, request.getStatut());
    return MembreResponse.from(saved);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  /**
   * Si l'utilisateur trouvé est PRE_ENROLLED et que son nom est encore vide, on le complète avec
   * les informations fournies par l'admin. Un compte ACTIVE n'est jamais modifié ici.
   */
  private User enrichPreEnrolledIfNeeded(User user, AddMembreRequest request) {
    if (user.getAccountStatus() != AccountStatus.PRE_ENROLLED) return user;
    boolean needsUpdate = false;
    if (user.getFirstName().isBlank()) {
      user.setFirstName(request.getFirstName().trim());
      needsUpdate = true;
    }
    if (user.getLastName().isBlank()) {
      user.setLastName(request.getLastName().trim());
      needsUpdate = true;
    }
    return needsUpdate ? userRepository.save(user) : user;
  }

  /**
   * Crée un compte pré-inscrit pour un numéro non encore enregistré. Le nom et prénom sont fournis
   * par l'admin pour l'affichage immédiat dans la tontine. Envoie un SMS d'invitation (non
   * bloquant).
   */
  private User createPreEnrolledUser(
      String phone, String firstName, String lastName, Tontine tontine) {
    User user =
        User.builder()
            .keycloakId(null)
            .firstName(firstName)
            .lastName(lastName)
            .phone(phone)
            .active(true)
            .accountStatus(AccountStatus.PRE_ENROLLED)
            .build();

    User saved = userRepository.save(user);
    log.info("Compte pré-inscrit créé – phone={} userId={}", phone, saved.getId());

    try {
      whatsappService.sendTontineInvite(phone, tontine.getNom());
    } catch (Exception e) {
      log.warn("SMS invitation non envoyé pour {} : {}", phone, e.getMessage());
    }

    return saved;
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
