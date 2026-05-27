package com.africa.dinthialma_backend.auth.service.impl;

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
import com.africa.dinthialma_backend.notification.service.SmsService;
import java.time.LocalDateTime;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

  private final OtpVerificationRepository otpVerificationRepository;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository userRoleAssignmentRepository;
  private final KeycloakAuthService keycloakAuthService;
  private final SmsService smsService;

  // ─── Étape 1 : Envoi OTP ──────────────────────────────────────────

  @Override
  @Transactional
  public void sendOtp(SendOtpRequest request) throws CustomException {
    String phone = PhoneUtils.normalize(request.getPhone());
    log.debug("Envoi OTP inscription pour : {}", phone);

    if (userRepository.existsByPhone(phone)) {
      log.warn("Tentative d'inscription avec un téléphone déjà utilisé : {}", phone);
      throw new ConflictException(ResponseMessageConstants.AUTH_PHONE_ALREADY_USED);
    }

    // Invalider les anciens OTP REGISTRATION en attente pour ce numéro
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
    smsService.sendOtp(phone, code);

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

    // Double vérification du téléphone
    if (userRepository.existsByPhone(phone)) {
      throw new ConflictException(ResponseMessageConstants.AUTH_PHONE_ALREADY_USED);
    }

    // Vérifier que l'OTP a bien été validé (vérifié + pas consommé)
    OtpVerification otp =
        otpVerificationRepository
            .findTopByPhoneAndPurposeAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(
                phone, Constants.OtpPurpose.REGISTRATION)
            .orElseThrow(
                () -> new BadRequestException(ResponseMessageConstants.AUTH_OTP_NOT_VERIFIED));

    // Créer l'utilisateur dans Keycloak
    String keycloakId = keycloakAuthService.createUser(request);

    // Attribuer le rôle USER dans Keycloak (rôle de base pour tous les inscrits)
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

    // Créer l'entité User locale
    User user =
        User.builder()
            .keycloakId(keycloakId)
            .firstName(request.getFirstName().trim())
            .lastName(request.getLastName().trim())
            .phone(phone)
            .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
            .active(true)
            .build();

    User savedUser = userRepository.save(user);

    // Créer l'attribution du rôle USER en base locale
    UserRoleAssignment roleAssignment =
        UserRoleAssignment.builder()
            .user(savedUser)
            .role(UserRole.USER)
            .syncedToKeycloak(keycloakSynced)
            .build();

    userRoleAssignmentRepository.save(roleAssignment);

    // Consommer l'OTP
    otp.setUsed(true);
    otpVerificationRepository.save(otp);

    log.info(
        "Inscription complétée – userId={} keycloakId={} phone={}",
        savedUser.getId(),
        keycloakId,
        phone);

    return new RegisterResponse(
        savedUser.getId(),
        keycloakId,
        savedUser.getPhone(),
        savedUser.getFirstName(),
        savedUser.getLastName(),
        savedUser.getEmail(),
        UserRole.USER.getKeycloakRole());
  }
}
