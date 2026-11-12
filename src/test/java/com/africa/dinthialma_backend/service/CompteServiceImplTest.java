package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.codeList.ClientType;
import com.africa.dinthialma_backend.auth.dto.LoginResponse;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserSession;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserSessionRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.auth.service.interfaces.UserSessionService;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.exception.UnAuthorizedException;
import com.africa.dinthialma_backend.me.codeList.ModeActivite;
import com.africa.dinthialma_backend.me.dto.ChangePasswordRequest;
import com.africa.dinthialma_backend.me.dto.PreferencesDto;
import com.africa.dinthialma_backend.me.dto.SessionResponse;
import com.africa.dinthialma_backend.me.service.impl.CompteServiceImpl;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("CompteServiceImpl")
class CompteServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private UserSessionRepository sessionRepository;
  @Mock private UserSessionService userSessionService;
  @Mock private KeycloakAuthService keycloakAuthService;
  @Mock private TontineRepository tontineRepository;
  @InjectMocks private CompteServiceImpl service;

  private User user;

  @BeforeEach
  void setUp() {
    user = Fixtures.user("Awa");
    lenient()
        .when(userRepository.findByKeycloakId(user.getKeycloakId()))
        .thenReturn(Optional.of(user));
  }

  /** Jeton JWT non signé portant le claim donné (sid ou session_state). */
  private static String jeton(String claim, String valeur) {
    Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
    String payload = claim == null ? "{}" : "{\"" + claim + "\":\"" + valeur + "\"}";
    return enc.encodeToString("{}".getBytes(StandardCharsets.UTF_8))
        + "."
        + enc.encodeToString(payload.getBytes(StandardCharsets.UTF_8))
        + ".sig";
  }

  private UserSession session(ClientType type, String refresh, LocalDateTime expiration)
      throws CustomException {
    UserSession s =
        UserSession.builder()
            .id(UUID.randomUUID())
            .user(user)
            .clientType(type)
            .deviceInfo(type.name())
            .lastUsedAt(LocalDateTime.now())
            .expiresAt(expiration)
            .build();
    lenient().when(userSessionService.decryptRefreshToken(s)).thenReturn(refresh);
    return s;
  }

  private static ChangePasswordRequest changement(String ancien, String nouveau, boolean autres) {
    ChangePasswordRequest r = new ChangePasswordRequest();
    r.setAncienMotDePasse(ancien);
    r.setNouveauMotDePasse(nouveau);
    r.setDeconnecterAutresAppareils(autres);
    return r;
  }

  @Test
  @DisplayName("changement de mot de passe : ancien vérifié, session de vérification fermée, reset")
  void changementMotDePasse() throws Exception {
    LoginResponse verif = new LoginResponse();
    verif.setRefreshToken("verif");
    when(keycloakAuthService.login(user.getPhone(), "Ancien@1")).thenReturn(verif);

    service.changerMotDePasse(
        user.getKeycloakId(), "sid-1", changement("Ancien@1", "Nouveau@1", false));

    verify(keycloakAuthService).logout("verif");
    verify(keycloakAuthService).resetPassword(user.getKeycloakId(), "Nouveau@1");
    verify(sessionRepository, never()).findByUserIdOrderByLastUsedAtDesc(any());
  }

  @Test
  @DisplayName("avec déconnexion des autres appareils : seule la session courante est conservée")
  void deconnexionAutresAppareils() throws Exception {
    when(keycloakAuthService.login(anyString(), anyString())).thenReturn(new LoginResponse());
    UserSession courante = session(ClientType.WEB, jeton("sid", "sid-1"), null);
    UserSession autre = session(ClientType.MOBILE, jeton("session_state", "sid-2"), null);
    when(sessionRepository.findByUserIdOrderByLastUsedAtDesc(user.getId()))
        .thenReturn(List.of(courante, autre));

    service.changerMotDePasse(user.getKeycloakId(), "sid-1", changement("A@1", "B@1", true));

    verify(sessionRepository).delete(autre);
    verify(sessionRepository, never()).delete(courante);
    verify(keycloakAuthService).logout(jeton("session_state", "sid-2"));
  }

  @Test
  @DisplayName(
      "refus : mot de passe inchangé (PASSWORD_UNCHANGED), ancien faux (CURRENT_PASSWORD_INVALID)")
  void changementRefuse() throws Exception {
    assertThatThrownBy(
            () ->
                service.changerMotDePasse(
                    user.getKeycloakId(), null, changement("Meme@1", "Meme@1", false)))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PASSWORD_UNCHANGED));

    when(keycloakAuthService.login(anyString(), anyString()))
        .thenThrow(new UnAuthorizedException("KO"));
    assertThatThrownBy(
            () ->
                service.changerMotDePasse(
                    user.getKeycloakId(), null, changement("Faux@1", "Neuf@1", false)))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.CURRENT_PASSWORD_INVALID));
    verify(keycloakAuthService, never()).resetPassword(anyString(), anyString());
  }

  @Test
  @DisplayName(
      "liste des sessions : courante via le sid, expirée signalée ; jeton illisible -> non courante")
  void listeSessions() throws Exception {
    UserSession courante =
        session(ClientType.WEB, jeton("sid", "sid-1"), LocalDateTime.now().plusDays(1));
    UserSession expiree = session(ClientType.MOBILE, "illisible", LocalDateTime.now().minusDays(1));
    UserSession sansSid = session(ClientType.MOBILE, jeton(null, null), null);
    when(sessionRepository.findByUserIdOrderByLastUsedAtDesc(user.getId()))
        .thenReturn(List.of(courante, expiree, sansSid));

    List<SessionResponse> r = service.listerSessions(user.getKeycloakId(), "sid-1");

    assertThat(r).extracting(SessionResponse::isCourante).containsExactly(true, false, false);
    assertThat(r).extracting(SessionResponse::isExpiree).containsExactly(false, true, false);
    assertThat(r.get(0).getClientType()).isEqualTo(ClientType.WEB);

    assertThat(service.listerSessions(user.getKeycloakId(), null))
        .noneMatch(SessionResponse::isCourante);
  }

  @Test
  @DisplayName("révocation à distance : Keycloak puis suppression ; Keycloak en échec non bloquant")
  void revocation() throws Exception {
    UserSession s = session(ClientType.MOBILE, "refresh", null);
    when(sessionRepository.findById(s.getId())).thenReturn(Optional.of(s));
    doThrow(new CustomException(HttpStatus.BAD_GATEWAY, "KO"))
        .when(keycloakAuthService)
        .logout("refresh");

    service.revoquerSession(user.getKeycloakId(), s.getId());

    verify(sessionRepository).delete(s);
  }

  @Test
  @DisplayName("révocation d'une session d'un autre utilisateur ou inconnue -> 404")
  void revocationInterdite() {
    UserSession autre =
        UserSession.builder().id(UUID.randomUUID()).user(Fixtures.user("Autre")).build();
    when(sessionRepository.findById(autre.getId())).thenReturn(Optional.of(autre));
    assertThatThrownBy(() -> service.revoquerSession(user.getKeycloakId(), autre.getId()))
        .isInstanceOf(NotFoundException.class);
    UUID inconnue = UUID.randomUUID();
    when(sessionRepository.findById(inconnue)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.revoquerSession(user.getKeycloakId(), inconnue))
        .isInstanceOf(NotFoundException.class);
    verify(sessionRepository, never()).delete(any());
  }

  @Test
  @DisplayName("préférences : valeurs par défaut si absentes ; favoris dédoublonnés dans l'ordre")
  void preferences() throws Exception {
    assertThat(service.getPreferences(user.getKeycloakId()).getTontinesFavorites()).isEmpty();

    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    PreferencesDto p = new PreferencesDto();
    p.setTontinesFavorites(new ArrayList<>(List.of(a, b, a)));
    p.setDernierMode(ModeActivite.GERE);
    p.setMasquerMontant(true);

    PreferencesDto r = service.savePreferences(user.getKeycloakId(), p);

    assertThat(r.getTontinesFavorites()).containsExactly(a, b);
    assertThat(user.getPreferences()).isSameAs(r);
    verify(userRepository).save(user);
    assertThat(service.getPreferences(user.getKeycloakId()).isMasquerMontant()).isTrue();
  }

  @Test
  @DisplayName(
      "dernière tontine gérée : conservée si gestionnaire ; tontine d'un autre -> null sans refus ;"
          + " plus gérée à la relecture -> null")
  void derniereTontineGeree() throws Exception {
    UUID mienne = UUID.randomUUID();
    UUID autre = UUID.randomUUID();
    when(tontineRepository.existsByIdAndCreePar_IdAndDeletedAtIsNull(mienne, user.getId()))
        .thenReturn(true);

    PreferencesDto p = new PreferencesDto();
    p.setDerniereTontineGeree(mienne);
    p.setMasquerMontant(true);
    assertThat(service.savePreferences(user.getKeycloakId(), p).getDerniereTontineGeree())
        .isEqualTo(mienne);

    PreferencesDto q = new PreferencesDto();
    q.setDerniereTontineGeree(autre);
    q.setMasquerMontant(true);
    PreferencesDto r = service.savePreferences(user.getKeycloakId(), q);
    assertThat(r.getDerniereTontineGeree()).isNull();
    assertThat(r.isMasquerMontant()).isTrue();

    user.getPreferences().setDerniereTontineGeree(mienne);
    when(tontineRepository.existsByIdAndCreePar_IdAndDeletedAtIsNull(mienne, user.getId()))
        .thenReturn(false);
    assertThat(service.getPreferences(user.getKeycloakId()).getDerniereTontineGeree()).isNull();
  }

  @Test
  @DisplayName("préférences nulles ou favoris nuls : enregistrées vides")
  void preferencesNulles() throws Exception {
    assertThat(service.savePreferences(user.getKeycloakId(), null).getTontinesFavorites())
        .isEmpty();
    PreferencesDto p = new PreferencesDto();
    p.setTontinesFavorites(null);
    assertThat(service.savePreferences(user.getKeycloakId(), p).getTontinesFavorites()).isEmpty();
    when(userRepository.findByKeycloakId("x")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getPreferences("x")).isInstanceOf(NotFoundException.class);
  }
}
