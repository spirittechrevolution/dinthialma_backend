package com.africa.dinthialma_backend.auth.service.impl;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.dto.RegisterCompleteRequest;
import com.africa.dinthialma_backend.auth.dto.RegisterResponse;
import com.africa.dinthialma_backend.auth.dto.SendOtpRequest;
import com.africa.dinthialma_backend.auth.dto.VerifyOtpRequest;
import com.africa.dinthialma_backend.auth.entity.OtpVerification;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserRoleAssignment;
import com.africa.dinthialma_backend.auth.repository.OtpVerificationRepository;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.auth.service.interfaces.RegistrationService;
import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.util.OtpUtils;
import com.africa.dinthialma_backend.common.util.PhoneUtils;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service d'inscription en 3 étapes :
 *
 * <ol>
 *   <li>Envoi OTP SMS
 *   <li>Vérification OTP
 *   <li>Complétion (création Keycloak + base locale)
 * </ol>
 *
 * <p>Cas particulier : si le numéro appartient à un compte {@link AccountStatus#PRE_ENROLLED} (créé
 * par un gestionnaire de tontine), les étapes s'enchaînent normalement mais la complétion active le
 * compte existant au lieu d'en créer un nouveau.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

  private final OtpVerificationRepository otpVerificationRepository;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository userRoleAssignmentRepository;
  private final KeycloakAuthService keycloakAuthService;
  private final WhatsappService whatsappService;

  // ─── Étape 1 : Envoi OTP ──────────────────────────────────────────

  @Override
  @Transactional
  public void sendOtp(SendOtpRequest request) throws CustomException {
    String phone = PhoneUtils.normalize(request.getPhone());
    log.debug("Envoi OTP inscription pour : {}", phone);

    Optional<User> existing = userRepository.findByPhone(phone);
    if (existing.isPresent()) {
      AccountStatus status = existing.get().getAccountStatus();
      // Compte PRE_ENROLLED : autoriser l'inscription pour activer le compte
      if (status == null || status == AccountStatus.ACTIVE) {
        log.warn("Tentative d'inscription avec un téléphone déjà utilisé : {}", phone);
        throw new ConflictException(ResponseMessageConstants.AUTH_PHONE_ALREADY_USED);
      }
    }

    otpVerificationRepository.deleteAllByPhoneAndPurpose(phone, Constants.OtpPurpose.REGISTRATION);

    String code = OtpUtils.generateOtp();
    OtpVerification otp =
        OtpVerification.builder()
            .phone(phone)
            .code(code)
            .purpose(Constants.OtpPurpose.REGISTRATION)
            .expiresAt(LocalDateTime.now().plusMinutes(Constants.Otp.EXPIRY_MINUTES))
            .used(false)
            .verified(false)
            .build();

    otpVerificationRepository.save(otp);
    whatsappService.sendOtp(phone, code);

    log.info("OTP inscription envoyé au numéro : {}", phone);
  }

  // ─── Étape 2 : Vérification OTP ───────────────────────────────────

  @Override
  @Transactional
  public void verifyOtp(VerifyOtpRequest request) throws CustomException {
    String phone = PhoneUtils.normalize(request.getPhone());
    log.debug("Vérification OTP inscription pour : {}", phone);

    OtpVerification otp =
        otpVerificationRepository
            .findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                phone, Constants.OtpPurpose.REGISTRATION)
            .orElseThrow(() -> new BadRequestException(ResponseMessageConstants.AUTH_OTP_INVALID));

    if (otp.isExpired()) {
      log.warn("OTP expiré pour le numéro : {}", phone);
      throw new BadRequestException(ResponseMessageConstants.AUTH_OTP_INVALID);
    }

    if (!otp.getCode().equals(request.getCode())) {
      log.warn("Code OTP incorrect pour le numéro : {}", phone);
      throw new BadRequestException(ResponseMessageConstants.AUTH_OTP_INVALID);
    }

    otp.setVerified(true);
    otpVerificationRepository.save(otp);
    log.info("OTP inscription vérifié pour le numéro : {}", phone);
  }

  // ─── Étape 3 : Complétion de l'inscription ────────────────────────

  @Override
  @Transactional
  public RegisterResponse completeRegistration(RegisterCompleteRequest request)
      throws CustomException {
    String phone = PhoneUtils.normalize(request.getPhone());
    log.debug("Complétion de l'inscription pour : {}", phone);

    OtpVerification otp =
        otpVerificationRepository
            .findTopByPhoneAndPurposeAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(
                phone, Constants.OtpPurpose.REGISTRATION)
            .orElseThrow(
                () -> new BadRequestException(ResponseMessageConstants.AUTH_OTP_NOT_VERIFIED));

    Optional<User> existing = userRepository.findByPhone(phone);
    if (existing.isPresent()) {
      User user = existing.get();
      AccountStatus status = user.getAccountStatus();
      if (status == null || status == AccountStatus.ACTIVE) {
        throw new ConflictException(ResponseMessageConstants.AUTH_PHONE_ALREADY_USED);
      }
      // Compte PRE_ENROLLED : activation
      return activatePreEnrolledUser(user, request, phone, otp);
    }

    // ── Nouvel utilisateur ────────────────────────────────────────────
    request.setPhone(phone);
    String keycloakId = keycloakAuthService.createUser(request);

    boolean keycloakSynced = true;
    try {
      keycloakAuthService.assignRole(keycloakId, UserRole.USER.getKeycloakRole());
    } catch (Exception e) {
      log.warn(
          "Impossible de synchroniser le rôle USER dans Keycloak pour keycloakId={} : {}",
          keycloakId,
          e.getMessage());
      keycloakSynced = false;
    }

    User user =
        User.builder()
            .keycloakId(keycloakId)
            .firstName(request.getFirstName().trim())
            .lastName(request.getLastName().trim())
            .phone(phone)
            .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
            .active(true)
            .accountStatus(AccountStatus.ACTIVE)
            .build();

    User savedUser = userRepository.save(user);

    UserRoleAssignment roleAssignment =
        UserRoleAssignment.builder()
            .user(savedUser)
            .role(UserRole.USER)
            .syncedToKeycloak(keycloakSynced)
            .build();
    userRoleAssignmentRepository.save(roleAssignment);

    otp.setUsed(true);
    otpVerificationRepository.save(otp);

    log.info(
        "Inscription complétée – userId={} keycloakId={} phone={}",
        savedUser.getId(),
        keycloakId,
        phone);

    return buildResponse(savedUser, UserRole.USER.getKeycloakRole());
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  /**
   * Active un compte PRE_ENROLLED : crée le compte Keycloak, assigne les rôles USER + MEMBER
   * (conserve les tontines existantes), et marque le compte ACTIVE.
   */
  private RegisterResponse activatePreEnrolledUser(
      User user, RegisterCompleteRequest request, String phone, OtpVerification otp)
      throws CustomException {

    request.setPhone(phone);
    String keycloakId = keycloakAuthService.createUser(request);

    boolean keycloakSynced = true;
    try {
      keycloakAuthService.assignRole(keycloakId, UserRole.USER.getKeycloakRole());
      keycloakAuthService.assignRole(keycloakId, UserRole.MEMBER.getKeycloakRole());
    } catch (Exception e) {
      log.warn(
          "Impossible de synchroniser les rôles dans Keycloak pour keycloakId={} : {}",
          keycloakId,
          e.getMessage());
      keycloakSynced = false;
    }

    user.setKeycloakId(keycloakId);
    user.setFirstName(request.getFirstName().trim());
    user.setLastName(request.getLastName().trim());
    user.setEmail(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null);
    user.setAccountStatus(AccountStatus.ACTIVE);
    User savedUser = userRepository.save(user);

    assignRoleIfAbsent(savedUser, UserRole.USER, keycloakSynced);
    assignRoleIfAbsent(savedUser, UserRole.MEMBER, keycloakSynced);

    otp.setUsed(true);
    otpVerificationRepository.save(otp);

    log.info(
        "Compte PRE_ENROLLED activé – userId={} keycloakId={} phone={}",
        savedUser.getId(),
        keycloakId,
        phone);

    return buildResponse(savedUser, UserRole.MEMBER.getKeycloakRole());
  }

  private void assignRoleIfAbsent(User user, UserRole role, boolean keycloakSynced) {
    if (userRoleAssignmentRepository.existsByUserIdAndRole(user.getId(), role)) return;
    UserRoleAssignment assignment =
        UserRoleAssignment.builder().user(user).role(role).syncedToKeycloak(keycloakSynced).build();
    userRoleAssignmentRepository.save(assignment);
  }

  private RegisterResponse buildResponse(User user, String role) {
    return new RegisterResponse(
        user.getId(),
        user.getKeycloakId(),
        user.getPhone(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        role);
  }
}
