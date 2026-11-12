package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.codeList.ClientType;
import com.africa.dinthialma_backend.auth.dto.LoginRequest;
import com.africa.dinthialma_backend.auth.dto.LoginResponse;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserSession;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.service.impl.LoginServiceImpl;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.auth.service.interfaces.UserSessionService;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.support.Fixtures;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginServiceImpl")
class LoginServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private KeycloakAuthService keycloakAuthService;
  @Mock private UserSessionService userSessionService;
  @InjectMocks private LoginServiceImpl service;

  private User user;

  @BeforeEach
  void setUp() {
    user = Fixtures.user("Awa");
    user.setPhone("221770000001");
  }

  private static LoginRequest requete(String username) {
    LoginRequest r = new LoginRequest();
    r.setUsername(username);
    r.setPassword("Secret@2026");
    r.setClientType(ClientType.MOBILE);
    r.setDeviceInfo("Pixel");
    return r;
  }

  private static String jwt(String sub) {
    String payload = sub == null ? "{}" : "{\"sub\":\"" + sub + "\"}";
    Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
    return enc.encodeToString("{}".getBytes(StandardCharsets.UTF_8))
        + "."
        + enc.encodeToString(payload.getBytes(StandardCharsets.UTF_8))
        + ".signature";
  }

  @Nested
  @DisplayName("login")
  class Connexion {

    @Test
    @DisplayName(
        "téléphone avec « + » normalisé : Keycloak appelé, session PIN créée, pinConfigured")
    void succes() throws Exception {
      user.setPinHash("hash");
      LoginResponse jetons = new LoginResponse();
      jetons.setRefreshToken("refresh");
      when(userRepository.findByPhone("221770000001")).thenReturn(Optional.of(user));
      when(keycloakAuthService.login("221770000001", "Secret@2026")).thenReturn(jetons);

      LoginResponse r = service.login(requete(" +221770000001 "));

      assertThat(r).isSameAs(jetons);
      assertThat(r.isPinConfigured()).isTrue();
      verify(userSessionService).createOrUpdateSession(user, ClientType.MOBILE, "refresh", "Pixel");
    }

    @Test
    @DisplayName("résolution par e-mail quand le téléphone ne correspond pas ; PIN absent")
    void parEmail() throws Exception {
      LoginResponse jetons = new LoginResponse();
      when(userRepository.findByPhone("awa@test.sn")).thenReturn(Optional.empty());
      when(userRepository.findByEmail("awa@test.sn")).thenReturn(Optional.of(user));
      when(keycloakAuthService.login(user.getPhone(), "Secret@2026")).thenReturn(jetons);

      assertThat(service.login(requete("awa@test.sn")).isPinConfigured()).isFalse();
    }

    @Test
    @DisplayName("compte supprimé -> 401 INVALID_CREDENTIALS, Keycloak jamais appelé")
    void supprime() throws Exception {
      user.setDeletedAt(LocalDateTime.now());
      when(userRepository.findByPhone("221770000001")).thenReturn(Optional.of(user));
      assertThatThrownBy(() -> service.login(requete("221770000001")))
          .isInstanceOfSatisfying(
              CustomException.class,
              e -> {
                assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
              });
      verify(keycloakAuthService, never()).login(anyString(), anyString());
    }

    @Test
    @DisplayName("compte PRE_ENROLLED -> 403 ACCOUNT_PRE_ENROLLED")
    void preInscrit() {
      user.setAccountStatus(AccountStatus.PRE_ENROLLED);
      when(userRepository.findByPhone("221770000001")).thenReturn(Optional.of(user));
      assertThatThrownBy(() -> service.login(requete("221770000001")))
          .isInstanceOfSatisfying(
              CustomException.class,
              e -> {
                assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(e.getCode()).isEqualTo(ErrorCode.ACCOUNT_PRE_ENROLLED);
              });
    }

    @Test
    @DisplayName("compte désactivé -> 403 ACCOUNT_DISABLED")
    void desactive() {
      user.setActive(false);
      when(userRepository.findByPhone("221770000001")).thenReturn(Optional.of(user));
      assertThatThrownBy(() -> service.login(requete("221770000001")))
          .isInstanceOfSatisfying(
              CustomException.class,
              e -> assertThat(e.getCode()).isEqualTo(ErrorCode.ACCOUNT_DISABLED));
    }

    @Test
    @DisplayName("absent de la base : repli direct sur Keycloak, sans session PIN")
    void repliKeycloak() throws Exception {
      LoginResponse jetons = new LoginResponse();
      when(userRepository.findByPhone("inconnu@test.sn")).thenReturn(Optional.empty());
      when(userRepository.findByEmail("inconnu@test.sn")).thenReturn(Optional.empty());
      when(keycloakAuthService.login("inconnu@test.sn", "Secret@2026")).thenReturn(jetons);

      assertThat(service.login(requete("inconnu@test.sn"))).isSameAs(jetons);
      verifyNoInteractions(userSessionService);
    }

    @Test
    @DisplayName("erreur Keycloak propagée, aucune session créée")
    void erreurKeycloak() throws Exception {
      when(userRepository.findByPhone("221770000001")).thenReturn(Optional.of(user));
      when(keycloakAuthService.login(anyString(), anyString()))
          .thenThrow(new CustomException(HttpStatus.UNAUTHORIZED, "KO"));
      assertThatThrownBy(() -> service.login(requete("221770000001")))
          .isInstanceOf(CustomException.class);
      verifyNoInteractions(userSessionService);
    }
  }

  @Nested
  @DisplayName("logout")
  class Deconnexion {

    @Test
    @DisplayName("avec clientType : seule la session de ce type est révoquée")
    void parType() throws Exception {
      UserSession session = UserSession.builder().id(UUID.randomUUID()).user(user).build();
      when(userRepository.findByKeycloakId(user.getKeycloakId())).thenReturn(Optional.of(user));
      when(userSessionService.findActiveSession(user.getId(), ClientType.WEB))
          .thenReturn(Optional.of(session));

      service.logout(jwt(user.getKeycloakId()), ClientType.WEB);

      verify(userSessionService).revokeSession(session.getId());
      verify(userSessionService, never()).revokeAllSessions(any());
    }

    @Test
    @DisplayName("échec de révocation non bloquant")
    void revocationEchoue() throws Exception {
      UserSession session = UserSession.builder().id(UUID.randomUUID()).user(user).build();
      when(userRepository.findByKeycloakId(user.getKeycloakId())).thenReturn(Optional.of(user));
      when(userSessionService.findActiveSession(user.getId(), ClientType.WEB))
          .thenReturn(Optional.of(session));
      org.mockito.Mockito.doThrow(new CustomException(HttpStatus.NOT_FOUND, "absente"))
          .when(userSessionService)
          .revokeSession(session.getId());

      service.logout(jwt(user.getKeycloakId()), ClientType.WEB);
    }

    @Test
    @DisplayName("sans clientType : toutes les sessions révoquées")
    void toutes() {
      when(userRepository.findByKeycloakId(user.getKeycloakId())).thenReturn(Optional.of(user));
      service.logout(jwt(user.getKeycloakId()), null);
      verify(userSessionService).revokeAllSessions(user.getId());
    }

    @Test
    @DisplayName("jeton illisible, sans sub ou utilisateur inconnu : rien n'est révoqué")
    void jetonInexploitable() {
      service.logout("pas-un-jwt", null);
      service.logout("a.!!!.c", null);
      service.logout(jwt(null), null);
      when(userRepository.findByKeycloakId("fantome")).thenReturn(Optional.empty());
      service.logout(jwt("fantome"), null);
      verifyNoInteractions(userSessionService);
    }
  }
}
