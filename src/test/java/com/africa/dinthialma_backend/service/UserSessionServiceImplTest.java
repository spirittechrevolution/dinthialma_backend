package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.codeList.ClientType;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserSession;
import com.africa.dinthialma_backend.auth.repository.UserSessionRepository;
import com.africa.dinthialma_backend.auth.service.impl.UserSessionServiceImpl;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.support.Fixtures;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSessionServiceImpl")
class UserSessionServiceImplTest {

  @Mock private UserSessionRepository repository;
  @InjectMocks private UserSessionServiceImpl service;

  private User user;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "tokenEncryptionKey", "cle-des-tests-unitaires!!");
    user = Fixtures.user("Awa");
    lenient().when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  @DisplayName("nouvelle session : jeton chiffré (jamais en clair), réversible, expiration posée")
  void creation() throws Exception {
    when(repository.findByUserIdAndClientType(user.getId(), ClientType.WEB))
        .thenReturn(Optional.empty());

    UserSession s = service.createOrUpdateSession(user, ClientType.WEB, "refresh-123", "Chrome");

    assertThat(s.getRefreshTokenHash()).isNotBlank().isNotEqualTo("refresh-123");
    assertThat(service.decryptRefreshToken(s)).isEqualTo("refresh-123");
    assertThat(s.getDeviceInfo()).isEqualTo("Chrome");
    assertThat(s.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(364));
  }

  @Test
  @DisplayName("session existante du même type : mise à jour, appareil conservé si non fourni")
  void miseAJour() throws Exception {
    UserSession existante =
        UserSession.builder()
            .id(UUID.randomUUID())
            .user(user)
            .clientType(ClientType.MOBILE)
            .refreshTokenHash("ancien")
            .deviceInfo("Pixel")
            .build();
    when(repository.findByUserIdAndClientType(user.getId(), ClientType.MOBILE))
        .thenReturn(Optional.of(existante));

    UserSession s = service.createOrUpdateSession(user, ClientType.MOBILE, "nouveau", " ");

    assertThat(s).isSameAs(existante);
    assertThat(s.getDeviceInfo()).isEqualTo("Pixel");
    assertThat(service.decryptRefreshToken(s)).isEqualTo("nouveau");
    assertThat(s.getLastUsedAt()).isNotNull();

    service.createOrUpdateSession(user, ClientType.MOBILE, "nouveau", "iPhone");
    assertThat(s.getDeviceInfo()).isEqualTo("iPhone");
  }

  @Test
  @DisplayName("déchiffrement d'un jeton corrompu -> 500")
  void dechiffrementImpossible() {
    UserSession s = UserSession.builder().id(UUID.randomUUID()).refreshTokenHash("xxx").build();
    assertThatThrownBy(() -> service.decryptRefreshToken(s))
        .isInstanceOfSatisfying(
            CustomException.class,
            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  @DisplayName("recherche de sessions actives : filtrées sur l'expiration")
  void sessionsActives() {
    UserSession s = UserSession.builder().id(UUID.randomUUID()).build();
    when(repository.findByUserIdAndClientTypeAndExpiresAtAfter(
            eq(user.getId()), eq(ClientType.WEB), any()))
        .thenReturn(Optional.of(s));
    when(repository.findTopByUserIdAndExpiresAtAfterOrderByExpiresAtDesc(eq(user.getId()), any()))
        .thenReturn(Optional.empty());

    assertThat(service.findActiveSession(user.getId(), ClientType.WEB)).contains(s);
    assertThat(service.findAnyActiveSession(user.getId())).isEmpty();
  }

  @Test
  @DisplayName("révocation : une session (inconnue -> 404) ou toutes")
  void revocation() throws Exception {
    UserSession s = UserSession.builder().id(UUID.randomUUID()).build();
    when(repository.findById(s.getId())).thenReturn(Optional.of(s));
    service.revokeSession(s.getId());
    verify(repository).delete(s);

    UUID inconnue = UUID.randomUUID();
    when(repository.findById(inconnue)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.revokeSession(inconnue)).isInstanceOf(NotFoundException.class);

    service.revokeAllSessions(user.getId());
    verify(repository).deleteAllByUserId(user.getId());
  }
}
