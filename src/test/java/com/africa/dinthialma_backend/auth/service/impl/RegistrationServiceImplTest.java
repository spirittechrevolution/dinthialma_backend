package com.africa.dinthialma_backend.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import com.africa.dinthialma_backend.support.Fixtures;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationServiceImpl")
class RegistrationServiceImplTest {

  private static final String TEL = "221770000001";
  private static final String P = Constants.OtpPurpose.REGISTRATION;

  @Mock private OtpVerificationRepository otpRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserRoleAssignmentRepository roleRepository;
  @Mock private KeycloakAuthService keycloakAuthService;
  @Mock private WhatsappService whatsappService;
  @InjectMocks private RegistrationServiceImpl service;

  private OtpVerification otpVerifie;

  @BeforeEach
  void setUp() {
    otpVerifie =
        OtpVerification.builder()
            .phone(TEL)
            .code("123456")
            .purpose(P)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .verified(true)
            .build();
    lenient()
        .when(userRepository.save(any()))
        .thenAnswer(
            i -> {
              User u = i.getArgument(0);
              if (u.getId() == null) u.setId(UUID.randomUUID());
              return u;
            });
  }

  private static RegisterCompleteRequest requete() {
    RegisterCompleteRequest r = new RegisterCompleteRequest();
    r.setPhone("+" + TEL);
    r.setFirstName(" Awa ");
    r.setLastName(" Diop ");
    r.setEmail(" Awa@Test.SN ");
    r.setPassword("Secret@2026");
    return r;
  }

  private void otpVerifieDisponible() {
    when(otpRepository.findTopByPhoneAndPurposeAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(
            TEL, P))
        .thenReturn(Optional.of(otpVerifie));
  }

  @Test
  @DisplayName("envoi OTP : numéro libre ou PRE_ENROLLED accepté, anciens OTP purgés")
  void envoi() throws Exception {
    SendOtpRequest req = new SendOtpRequest();
    req.setPhone("+" + TEL);
    when(userRepository.findByPhone(TEL)).thenReturn(Optional.empty());
    service.sendOtp(req);

    User pre = Fixtures.user("Pré");
    pre.setAccountStatus(AccountStatus.PRE_ENROLLED);
    when(userRepository.findByPhone(TEL)).thenReturn(Optional.of(pre));
    service.sendOtp(req);

    verify(otpRepository, times(2)).deleteAllByPhoneAndPurpose(TEL, P);
    ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
    verify(otpRepository, times(2)).save(captor.capture());
    verify(whatsappService).sendOtp(TEL, captor.getAllValues().get(0).getCode());
  }

  @Test
  @DisplayName("envoi OTP : numéro déjà ACTIVE (ou statut NULL) -> 409 PHONE_ALREADY_USED")
  void envoiNumeroPris() {
    SendOtpRequest req = new SendOtpRequest();
    req.setPhone(TEL);
    User actif = Fixtures.user("Actif");
    actif.setAccountStatus(null);
    when(userRepository.findByPhone(TEL)).thenReturn(Optional.of(actif));
    assertThatThrownBy(() -> service.sendOtp(req))
        .isInstanceOfSatisfying(
            ConflictException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PHONE_ALREADY_USED));
    verify(otpRepository, never()).save(any());
  }

  @Test
  @DisplayName("vérification OTP : absent / expiré / faux -> 400 ; bon -> verified")
  void verification() throws Exception {
    VerifyOtpRequest req = new VerifyOtpRequest();
    req.setPhone("+" + TEL);
    req.setCode("123456");
    when(otpRepository.findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(TEL, P))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.verifyOtp(req))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.OTP_INVALID));

    OtpVerification expire =
        OtpVerification.builder()
            .code("123456")
            .expiresAt(LocalDateTime.now().minusSeconds(1))
            .build();
    when(otpRepository.findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(TEL, P))
        .thenReturn(Optional.of(expire));
    assertThatThrownBy(() -> service.verifyOtp(req))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.OTP_EXPIRED));

    OtpVerification valide =
        OtpVerification.builder()
            .code("123456")
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .build();
    when(otpRepository.findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(TEL, P))
        .thenReturn(Optional.of(valide));
    req.setCode("000000");
    assertThatThrownBy(() -> service.verifyOtp(req)).isInstanceOf(BadRequestException.class);
    req.setCode("123456");
    service.verifyOtp(req);
    assertThat(valide.isVerified()).isTrue();
  }

  @Test
  @DisplayName(
      "finalisation nouveau compte : Keycloak + rôle USER, données nettoyées, OTP consommé")
  void nouveauCompte() throws Exception {
    otpVerifieDisponible();
    when(userRepository.findByPhone(TEL)).thenReturn(Optional.empty());
    when(keycloakAuthService.createUser(any())).thenReturn("kc-1");

    RegisterResponse r = service.completeRegistration(requete());

    assertThat(r.getKeycloakId()).isEqualTo("kc-1");
    assertThat(r.getPhone()).isEqualTo(TEL);
    assertThat(r.getFirstName()).isEqualTo("Awa");
    assertThat(r.getEmail()).isEqualTo("awa@test.sn");
    assertThat(r.getRole()).isEqualTo("DINTHIALMA_USER");
    verify(keycloakAuthService).assignRole("kc-1", "DINTHIALMA_USER");
    ArgumentCaptor<UserRoleAssignment> role = ArgumentCaptor.forClass(UserRoleAssignment.class);
    verify(roleRepository).save(role.capture());
    assertThat(role.getValue().getRole()).isEqualTo(UserRole.USER);
    assertThat(role.getValue().isSyncedToKeycloak()).isTrue();
    assertThat(otpVerifie.isUsed()).isTrue();
  }

  @Test
  @DisplayName("rôle Keycloak non synchronisé : inscription réussie, rôle marqué non synchronisé")
  void roleNonSynchronise() throws Exception {
    otpVerifieDisponible();
    when(userRepository.findByPhone(TEL)).thenReturn(Optional.empty());
    when(keycloakAuthService.createUser(any())).thenReturn("kc-1");
    doThrow(new CustomException(HttpStatus.BAD_GATEWAY, "KO"))
        .when(keycloakAuthService)
        .assignRole(anyString(), anyString());

    service.completeRegistration(requete());

    ArgumentCaptor<UserRoleAssignment> role = ArgumentCaptor.forClass(UserRoleAssignment.class);
    verify(roleRepository).save(role.capture());
    assertThat(role.getValue().isSyncedToKeycloak()).isFalse();
  }

  @Test
  @DisplayName(
      "activation d'un PRE_ENROLLED : même ligne, rôles USER + MEMBER ajoutés s'ils manquent")
  void activationPreInscrit() throws Exception {
    User pre = Fixtures.user("Ancien");
    pre.setKeycloakId(null);
    pre.setAccountStatus(AccountStatus.PRE_ENROLLED);
    otpVerifieDisponible();
    when(userRepository.findByPhone(TEL)).thenReturn(Optional.of(pre));
    when(keycloakAuthService.createUser(any())).thenReturn("kc-2");
    when(roleRepository.existsByUserIdAndRole(pre.getId(), UserRole.USER)).thenReturn(true);
    when(roleRepository.existsByUserIdAndRole(pre.getId(), UserRole.MEMBER)).thenReturn(false);

    RegisterResponse r = service.completeRegistration(requete());

    assertThat(r.getUserId()).isEqualTo(pre.getId());
    assertThat(r.getRole()).isEqualTo("DINTHIALMA_MEMBER");
    assertThat(pre.getKeycloakId()).isEqualTo("kc-2");
    assertThat(pre.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(pre.getFirstName()).isEqualTo("Awa");
    verify(keycloakAuthService).assignRole("kc-2", "DINTHIALMA_USER");
    verify(keycloakAuthService).assignRole("kc-2", "DINTHIALMA_MEMBER");
    ArgumentCaptor<UserRoleAssignment> role = ArgumentCaptor.forClass(UserRoleAssignment.class);
    verify(roleRepository, times(1)).save(role.capture());
    assertThat(role.getValue().getRole()).isEqualTo(UserRole.MEMBER);
    assertThat(otpVerifie.isUsed()).isTrue();
  }

  @Test
  @DisplayName(
      "activation PRE_ENROLLED avec rôles Keycloak en échec : rôles marqués non synchronisés")
  void activationSansSynchro() throws Exception {
    User pre = Fixtures.user("Ancien");
    pre.setAccountStatus(AccountStatus.PRE_ENROLLED);
    RegisterCompleteRequest req = requete();
    req.setEmail(null);
    otpVerifieDisponible();
    when(userRepository.findByPhone(TEL)).thenReturn(Optional.of(pre));
    when(keycloakAuthService.createUser(any())).thenReturn("kc-3");
    doThrow(new CustomException(HttpStatus.BAD_GATEWAY, "KO"))
        .when(keycloakAuthService)
        .assignRole(anyString(), anyString());

    service.completeRegistration(req);

    assertThat(pre.getEmail()).isNull();
    ArgumentCaptor<UserRoleAssignment> role = ArgumentCaptor.forClass(UserRoleAssignment.class);
    verify(roleRepository, times(2)).save(role.capture());
    assertThat(role.getAllValues()).noneMatch(UserRoleAssignment::isSyncedToKeycloak);
  }

  @Test
  @DisplayName("finalisation refusée : OTP non vérifié (400) ou numéro déjà actif (409)")
  void finalisationRefusee() throws Exception {
    when(otpRepository.findTopByPhoneAndPurposeAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(
            TEL, P))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.completeRegistration(requete()))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.OTP_NOT_VERIFIED));

    otpVerifieDisponible();
    when(userRepository.findByPhone(TEL)).thenReturn(Optional.of(Fixtures.user("Actif")));
    assertThatThrownBy(() -> service.completeRegistration(requete()))
        .isInstanceOf(ConflictException.class);
    verify(keycloakAuthService, never()).createUser(any());
  }
}
