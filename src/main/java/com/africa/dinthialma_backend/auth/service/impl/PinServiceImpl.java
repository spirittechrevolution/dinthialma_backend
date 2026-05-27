package com.africa.dinthialma_backend.auth.service.impl;

import com.africa.dinthialma_backend.auth.dto.LoginResponse;
import com.africa.dinthialma_backend.auth.dto.PinLoginRequest;
import com.africa.dinthialma_backend.auth.dto.PinResetRequest;
import com.africa.dinthialma_backend.auth.dto.PinSetupRequest;
import com.africa.dinthialma_backend.auth.entity.OtpVerification;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserSession;
import com.africa.dinthialma_backend.auth.repository.OtpVerificationRepository;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.auth.service.interfaces.PinService;
import com.africa.dinthialma_backend.auth.service.interfaces.UserSessionService;
import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.exception.TooManyRequestsException;
import com.africa.dinthialma_backend.common.exception.UnAuthorizedException;
import com.africa.dinthialma_backend.common.util.OtpUtils;
import com.africa.dinthialma_backend.notification.service.SmsService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion du code PIN – configuration, connexion rapide et réinitialisation.
 *
 * <p>Sécurité :
 *
 * <ul>
 *   <li>Argon2id pour le hachage (résistant aux attaques GPU et side-channel)
 *   <li>Verrouillage après {@code MAX_ATTEMPTS} tentatives incorrectes consécutives
 *   <li>Liste noire des PINs triviaux
 *   <li>Sessions révoquées lors du reset PIN
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PinServiceImpl implements PinService {

  private final UserRepository userRepository;
  private final OtpVerificationRepository otpVerificationRepository;
  private final UserSessionService userSessionService;
  private final KeycloakAuthService keycloakAuthService;
  private final SmsService smsService;

  /**
   * Encodeur Argon2id – paramètres renforcés pour les PINs courts (6 chiffres). saltLength=16,
   * hashLength=32, parallelism=1, memory=65536 (64 MB), iterations=3
   */
  private static final Argon2PasswordEncoder ARGON2 =
      new Argon2PasswordEncoder(16, 32, 1, 65536, 3);

  // ─── Configuration du PIN ─────────────────────────────────────────

  @Override
  @Transactional
  public void setupPin(String keycloakId, PinSetupRequest request) throws CustomException {
    log.debug("Configuration PIN pour keycloakId={}", keycloakId);

    User user = findUserByKeycloakId(keycloakId);

    validatePinFormat(request.getPin());

    if (!request.getPin().equals(request.getConfirmPin())) {
      throw new BadRequestException(ResponseMessageConstants.AUTH_PIN_MISMATCH);
    }

    String pinHash = ARGON2.encode(request.getPin());
    user.setPinHash(pinHash);
    user.setPinAttempts(0);
    user.setPinLockedUntil(null);
    userRepository.save(user);

    log.info("Code PIN configuré pour userId={}", user.getId());
  }

  // ─── Connexion PIN ────────────────────────────────────────────────

  @Override
  @Transactional
  public LoginResponse loginWithPin(PinLoginRequest request) throws CustomException {
    String phone = request.getPhone().trim();
    log.debug(
        "Tentative de connexion PIN pour phone={} clientType={}", phone, request.getClientType());

    User user =
        userRepository
            .findByPhone(phone)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));

    // Vérifier que le compte est actif
    if (!user.isActive()) {
      log.warn("Tentative de connexion PIN sur compte désactivé – userId={}", user.getId());
      throw new UnAuthorizedException(ResponseMessageConstants.AUTH_ACCOUNT_DISABLED);
    }

    // Vérifier que le PIN est configuré
    if (user.getPinHash() == null) {
      throw new BadRequestException(ResponseMessageConstants.AUTH_PIN_NOT_CONFIGURED);
    }

    // Vérifier le verrouillage
    if (user.getPinLockedUntil() != null
        && LocalDateTime.now().isBefore(user.getPinLockedUntil())) {
      long minutesLeft =
          ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getPinLockedUntil()) + 1;
      log.warn("PIN verrouillé pour userId={} – encore {} min", user.getId(), minutesLeft);
      throw new TooManyRequestsException(
          String.format(ResponseMessageConstants.AUTH_PIN_LOCKED, minutesLeft));
    }

    // Valider le PIN
    if (!ARGON2.matches(request.getPin(), user.getPinHash())) {
      handlePinFailure(user);
      return null; // jamais atteint – handlePinFailure lève toujours une exception
    }

    // PIN valide → remettre les compteurs à zéro
    user.setPinAttempts(0);
    user.setPinLockedUntil(null);

    // Récupérer la session active pour ce type de client
    Optional<UserSession> sessionOpt =
        userSessionService.findActiveSession(user.getId(), request.getClientType());

    if (sessionOpt.isEmpty()) {
      log.warn(
          "Aucune session active pour userId={} clientType={}",
          user.getId(),
          request.getClientType());
      throw new UnAuthorizedException(ResponseMessageConstants.AUTH_SESSION_NOT_FOUND);
    }

    UserSession session = sessionOpt.get();

    // Déchiffrer le refresh token et appeler Keycloak
    String refreshToken = userSessionService.decryptRefreshToken(session);
    LoginResponse newTokens = keycloakAuthService.refreshToken(refreshToken);

    // Mettre à jour la session avec le nouveau refresh token et renouveler le TTL
    userSessionService.createOrUpdateSession(
        user, request.getClientType(), newTokens.getRefreshToken(), request.getDeviceInfo());

    userRepository.save(user);

    log.info(
        "Connexion PIN réussie – userId={} clientType={}", user.getId(), request.getClientType());
    return newTokens;
  }

  // ─── Réinitialisation PIN via OTP ─────────────────────────────────

  @Override
  @Transactional
  public void sendResetPinOtp(String phone) throws CustomException {
    phone = phone.trim();
    log.debug("Envoi OTP reset PIN pour : {}", phone);

    if (!userRepository.existsByPhone(phone)) {
      // Anti-énumération : réponse silencieuse
      log.debug("Numéro {} inconnu – réponse silencieuse (anti-énumération PIN)", phone);
      return;
    }

    // Invalider l'OTP RESET_PIN précédent
    otpVerificationRepository
        .findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            phone, Constants.OtpPurpose.RESET_PIN)
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
            .purpose(Constants.OtpPurpose.RESET_PIN)
            .expiresAt(LocalDateTime.now().plusMinutes(Constants.Otp.EXPIRY_MINUTES))
            .used(false)
            .verified(false)
            .build();

    otpVerificationRepository.save(otp);
    smsService.sendOtp(phone, code);
    log.info("OTP reset PIN envoyé au numéro : {}", phone);
  }

  @Override
  @Transactional
  public void verifyResetPinOtp(String phone, String code) throws CustomException {
    phone = phone.trim();
    log.debug("Vérification OTP reset PIN pour : {}", phone);

    OtpVerification otp =
        otpVerificationRepository
            .findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                phone, Constants.OtpPurpose.RESET_PIN)
            .orElseThrow(() -> new BadRequestException(ResponseMessageConstants.AUTH_OTP_INVALID));

    if (otp.isExpired() || !otp.getCode().equals(code)) {
      log.warn("OTP reset PIN invalide ou expiré pour : {}", phone);
      throw new BadRequestException(ResponseMessageConstants.AUTH_OTP_INVALID);
    }

    otp.setVerified(true);
    otpVerificationRepository.save(otp);
    log.info("OTP reset PIN vérifié pour : {}", phone);
  }

  @Override
  @Transactional
  public void resetPin(PinResetRequest request) throws CustomException {
    String phone = request.getPhone().trim();
    log.debug("Reset PIN pour : {}", phone);

    // Vérifier l'OTP vérifié non consommé
    OtpVerification otp =
        otpVerificationRepository
            .findTopByPhoneAndPurposeAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(
                phone, Constants.OtpPurpose.RESET_PIN)
            .orElseThrow(() -> new BadRequestException(ResponseMessageConstants.AUTH_OTP_INVALID));

    User user =
        userRepository
            .findByPhone(phone)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));

    validatePinFormat(request.getNewPin());

    if (!request.getNewPin().equals(request.getConfirmPin())) {
      throw new BadRequestException(ResponseMessageConstants.AUTH_PIN_MISMATCH);
    }

    // Mettre à jour le PIN
    user.setPinHash(ARGON2.encode(request.getNewPin()));
    user.setPinAttempts(0);
    user.setPinLockedUntil(null);
    userRepository.save(user);

    // Révoquer toutes les sessions (sécurité post-reset)
    userSessionService.revokeAllSessions(user.getId());
    log.info("Sessions révoquées après reset PIN – userId={}", user.getId());

    // Consommer l'OTP
    otp.setUsed(true);
    otpVerificationRepository.save(otp);

    log.info("PIN réinitialisé avec succès pour userId={}", user.getId());
  }

  // ─── Vérification configuration PIN ──────────────────────────────

  @Override
  public boolean isPinConfigured(String keycloakId) throws CustomException {
    User user = findUserByKeycloakId(keycloakId);
    return user.getPinHash() != null;
  }

  // ─── Helpers privés ───────────────────────────────────────────────

  /** Incrémente les tentatives échouées et verrouille si nécessaire. */
  private void handlePinFailure(User user) throws CustomException {
    int newAttempts = user.getPinAttempts() + 1;
    user.setPinAttempts(newAttempts);

    if (newAttempts >= Constants.Pin.MAX_ATTEMPTS) {
      LocalDateTime lockUntil =
          LocalDateTime.now().plusMinutes(Constants.Pin.LOCK_DURATION_MINUTES);
      user.setPinLockedUntil(lockUntil);
      user.setPinAttempts(0);
      userRepository.save(user);

      log.warn(
          "PIN verrouillé pour userId={} jusqu'à {} – {} tentatives échouées",
          user.getId(),
          lockUntil,
          Constants.Pin.MAX_ATTEMPTS);

      throw new TooManyRequestsException(
          String.format(
              ResponseMessageConstants.AUTH_PIN_LOCKED, Constants.Pin.LOCK_DURATION_MINUTES));
    }

    userRepository.save(user);
    log.warn(
        "Tentative PIN incorrecte – userId={} attempt={}/{}",
        user.getId(),
        newAttempts,
        Constants.Pin.MAX_ATTEMPTS);
    throw new UnAuthorizedException(ResponseMessageConstants.AUTH_PIN_INVALID);
  }

  /** Valide le format du PIN : 6 chiffres, non trivial. */
  private void validatePinFormat(String pin) throws CustomException {
    if (pin == null || !pin.matches("\\d{6}")) {
      throw new BadRequestException(ResponseMessageConstants.AUTH_PIN_INVALID_FORMAT);
    }
    if (Constants.Pin.FORBIDDEN_PINS.contains(pin)) {
      throw new BadRequestException(ResponseMessageConstants.AUTH_PIN_TOO_SIMPLE);
    }
  }

  private User findUserByKeycloakId(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }
}
