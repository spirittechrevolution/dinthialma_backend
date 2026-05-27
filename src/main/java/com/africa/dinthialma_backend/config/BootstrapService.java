package com.africa.dinthialma_backend.config;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.dto.RegisterCompleteRequest;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserRoleAssignment;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.common.util.PhoneUtils;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crée automatiquement le premier Super Admin au démarrage si aucun n'existe en base.
 *
 * <p>Comportement (idempotent) :
 *
 * <ol>
 *   <li>{@code BOOTSTRAP_ENABLED=false} → ignoré
 *   <li>Un {@code SUPER_ADMIN} existe déjà → ignoré
 *   <li>Sinon → création Keycloak + rôles + persistance locale
 * </ol>
 *
 * <p>Désactiver via {@code BOOTSTRAP_ENABLED=false} après le premier login.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapService implements ApplicationRunner {

  private final BootstrapProperties bootstrapProperties;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository userRoleAssignmentRepository;
  private final KeycloakAuthService keycloakAuthService;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {

    if (!bootstrapProperties.isEnabled()) {
      log.debug("[Bootstrap] Désactivé (BOOTSTRAP_ENABLED=false) — ignoré.");
      return;
    }

    long existingSuperAdmins = userRepository.findAdminUsers(Set.of(UserRole.SUPER_ADMIN)).size();

    if (existingSuperAdmins > 0) {
      log.info(
          "[Bootstrap] {} SUPER_ADMIN détecté(s) en base — initialisation ignorée.",
          existingSuperAdmins);
      return;
    }

    String phone = PhoneUtils.normalize(bootstrapProperties.getPhone());

    log.info(
        "[Bootstrap] Aucun SUPER_ADMIN en base. Création du compte bootstrap : phone={}", phone);

    try {
      // ── 1. Créer dans Keycloak ─────────────────────────────────
      RegisterCompleteRequest req = new RegisterCompleteRequest();
      req.setPhone(phone);
      req.setFirstName(bootstrapProperties.getFirstName());
      req.setLastName(bootstrapProperties.getLastName());
      req.setEmail(bootstrapProperties.getEmail());
      req.setPassword(bootstrapProperties.getPassword());

      String keycloakId = keycloakAuthService.createUser(req);

      // ── 2. Attribuer les rôles dans Keycloak ───────────────────
      boolean superAdminSynced = assignKeycloakRole(keycloakId, UserRole.SUPER_ADMIN);
      boolean userSynced = assignKeycloakRole(keycloakId, UserRole.USER);

      // ── 3. Persister en base locale ────────────────────────────
      User superAdmin =
          User.builder()
              .keycloakId(keycloakId)
              .firstName(bootstrapProperties.getFirstName())
              .lastName(bootstrapProperties.getLastName())
              .phone(phone)
              .email(bootstrapProperties.getEmail())
              .active(true)
              .build();

      User saved = userRepository.save(superAdmin);

      userRoleAssignmentRepository.save(
          UserRoleAssignment.builder()
              .user(saved)
              .role(UserRole.SUPER_ADMIN)
              .syncedToKeycloak(superAdminSynced)
              .build());

      userRoleAssignmentRepository.save(
          UserRoleAssignment.builder()
              .user(saved)
              .role(UserRole.USER)
              .syncedToKeycloak(userSynced)
              .build());

      // ── 4. Confirmation ────────────────────────────────────────
      log.info("═══════════════════════════════════════════════════════════");
      log.info("[Bootstrap] Compte SUPER_ADMIN créé avec succès.");
      log.info("[Bootstrap]   userId     : {}", saved.getId());
      log.info("[Bootstrap]   keycloakId : {}", keycloakId);
      log.info("[Bootstrap]   téléphone  : {}", phone);
      log.info("[Bootstrap]   email      : {}", bootstrapProperties.getEmail());
      log.info("[Bootstrap]   ⚠️  Changez le mot de passe dès le premier login !");
      log.info("[Bootstrap]   → Pensez à désactiver BOOTSTRAP_ENABLED après connexion.");
      log.info("═══════════════════════════════════════════════════════════");

    } catch (Exception e) {
      log.error("═══════════════════════════════════════════════════════════");
      log.error("[Bootstrap] ÉCHEC de la création du compte SUPER_ADMIN.");
      log.error("[Bootstrap]   Cause : {}", e.getMessage());
      log.error(
          "[Bootstrap]   → Vérifiez que Keycloak est démarré et que le realm '{}' existe.",
          "dinthialma");
      log.error(
          "[Bootstrap]   → L'application continue — relancez avec BOOTSTRAP_ENABLED=true après correction.");
      log.error("═══════════════════════════════════════════════════════════");
    }
  }

  private boolean assignKeycloakRole(String keycloakId, UserRole role) {
    try {
      keycloakAuthService.assignRole(keycloakId, role.getKeycloakRole());
      log.info("[Bootstrap] Rôle '{}' attribué dans Keycloak", role.getKeycloakRole());
      return true;
    } catch (Exception e) {
      log.warn(
          "[Bootstrap] Impossible d'attribuer le rôle '{}' : {}",
          role.getKeycloakRole(),
          e.getMessage());
      return false;
    }
  }
}
