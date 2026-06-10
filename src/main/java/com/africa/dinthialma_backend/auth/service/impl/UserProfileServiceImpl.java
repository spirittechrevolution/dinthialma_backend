package com.africa.dinthialma_backend.auth.service.impl;

import com.africa.dinthialma_backend.auth.config.KeycloakProperties;
import com.africa.dinthialma_backend.auth.dto.PhoneChangeRequest;
import com.africa.dinthialma_backend.auth.dto.PhoneChangeVerifyRequest;
import com.africa.dinthialma_backend.auth.dto.UpdateProfileRequest;
import com.africa.dinthialma_backend.auth.dto.UserProfileResponse;
import com.africa.dinthialma_backend.auth.entity.PhoneChangeRequestEntity;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.PhoneChangeRequestRepository;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.UserProfileService;
import com.africa.dinthialma_backend.auth.service.interfaces.UserSessionService;
import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.util.OtpUtils;
import com.africa.dinthialma_backend.common.util.PhoneUtils;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion du profil utilisateur : lecture, modification, changement de numéro.
 *
 * <p>Le changement de numéro met à jour :
 *
 * <ol>
 *   <li>Keycloak : username + attribut {@code phone}
 *   <li>Base locale : {@code users.phone}
 *   <li>Sessions : toutes révoquées → re-login obligatoire
 * </ol>
 *
 * Les tontines/memberships sont liés à l'UUID {@code users.id} → aucune relation cassée.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

  private final UserRepository userRepository;
  private final PhoneChangeRequestRepository phoneChangeRequestRepository;
  private final UserSessionService userSessionService;
  private final WhatsappService whatsappService;
  private final Keycloak keycloakAdminClient;
  private final KeycloakProperties keycloakProperties;

  // ─── Profil ───────────────────────────────────────────────────────────

  @Override
  public UserProfileResponse getProfile(String keycloakId) throws CustomException {
    User user = findByKeycloakId(keycloakId);
    log.debug("[Profile] Lecture profil – userId={}", user.getId());
    return UserProfileResponse.from(user);
  }

  @Override
  @Transactional
  public UserProfileResponse updateProfile(String keycloakId, UpdateProfileRequest request)
      throws CustomException {
    User user = findByKeycloakId(keycloakId);

    // Vérifier unicité email si modifié
    if (request.getEmail() != null
        && !request.getEmail().isBlank()
        && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
      if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
        throw new ConflictException("Cette adresse email est déjà utilisée");
      }
    }

    // Mise à jour Keycloak (non bloquant)
    updateKeycloakProfile(keycloakId, request);

    // Mise à jour locale
    user.setFirstName(request.getFirstName().trim());
    user.setLastName(request.getLastName().trim());
    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      user.setEmail(request.getEmail().trim().toLowerCase());
    }
    User saved = userRepository.save(user);

    log.info("[Profile] Profil mis à jour – userId={}", saved.getId());
    return UserProfileResponse.from(saved);
  }

  // ─── Changement de numéro ────────────────────────────────────────────

  @Override
  @Transactional
  public void requestPhoneChange(String keycloakId, PhoneChangeRequest request)
      throws CustomException {
    User user = findByKeycloakId(keycloakId);
    String newPhone = PhoneUtils.normalize(request.getNewPhone());

    if (newPhone.equals(user.getPhone())) {
      throw new BadRequestException("Le nouveau numéro est identique au numéro actuel");
    }

    if (userRepository.existsByPhone(newPhone)) {
      throw new ConflictException("Ce numéro de téléphone est déjà utilisé par un autre compte");
    }

    // Supprimer les demandes précédentes de cet utilisateur
    phoneChangeRequestRepository.deleteAllByUserId(user.getId());

    String otp = OtpUtils.generateOtp();
    PhoneChangeRequestEntity changeReq =
        PhoneChangeRequestEntity.builder()
            .user(user)
            .oldPhone(user.getPhone())
            .newPhone(newPhone)
            .otpCode(otp)
            .verified(false)
            .expiresAt(LocalDateTime.now().plusMinutes(Constants.Otp.EXPIRY_MINUTES))
            .build();

    phoneChangeRequestRepository.save(changeReq);
    whatsappService.sendOtp(newPhone, otp);

    log.info("[Profile] OTP changement numéro envoyé – userId={} → {}", user.getId(), newPhone);
  }

  @Override
  @Transactional
  public void verifyPhoneChange(String keycloakId, PhoneChangeVerifyRequest request)
      throws CustomException {
    User user = findByKeycloakId(keycloakId);
    String newPhone = PhoneUtils.normalize(request.getNewPhone());

    PhoneChangeRequestEntity changeReq =
        phoneChangeRequestRepository
            .findTopByUserIdAndNewPhoneAndVerifiedFalseOrderByCreatedAtDesc(user.getId(), newPhone)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Aucune demande de changement en cours pour ce numéro"));

    if (changeReq.isExpired()) {
      throw new BadRequestException("Le code OTP a expiré. Veuillez faire une nouvelle demande");
    }

    if (!changeReq.getOtpCode().equals(request.getCode())) {
      throw new BadRequestException("Code OTP incorrect");
    }

    String oldPhone = user.getPhone();

    // 1. Mettre à jour Keycloak (username + attribut phone) — bloquant
    updateKeycloakPhone(user.getKeycloakId(), newPhone);

    // 2. Mettre à jour la base locale
    user.setPhone(newPhone);
    userRepository.save(user);

    // 3. Révoquer toutes les sessions → re-login obligatoire
    userSessionService.revokeAllSessions(user.getId());

    // 4. Marquer la demande comme traitée
    changeReq.setVerified(true);
    phoneChangeRequestRepository.save(changeReq);

    log.info("[Profile] Numéro changé – userId={} {} → {}", user.getId(), oldPhone, newPhone);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────

  private User findByKeycloakId(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
  }

  /** Mise à jour firstName/lastName/email dans Keycloak. Non bloquant si Keycloak indisponible. */
  private void updateKeycloakProfile(String keycloakId, UpdateProfileRequest request) {
    try {
      UserRepresentation userRep =
          keycloakAdminClient
              .realm(keycloakProperties.getRealm())
              .users()
              .get(keycloakId)
              .toRepresentation();

      userRep.setFirstName(request.getFirstName().trim());
      userRep.setLastName(request.getLastName().trim());
      if (request.getEmail() != null && !request.getEmail().isBlank()) {
        userRep.setEmail(request.getEmail().trim().toLowerCase());
      }

      keycloakAdminClient
          .realm(keycloakProperties.getRealm())
          .users()
          .get(keycloakId)
          .update(userRep);

      log.debug("[Profile] Keycloak profil mis à jour – keycloakId={}", keycloakId);
    } catch (Exception e) {
      log.warn("[Profile] Keycloak non mis à jour (non bloquant) : {}", e.getMessage());
    }
  }

  /** Mise à jour username + attribut phone dans Keycloak. Bloquant (lève si échec). */
  private void updateKeycloakPhone(String keycloakId, String newPhone) {
    try {
      UserRepresentation userRep =
          keycloakAdminClient
              .realm(keycloakProperties.getRealm())
              .users()
              .get(keycloakId)
              .toRepresentation();

      userRep.setUsername(newPhone);
      userRep.setAttributes(Map.of("phone", List.of(newPhone)));

      keycloakAdminClient
          .realm(keycloakProperties.getRealm())
          .users()
          .get(keycloakId)
          .update(userRep);

      log.debug("[Profile] Keycloak username/phone mis à jour → {}", newPhone);
    } catch (Exception e) {
      log.error("[Profile] Échec mise à jour Keycloak phone : {}", e.getMessage());
      throw new RuntimeException("Impossible de mettre à jour le numéro dans l'IAM", e);
    }
  }
}
