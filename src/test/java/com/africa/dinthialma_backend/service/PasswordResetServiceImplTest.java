package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.dto.ResetPasswordByPhoneRequest;
import com.africa.dinthialma_backend.auth.dto.SendOtpRequest;
import com.africa.dinthialma_backend.auth.dto.VerifyOtpRequest;
import com.africa.dinthialma_backend.auth.entity.OtpVerification;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.OtpVerificationRepository;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.service.impl.PasswordResetServiceImpl;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.auth.service.interfaces.UserSessionService;
import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import com.africa.dinthialma_backend.support.Fixtures;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetServiceImpl")
class PasswordResetServiceImplTest {

  private static final String TEL = "221770000001";
  private static final String P = Constants.OtpPurpose.RESET_PASSWORD;

  @Mock private OtpVerificationRepository otpRepository;
  @Mock private UserRepository userRepository;
  @Mock private KeycloakAuthService keycloakAuthService;
  @Mock private UserSessionService userSessionService;
  @Mock private WhatsappService whatsappService;
  @InjectMocks private PasswordResetServiceImpl service;

  private static OtpVerification otp(String code, LocalDateTime expiration) {
    return OtpVerification.builder()
        .phone(TEL)
        .code(code)
        .purpose(P)
        .expiresAt(expiration)
        .used(false)
        .verified(false)
        .build();
  }

  private static <T> T avecTelephone(T requete) {
    if (requete instanceof SendOtpRequest s) s.setPhone(" +" + TEL + " ");
    if (requete instanceof VerifyOtpRequest v) v.setPhone("+" + TEL);
    if (requete instanceof ResetPasswordByPhoneRequest r) r.setPhone("+" + TEL);
    return requete;
  }

  @Test
  @DisplayName("envoi : numéro inconnu -> silence (anti-énumération), aucun OTP")
  void envoiNumeroInconnu() throws Exception {
    when(userRepository.existsByPhone(TEL)).thenReturn(false);
    service.sendForgotPasswordOtp(avecTelephone(new SendOtpRequest()));
    verifyNoInteractions(otpRepository, whatsappService);
  }

  @Test
  @DisplayName("envoi : numéro « + » normalisé, ancien OTP invalidé, nouveau envoyé par WhatsApp")
  void envoi() throws Exception {
    OtpVerification ancien = otp("111111", LocalDateTime.now().plusMinutes(5));
    when(userRepository.existsByPhone(TEL)).thenReturn(true);
    when(otpRepository.findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(TEL, P))
        .thenReturn(Optional.of(ancien));

    service.sendForgotPasswordOtp(avecTelephone(new SendOtpRequest()));

    assertThat(ancien.isUsed()).isTrue();
    ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
    verify(otpRepository, org.mockito.Mockito.times(2)).save(captor.capture());
    OtpVerification nouveau = captor.getAllValues().get(1);
    assertThat(nouveau.getPhone()).isEqualTo(TEL);
    assertThat(nouveau.getPurpose()).isEqualTo(P);
    assertThat(nouveau.getCode()).matches("\\d{6}");
    assertThat(nouveau.getExpiresAt()).isAfter(LocalDateTime.now());
    verify(whatsappService).sendOtp(TEL, nouveau.getCode());
  }

  @Test
  @DisplayName("vérification : bon code -> verified ; mauvais code, expiré ou absent -> 400")
  void verification() throws Exception {
    VerifyOtpRequest req = avecTelephone(new VerifyOtpRequest());

    when(otpRepository.findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(TEL, P))
        .thenReturn(Optional.empty());
    req.setCode("123456");
    assertThatThrownBy(() -> service.verifyForgotPasswordOtp(req))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.OTP_INVALID));

    OtpVerification expire = otp("123456", LocalDateTime.now().minusMinutes(1));
    when(otpRepository.findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(TEL, P))
        .thenReturn(Optional.of(expire));
    assertThatThrownBy(() -> service.verifyForgotPasswordOtp(req))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.OTP_EXPIRED));

    OtpVerification valide = otp("123456", LocalDateTime.now().plusMinutes(5));
    when(otpRepository.findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(TEL, P))
        .thenReturn(Optional.of(valide));
    req.setCode("000000");
    assertThatThrownBy(() -> service.verifyForgotPasswordOtp(req))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.OTP_INVALID));
    assertThat(valide.isVerified()).isFalse();

    req.setCode("123456");
    service.verifyForgotPasswordOtp(req);
    assertThat(valide.isVerified()).isTrue();
  }

  @Test
  @DisplayName("réinitialisation : Keycloak mis à jour, sessions révoquées, OTP consommé")
  void reinitialisation() throws Exception {
    User user = Fixtures.user("Awa");
    OtpVerification verifie = otp("123456", LocalDateTime.now().plusMinutes(5));
    verifie.setVerified(true);
    when(otpRepository.findTopByPhoneAndPurposeAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(
            TEL, P))
        .thenReturn(Optional.of(verifie));
    when(userRepository.findByPhone(TEL)).thenReturn(Optional.of(user));
    ResetPasswordByPhoneRequest req = avecTelephone(new ResetPasswordByPhoneRequest());
    req.setNewPassword("Nouveau@2026");

    service.resetPassword(req);

    verify(keycloakAuthService).resetPassword(user.getKeycloakId(), "Nouveau@2026");
    verify(userSessionService).revokeAllSessions(user.getId());
    assertThat(verifie.isUsed()).isTrue();
  }

  @Test
  @DisplayName(
      "réinitialisation sans OTP vérifié -> 400 OTP_NOT_VERIFIED ; utilisateur absent -> 404")
  void reinitialisationRefusee() throws Exception {
    ResetPasswordByPhoneRequest req = avecTelephone(new ResetPasswordByPhoneRequest());
    req.setNewPassword("Nouveau@2026");
    when(otpRepository.findTopByPhoneAndPurposeAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(
            TEL, P))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.resetPassword(req))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.OTP_NOT_VERIFIED));

    when(otpRepository.findTopByPhoneAndPurposeAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(
            TEL, P))
        .thenReturn(Optional.of(otp("1", LocalDateTime.now().plusMinutes(5))));
    when(userRepository.findByPhone(TEL)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.resetPassword(req)).isInstanceOf(NotFoundException.class);
    verify(keycloakAuthService, never()).resetPassword(anyString(), eq("Nouveau@2026"));
    verify(otpRepository, never()).save(any());
  }
}
