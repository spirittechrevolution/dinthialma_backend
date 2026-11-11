package com.africa.dinthialma_backend.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.codeList.ClientType;
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
import com.africa.dinthialma_backend.auth.service.interfaces.UserSessionService;
import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.exception.UnAuthorizedException;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import com.africa.dinthialma_backend.support.Fixtures;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("PinServiceImpl")
class PinServiceImplTest {

  private static final String PIN = "482913";
  private static final String TEL = "221770000001";
  private static final String P = Constants.OtpPurpose.RESET_PIN;
  private static String hashPin;

  @Mock private UserRepository userRepository;
  @Mock private OtpVerificationRepository otpRepository;
  @Mock private UserSessionService userSessionService;
  @Mock private KeycloakAuthService keycloakAuthService;
  @Mock private WhatsappService whatsappService;
  @InjectMocks private PinServiceImpl service;

  private User user;

  @BeforeAll
  static void hacher() {
    hashPin = new Argon2PasswordEncoder(16, 32, 1, 4096, 1).encode(PIN);
  }

  @BeforeEach
  void setUp() {
    user = Fixtures.user("Awa");
    user.setPhone(TEL);
    lenient()
        .when(userRepository.findByKeycloakId(user.getKeycloakId()))
        .thenReturn(Optional.of(user));
    lenient().when(userRepository.findByPhone(TEL)).thenReturn(Optional.of(user));
  }

  private static void codeAttendu(Throwable e, ErrorCode code) {
    assertThat(e).isInstanceOf(CustomException.class);
    assertThat(((CustomException) e).getCode()).isEqualTo(code);
  }

  @Nested
  @DisplayName("configuration du PIN")
  class Configuration {

    private PinSetupRequest req(String pin, String confirmation) {
      PinSetupRequest r = new PinSetupRequest();
      r.setPin(pin);
      r.setConfirmPin(confirmation);
      return r;
    }

    @Test
    @DisplayName("PIN valide : haché (Argon2), compteurs remis à zéro, date de création posée")
    void succes() throws Exception {
      user.setPinAttempts(3);
      user.setPinLockedUntil(LocalDateTime.now().plusMinutes(5));

      service.setupPin(user.getKeycloakId(), req(PIN, PIN));

      assertThat(user.getPinHash()).isNotNull().isNotEqualTo(PIN).startsWith("$argon2");
      assertThat(user.getPinAttempts()).isZero();
      assertThat(user.getPinLockedUntil()).isNull();
      assertThat(user.getPinCreatedAt()).isNotNull();
      assertThat(service.isPinConfigured(user.getKeycloakId())).isTrue();
    }

    @Test
    @DisplayName("format, PIN trivial, confirmation différente -> 400 avec code dédié")
    void refus() {
      assertThatThrownBy(() -> service.setupPin(user.getKeycloakId(), req("12a456", "12a456")))
          .satisfies(e -> codeAttendu(e, ErrorCode.PIN_INVALID_FORMAT));
      assertThatThrownBy(() -> service.setupPin(user.getKeycloakId(), req(null, null)))
          .satisfies(e -> codeAttendu(e, ErrorCode.PIN_INVALID_FORMAT));
      assertThatThrownBy(() -> service.setupPin(user.getKeycloakId(), req("123456", "123456")))
          .satisfies(e -> codeAttendu(e, ErrorCode.PIN_TOO_SIMPLE));
      assertThatThrownBy(() -> service.setupPin(user.getKeycloakId(), req(PIN, "482914")))
          .satisfies(e -> codeAttendu(e, ErrorCode.PIN_MISMATCH));
      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("utilisateur inconnu -> 404 ; PIN non configuré -> false")
    void inconnu() throws Exception {
      when(userRepository.findByKeycloakId("x")).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.setupPin("x", req(PIN, PIN)))
          .isInstanceOf(NotFoundException.class);
      assertThat(service.isPinConfigured(user.getKeycloakId())).isFalse();
    }
  }

  @Nested
  @DisplayName("connexion par PIN")
  class Connexion {

    private UserSession session(ClientType type) {
      return UserSession.builder().id(UUID.randomUUID()).user(user).clientType(type).build();
    }

    private PinLoginRequest req(String pin, ClientType type) {
      PinLoginRequest r = new PinLoginRequest();
      r.setPhone("+" + TEL);
      r.setPin(pin);
      r.setClientType(type);
      r.setDeviceInfo("Pixel");
      return r;
    }

    @BeforeEach
    void pinConfigure() {
      user.setPinHash(hashPin);
      user.setPinCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("bon PIN + session du même type : jetons rafraîchis, session mise à jour")
    void succes() throws Exception {
      user.setPinAttempts(2);
      UserSession s = session(ClientType.MOBILE);
      LoginResponse jetons = new LoginResponse();
      jetons.setRefreshToken("nouveau");
      when(userSessionService.findActiveSession(user.getId(), ClientType.MOBILE))
          .thenReturn(Optional.of(s));
      when(userSessionService.decryptRefreshToken(s)).thenReturn("ancien");
      when(keycloakAuthService.refreshToken("ancien")).thenReturn(jetons);

      assertThat(service.loginWithPin(req(PIN, ClientType.MOBILE))).isSameAs(jetons);

      assertThat(user.getPinAttempts()).isZero();
      verify(userSessionService).createOrUpdateSession(user, ClientType.MOBILE, "nouveau", "Pixel");
      verify(userSessionService, never()).revokeSession(any());
    }

    @Test
    @DisplayName("repli sur une session d'un autre type : la session source est révoquée")
    void repli() throws Exception {
      UserSession web = session(ClientType.WEB);
      LoginResponse jetons = new LoginResponse();
      jetons.setRefreshToken("nouveau");
      when(userSessionService.findActiveSession(user.getId(), ClientType.MOBILE))
          .thenReturn(Optional.empty());
      when(userSessionService.findAnyActiveSession(user.getId())).thenReturn(Optional.of(web));
      when(userSessionService.decryptRefreshToken(web)).thenReturn("ancien");
      when(keycloakAuthService.refreshToken("ancien")).thenReturn(jetons);
      doThrow(new NotFoundException("déjà supprimée"))
          .when(userSessionService)
          .revokeSession(web.getId());

      assertThat(service.loginWithPin(req(PIN, ClientType.MOBILE))).isSameAs(jetons);
      verify(userSessionService).revokeSession(web.getId());
      verify(userSessionService).createOrUpdateSession(user, ClientType.MOBILE, "nouveau", "Pixel");
    }

    @Test
    @DisplayName("aucune session active -> 401 PIN_SESSION_EXPIRED")
    void sansSession() {
      when(userSessionService.findActiveSession(user.getId(), ClientType.WEB))
          .thenReturn(Optional.empty());
      when(userSessionService.findAnyActiveSession(user.getId())).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.loginWithPin(req(PIN, ClientType.WEB)))
          .satisfies(e -> codeAttendu(e, ErrorCode.PIN_SESSION_EXPIRED));
    }

    @Test
    @DisplayName("session Keycloak expirée : session locale supprimée, erreur propagée")
    void keycloakExpire() throws Exception {
      UserSession s = session(ClientType.WEB);
      when(userSessionService.findActiveSession(user.getId(), ClientType.WEB))
          .thenReturn(Optional.of(s));
      when(userSessionService.decryptRefreshToken(s)).thenReturn("ancien");
      when(keycloakAuthService.refreshToken("ancien"))
          .thenThrow(new UnAuthorizedException("expiré"));

      assertThatThrownBy(() -> service.loginWithPin(req(PIN, ClientType.WEB)))
          .isInstanceOf(UnAuthorizedException.class);
      verify(userSessionService).revokeSession(s.getId());
      verify(userSessionService, never()).createOrUpdateSession(any(), any(), any(), any());
    }

    @Test
    @DisplayName("mauvais PIN : tentative comptée -> 401 PIN_INVALID")
    void mauvaisPin() {
      user.setPinAttempts(1);
      assertThatThrownBy(() -> service.loginWithPin(req("000001", ClientType.WEB)))
          .satisfies(e -> codeAttendu(e, ErrorCode.PIN_INVALID));
      assertThat(user.getPinAttempts()).isEqualTo(2);
      verify(userRepository).save(user);
      verifyNoInteractions(keycloakAuthService);
    }

    @Test
    @DisplayName("5e échec : verrouillage 30 min -> 429 PIN_LOCKED avec délai")
    void verrouillage() {
      user.setPinAttempts(Constants.Pin.MAX_ATTEMPTS - 1);
      assertThatThrownBy(() -> service.loginWithPin(req("000001", ClientType.WEB)))
          .satisfies(e -> codeAttendu(e, ErrorCode.PIN_LOCKED));
      assertThat(user.getPinLockedUntil()).isAfter(LocalDateTime.now().plusMinutes(29));
      assertThat(user.getPinAttempts()).isZero();
    }

    @Test
    @DisplayName("compte verrouillé : refus même avec le bon PIN")
    void dejaVerrouille() {
      user.setPinLockedUntil(LocalDateTime.now().plusMinutes(10));
      assertThatThrownBy(() -> service.loginWithPin(req(PIN, ClientType.WEB)))
          .satisfies(e -> codeAttendu(e, ErrorCode.PIN_LOCKED));
      verifyNoInteractions(userSessionService);
    }

    @Test
    @DisplayName("PIN de plus de 90 jours -> 400 PIN_EXPIRED")
    void expire() {
      user.setPinCreatedAt(LocalDateTime.now().minusDays(Constants.Pin.EXPIRY_DAYS));
      assertThatThrownBy(() -> service.loginWithPin(req(PIN, ClientType.WEB)))
          .satisfies(e -> codeAttendu(e, ErrorCode.PIN_EXPIRED));
    }

    @Test
    @DisplayName("PIN non configuré, compte désactivé, numéro inconnu")
    void refusPrealables() {
      user.setPinHash(null);
      assertThatThrownBy(() -> service.loginWithPin(req(PIN, ClientType.WEB)))
          .satisfies(e -> codeAttendu(e, ErrorCode.PIN_NOT_CONFIGURED));
      user.setActive(false);
      assertThatThrownBy(() -> service.loginWithPin(req(PIN, ClientType.WEB)))
          .satisfies(e -> codeAttendu(e, ErrorCode.ACCOUNT_DISABLED));
      when(userRepository.findByPhone(TEL)).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.loginWithPin(req(PIN, ClientType.WEB)))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("réinitialisation du PIN")
  class Reinitialisation {

    private OtpVerification otp(String code, LocalDateTime expiration) {
      return OtpVerification.builder()
          .phone(TEL)
          .code(code)
          .purpose(P)
          .expiresAt(expiration)
          .used(false)
          .verified(false)
          .build();
    }

    private PinResetRequest req(String pin, String confirmation) {
      PinResetRequest r = new PinResetRequest();
      r.setPhone("+" + TEL);
      r.setNewPin(pin);
      r.setConfirmPin(confirmation);
      return r;
    }

    @Test
    @DisplayName("envoi OTP : numéro inconnu silencieux ; sinon ancien invalidé et nouveau envoyé")
    void envoi() throws Exception {
      when(userRepository.existsByPhone(TEL)).thenReturn(false);
      service.sendResetPinOtp("+" + TEL);
      verifyNoInteractions(otpRepository, whatsappService);

      OtpVerification ancien = otp("111111", LocalDateTime.now().plusMinutes(5));
      when(userRepository.existsByPhone(TEL)).thenReturn(true);
      when(otpRepository.findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(TEL, P))
          .thenReturn(Optional.of(ancien));
      service.sendResetPinOtp("+" + TEL);

      assertThat(ancien.isUsed()).isTrue();
      ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
      verify(otpRepository, times(2)).save(captor.capture());
      OtpVerification nouveau = captor.getAllValues().get(1);
      assertThat(nouveau.getPurpose()).isEqualTo(P);
      verify(whatsappService).sendOtp(TEL, nouveau.getCode());
    }

    @Test
    @DisplayName("vérification OTP : absent, expiré, faux -> 400 ; bon -> verified")
    void verification() throws Exception {
      when(otpRepository.findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(TEL, P))
          .thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.verifyResetPinOtp(TEL, "123456"))
          .satisfies(e -> codeAttendu(e, ErrorCode.OTP_INVALID));

      when(otpRepository.findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(TEL, P))
          .thenReturn(Optional.of(otp("123456", LocalDateTime.now().minusSeconds(1))));
      assertThatThrownBy(() -> service.verifyResetPinOtp(TEL, "123456"))
          .satisfies(e -> codeAttendu(e, ErrorCode.OTP_EXPIRED));

      OtpVerification valide = otp("123456", LocalDateTime.now().plusMinutes(5));
      when(otpRepository.findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(TEL, P))
          .thenReturn(Optional.of(valide));
      assertThatThrownBy(() -> service.verifyResetPinOtp(TEL, "654320"))
          .satisfies(e -> codeAttendu(e, ErrorCode.OTP_INVALID));
      service.verifyResetPinOtp("+" + TEL, "123456");
      assertThat(valide.isVerified()).isTrue();
    }

    @Test
    @DisplayName("nouveau PIN : enregistré, verrou levé, sessions révoquées, OTP consommé")
    void succes() throws Exception {
      OtpVerification verifie = otp("123456", LocalDateTime.now().plusMinutes(5));
      verifie.setVerified(true);
      user.setPinLockedUntil(LocalDateTime.now().plusMinutes(20));
      when(otpRepository.findTopByPhoneAndPurposeAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(
              TEL, P))
          .thenReturn(Optional.of(verifie));

      service.resetPin(req(PIN, PIN));

      assertThat(user.getPinHash()).startsWith("$argon2");
      assertThat(user.getPinLockedUntil()).isNull();
      assertThat(user.getPinCreatedAt()).isNotNull();
      assertThat(verifie.isUsed()).isTrue();
      verify(userSessionService).revokeAllSessions(user.getId());
    }

    @Test
    @DisplayName("refus : OTP non vérifié, numéro inconnu, PIN trivial, confirmation différente")
    void refus() {
      when(otpRepository.findTopByPhoneAndPurposeAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(
              TEL, P))
          .thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.resetPin(req(PIN, PIN)))
          .satisfies(e -> codeAttendu(e, ErrorCode.OTP_NOT_VERIFIED));

      when(otpRepository.findTopByPhoneAndPurposeAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(
              TEL, P))
          .thenReturn(Optional.of(otp("1", LocalDateTime.now().plusMinutes(5))));
      assertThatThrownBy(() -> service.resetPin(req("111111", "111111")))
          .satisfies(e -> codeAttendu(e, ErrorCode.PIN_TOO_SIMPLE));
      assertThatThrownBy(() -> service.resetPin(req(PIN, "999990")))
          .satisfies(e -> codeAttendu(e, ErrorCode.PIN_MISMATCH));
      when(userRepository.findByPhone(TEL)).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.resetPin(req(PIN, PIN)))
          .isInstanceOf(NotFoundException.class);
      verify(userSessionService, never()).revokeAllSessions(any());
    }
  }
}
