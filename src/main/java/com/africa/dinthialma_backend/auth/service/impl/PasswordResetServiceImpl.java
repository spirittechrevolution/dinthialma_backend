package com.africa.dinthialma_backend.auth.service.impl;

import com.africa.dinthialma_backend.auth.dto.ResetPasswordByPhoneRequest;
import com.africa.dinthialma_backend.auth.dto.SendOtpRequest;
import com.africa.dinthialma_backend.auth.dto.VerifyOtpRequest;
import com.africa.dinthialma_backend.auth.entity.OtpVerification;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.OtpVerificationRepository;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.auth.service.interfaces.PasswordResetService;
import com.africa.dinthialma_backend.auth.service.interfaces.UserSessionService;
import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.util.OtpUtils;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Réinitialisation du mot de passe via OTP SMS en 3 étapes.
 *
 * <p>Anti-énumération : {@link #sendForgotPasswordOtp} répond toujours HTTP 200 pour ne pas révéler
 * si un numéro est enregistré sur la plateforme.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

  private final OtpVerificationRepository otpVerificationRepository;
  private final UserRepository userRepository;
  private final KeycloakAuthService keycloakAuthService;
  private final UserSessionService userSessionService;
  private final WhatsappService whatsappService;

  // ─── Étape 1 : Envoi OTP (anti-énumération) ────────────────────────

  @Override
  @Transactional
  public void sendForgotPasswordOtp(SendOtpRequest request) throws CustomException {
    String phone = request.getPhone().trim();
    log.debug("Demande OTP reset mot de passe pour : {}", phone);

    // Anti-énumération : ne jamais révéler si le numéro existe
    if (!userRepository.existsByPhone(phone)) {
      log.debug("Numéro {} inconnu – réponse silencieuse (anti-énumération)", phone);
      return; // HTTP 200 côté contrôleur, pas d'erreur
    }

    // Invalider les anciens OTP RESET_PASSWORD pour ce numéro
    otpVerificationRepository
        .findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            phone, Constants.OtpPurpose.RESET_PASSWORD)
        .ifPresent(
            existing -> {
              existing.setUsed(true);
              otpVerificationRepository.save(existing);
            });

    String code = OtpUtils.generateOtp();
    OtpVerification otp =
        OtpVerification.builder()
            .phone(phone)
            .code(code)
            .purpose(Constants.OtpPurpose.RESET_PASSWORD)
            .expiresAt(LocalDateTime.now().plusMinutes(Constants.Otp.EXPIRY_MINUTES))
            .used(false)
            .verified(false)
            .build();

    otpVerificationRepository.save(otp);
    whatsappService.sendOtp(phone, code);
    log.info("OTP reset mot de passe envoyé au numéro : {}", phone);
  }

  // ─── Étape 2 : Vérification OTP ───────────────────────────────────

  @Override
  @Transactional
  public void verifyForgotPasswordOtp(VerifyOtpRequest request) throws CustomException {
    String phone = request.getPhone().trim();
    log.debug("Vérification OTP reset mot de passe pour : {}", phone);

    OtpVerification otp =
        otpVerificationRepository
            .findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                phone, Constants.OtpPurpose.RESET_PASSWORD)
            .orElseThrow(() -> new BadRequestException(ResponseMessageConstants.AUTH_OTP_INVALID));

    if (otp.isExpired()) {
      log.warn("OTP reset mot de passe expiré pour : {}", phone);
      throw new BadRequestException(ResponseMessageConstants.AUTH_OTP_INVALID);
    }

    if (!otp.getCode().equals(request.getCode())) {
      log.warn("Code OTP reset mot de passe incorrect pour : {}", phone);
      throw new BadRequestException(ResponseMessageConstants.AUTH_OTP_INVALID);
    }

    otp.setVerified(true);
    otpVerificationRepository.save(otp);
    log.info("OTP reset mot de passe vérifié pour : {}", phone);
  }

  // ─── Étape 3 : Reset du mot de passe ──────────────────────────────

  @Override
  @Transactional
  public void resetPassword(ResetPasswordByPhoneRequest request) throws CustomException {
    String phone = request.getPhone().trim();
    log.debug("Reset mot de passe pour : {}", phone);

    // Vérifier que l'OTP a été validé (vérifié + pas consommé)
    OtpVerification otp =
        otpVerificationRepository
            .findTopByPhoneAndPurposeAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(
                phone, Constants.OtpPurpose.RESET_PASSWORD)
            .orElseThrow(() -> new BadRequestException(ResponseMessageConstants.AUTH_OTP_INVALID));

    // Retrouver l'utilisateur local
    User user =
        userRepository
            .findByPhone(phone)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));

    // Réinitialiser le mot de passe dans Keycloak
    keycloakAuthService.resetPassword(user.getKeycloakId(), request.getNewPassword());

    // Révoquer toutes les sessions PIN (sécurité : sessions liées à l'ancien mot de passe)
    userSessionService.revokeAllSessions(user.getId());
    log.info("Sessions PIN révoquées après reset mot de passe – userId={}", user.getId());

    // Consommer l'OTP
    otp.setUsed(true);
    otpVerificationRepository.save(otp);

    log.info("Mot de passe réinitialisé avec succès pour userId={}", user.getId());
  }
}
