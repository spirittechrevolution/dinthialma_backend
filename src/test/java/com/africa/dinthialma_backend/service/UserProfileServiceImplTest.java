package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.config.KeycloakProperties;
import com.africa.dinthialma_backend.auth.dto.PhoneChangeRequest;
import com.africa.dinthialma_backend.auth.dto.PhoneChangeVerifyRequest;
import com.africa.dinthialma_backend.auth.dto.UpdateProfileRequest;
import com.africa.dinthialma_backend.auth.dto.UserProfileResponse;
import com.africa.dinthialma_backend.auth.entity.PhoneChangeRequestEntity;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.mapper.UserMapper;
import com.africa.dinthialma_backend.auth.repository.PhoneChangeRequestRepository;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.service.impl.UserProfileServiceImpl;
import com.africa.dinthialma_backend.auth.service.interfaces.UserSessionService;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import com.africa.dinthialma_backend.support.Fixtures;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.factory.Mappers;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileServiceImpl")
class UserProfileServiceImplTest {

  private static final String NOUVEAU = "221779999999";

  @Mock private UserRepository userRepository;
  @Mock private PhoneChangeRequestRepository phoneChangeRepository;
  @Mock private UserSessionService userSessionService;
  @Mock private WhatsappService whatsappService;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Keycloak keycloak;

  @Mock private UserResource userResource;

  private UserProfileServiceImpl service;
  private User user;
  private UserRepresentation representation;

  @BeforeEach
  void setUp() {
    KeycloakProperties props = new KeycloakProperties();
    props.setRealm("dinthialma");
    service =
        new UserProfileServiceImpl(
            userRepository,
            Mappers.getMapper(UserMapper.class),
            phoneChangeRepository,
            userSessionService,
            whatsappService,
            keycloak,
            props);
    user = Fixtures.user("Awa");
    user.setEmail("awa@test.sn");
    representation = new UserRepresentation();
    lenient()
        .when(userRepository.findByKeycloakId(user.getKeycloakId()))
        .thenReturn(Optional.of(user));
    lenient().when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    lenient().when(keycloak.realm("dinthialma").users().get(anyString())).thenReturn(userResource);
    lenient().when(userResource.toRepresentation()).thenReturn(representation);
  }

  private static UpdateProfileRequest profil(String email) {
    UpdateProfileRequest r = new UpdateProfileRequest();
    r.setFirstName(" Fatou ");
    r.setLastName(" Ndiaye ");
    r.setEmail(email);
    return r;
  }

  @Test
  @DisplayName("lecture du profil ; utilisateur inconnu -> 404")
  void lecture() throws Exception {
    UserProfileResponse r = service.getProfile(user.getKeycloakId());
    assertThat(r.getFirstName()).isEqualTo("Awa");
    when(userRepository.findByKeycloakId("x")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getProfile("x")).isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("modification : base et Keycloak mis à jour, e-mail en minuscules")
  void modification() throws Exception {
    when(userRepository.existsByEmail("fatou@test.sn")).thenReturn(false);

    UserProfileResponse r = service.updateProfile(user.getKeycloakId(), profil(" Fatou@Test.SN "));

    assertThat(r.getFirstName()).isEqualTo("Fatou");
    assertThat(r.getLastName()).isEqualTo("Ndiaye");
    assertThat(user.getEmail()).isEqualTo("fatou@test.sn");
    assertThat(representation.getFirstName()).isEqualTo("Fatou");
    assertThat(representation.getEmail()).isEqualTo("fatou@test.sn");
    verify(userResource).update(representation);
  }

  @Test
  @DisplayName("modification sans e-mail : e-mail conservé ; Keycloak en panne non bloquant")
  void modificationSansEmail() throws Exception {
    when(userResource.toRepresentation()).thenThrow(new RuntimeException("Keycloak KO"));

    service.updateProfile(user.getKeycloakId(), profil(" "));

    assertThat(user.getEmail()).isEqualTo("awa@test.sn");
    assertThat(user.getFirstName()).isEqualTo("Fatou");
  }

  @Test
  @DisplayName(
      "e-mail déjà pris par un autre compte -> 409 EMAIL_ALREADY_USED ; le sien est accepté")
  void emailPris() throws Exception {
    when(userRepository.existsByEmail("pris@test.sn")).thenReturn(true);
    assertThatThrownBy(() -> service.updateProfile(user.getKeycloakId(), profil("pris@test.sn")))
        .isInstanceOfSatisfying(
            ConflictException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_USED));

    service.updateProfile(user.getKeycloakId(), profil("AWA@test.sn"));
    verify(userRepository, never()).existsByEmail("awa@test.sn");
  }

  @Test
  @DisplayName("demande de changement de numéro : OTP envoyé au nouveau numéro")
  void demandeChangement() throws Exception {
    PhoneChangeRequest req = new PhoneChangeRequest();
    req.setNewPhone("+" + NOUVEAU);
    when(userRepository.existsByPhone(NOUVEAU)).thenReturn(false);

    service.requestPhoneChange(user.getKeycloakId(), req);

    verify(phoneChangeRepository).deleteAllByUserId(user.getId());
    ArgumentCaptor<PhoneChangeRequestEntity> captor =
        ArgumentCaptor.forClass(PhoneChangeRequestEntity.class);
    verify(phoneChangeRepository).save(captor.capture());
    PhoneChangeRequestEntity d = captor.getValue();
    assertThat(d.getOldPhone()).isEqualTo(user.getPhone());
    assertThat(d.getNewPhone()).isEqualTo(NOUVEAU);
    verify(whatsappService).sendOtp(NOUVEAU, d.getOtpCode());
  }

  @Test
  @DisplayName(
      "demande refusée : numéro identique (PHONE_UNCHANGED) ou déjà utilisé (PHONE_ALREADY_USED)")
  void demandeRefusee() {
    PhoneChangeRequest req = new PhoneChangeRequest();
    req.setNewPhone(user.getPhone());
    assertThatThrownBy(() -> service.requestPhoneChange(user.getKeycloakId(), req))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PHONE_UNCHANGED));

    req.setNewPhone(NOUVEAU);
    when(userRepository.existsByPhone(NOUVEAU)).thenReturn(true);
    assertThatThrownBy(() -> service.requestPhoneChange(user.getKeycloakId(), req))
        .isInstanceOfSatisfying(
            ConflictException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PHONE_ALREADY_USED));
    verify(whatsappService, never()).sendOtp(any(), any());
  }

  private PhoneChangeRequestEntity demande(LocalDateTime expiration) {
    return PhoneChangeRequestEntity.builder()
        .user(user)
        .oldPhone(user.getPhone())
        .newPhone(NOUVEAU)
        .otpCode("123456")
        .expiresAt(expiration)
        .build();
  }

  private PhoneChangeVerifyRequest verification(String code) {
    PhoneChangeVerifyRequest r = new PhoneChangeVerifyRequest();
    r.setNewPhone("+" + NOUVEAU);
    r.setCode(code);
    return r;
  }

  @Test
  @DisplayName("vérification : numéro changé (base + Keycloak), sessions révoquées")
  void verificationReussie() throws Exception {
    PhoneChangeRequestEntity d = demande(LocalDateTime.now().plusMinutes(5));
    when(phoneChangeRepository.findTopByUserIdAndNewPhoneAndVerifiedFalseOrderByCreatedAtDesc(
            user.getId(), NOUVEAU))
        .thenReturn(Optional.of(d));

    service.verifyPhoneChange(user.getKeycloakId(), verification("123456"));

    assertThat(user.getPhone()).isEqualTo(NOUVEAU);
    assertThat(representation.getUsername()).isEqualTo(NOUVEAU);
    assertThat(representation.getAttributes()).containsEntry("phone", List.of(NOUVEAU));
    verify(userSessionService).revokeAllSessions(user.getId());
    assertThat(d.isVerified()).isTrue();
  }

  @Test
  @DisplayName("vérification refusée : aucune demande, OTP expiré, OTP faux")
  void verificationRefusee() {
    when(phoneChangeRepository.findTopByUserIdAndNewPhoneAndVerifiedFalseOrderByCreatedAtDesc(
            user.getId(), NOUVEAU))
        .thenReturn(Optional.empty());
    assertThatThrownBy(
            () -> service.verifyPhoneChange(user.getKeycloakId(), verification("123456")))
        .isInstanceOf(BadRequestException.class);

    when(phoneChangeRepository.findTopByUserIdAndNewPhoneAndVerifiedFalseOrderByCreatedAtDesc(
            user.getId(), NOUVEAU))
        .thenReturn(Optional.of(demande(LocalDateTime.now().minusSeconds(1))));
    assertThatThrownBy(
            () -> service.verifyPhoneChange(user.getKeycloakId(), verification("123456")))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.OTP_EXPIRED));

    when(phoneChangeRepository.findTopByUserIdAndNewPhoneAndVerifiedFalseOrderByCreatedAtDesc(
            user.getId(), NOUVEAU))
        .thenReturn(Optional.of(demande(LocalDateTime.now().plusMinutes(5))));
    assertThatThrownBy(
            () -> service.verifyPhoneChange(user.getKeycloakId(), verification("000000")))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.OTP_INVALID));
    verify(userSessionService, never()).revokeAllSessions(any());
  }

  @Test
  @DisplayName("échec Keycloak sur le numéro : bloquant, numéro local inchangé")
  void keycloakEnPanne() {
    String ancien = user.getPhone();
    when(phoneChangeRepository.findTopByUserIdAndNewPhoneAndVerifiedFalseOrderByCreatedAtDesc(
            user.getId(), NOUVEAU))
        .thenReturn(Optional.of(demande(LocalDateTime.now().plusMinutes(5))));
    when(userResource.toRepresentation()).thenThrow(new RuntimeException("KO"));

    assertThatThrownBy(
            () -> service.verifyPhoneChange(user.getKeycloakId(), verification("123456")))
        .isInstanceOf(RuntimeException.class);
    assertThat(user.getPhone()).isEqualTo(ancien);
  }
}
