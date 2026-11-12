package com.africa.dinthialma_backend.me.service.impl;

import com.africa.dinthialma_backend.auth.dto.LoginResponse;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserSession;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserSessionRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.auth.service.interfaces.UserSessionService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.exception.UnAuthorizedException;
import com.africa.dinthialma_backend.me.dto.ChangePasswordRequest;
import com.africa.dinthialma_backend.me.dto.PreferencesDto;
import com.africa.dinthialma_backend.me.dto.SessionResponse;
import com.africa.dinthialma_backend.me.service.interfaces.CompteService;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompteServiceImpl implements CompteService {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final UserRepository userRepository;
  private final UserSessionRepository sessionRepository;
  private final UserSessionService userSessionService;
  private final KeycloakAuthService keycloakAuthService;
  private final TontineRepository tontineRepository;

  // ─── B21 : mot de passe ──────────────────────────────────────────────────

  @Override
  @Transactional
  public void changerMotDePasse(
      String keycloakId, String sessionIdCourante, ChangePasswordRequest request)
      throws CustomException {
    User user = findUser(keycloakId);

    if (request.getAncienMotDePasse().equals(request.getNouveauMotDePasse())) {
      throw new BadRequestException(
          "Le nouveau mot de passe doit être différent de l'ancien", ErrorCode.PASSWORD_UNCHANGED);
    }

    // Vérification de l'ancien mot de passe auprès de Keycloak ; la session de vérification
    // ainsi ouverte est immédiatement refermée.
    LoginResponse verification;
    try {
      verification = keycloakAuthService.login(user.getPhone(), request.getAncienMotDePasse());
    } catch (UnAuthorizedException e) {
      throw new BadRequestException(
          "Le mot de passe actuel est incorrect", ErrorCode.CURRENT_PASSWORD_INVALID);
    }
    keycloakAuthService.logout(verification.getRefreshToken());

    keycloakAuthService.resetPassword(user.getKeycloakId(), request.getNouveauMotDePasse());
    log.info("Mot de passe changé – userId={}", user.getId());

    if (request.isDeconnecterAutresAppareils()) {
      for (UserSession s : sessionRepository.findByUserIdOrderByLastUsedAtDesc(user.getId())) {
        if (!estCourante(s, sessionIdCourante)) {
          revoquer(s);
        }
      }
    }
  }

  // ─── B20 : appareils connectés ───────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<SessionResponse> listerSessions(String keycloakId, String sessionIdCourante)
      throws CustomException {
    User user = findUser(keycloakId);
    LocalDateTime maintenant = LocalDateTime.now();
    return sessionRepository.findByUserIdOrderByLastUsedAtDesc(user.getId()).stream()
        .map(
            s ->
                SessionResponse.builder()
                    .id(s.getId())
                    .clientType(s.getClientType())
                    .deviceInfo(s.getDeviceInfo())
                    .derniereActivite(s.getLastUsedAt())
                    .expireLe(s.getExpiresAt())
                    .expiree(s.getExpiresAt() != null && s.getExpiresAt().isBefore(maintenant))
                    .courante(estCourante(s, sessionIdCourante))
                    .build())
        .toList();
  }

  @Override
  @Transactional
  public void revoquerSession(String keycloakId, UUID sessionId) throws CustomException {
    User user = findUser(keycloakId);
    UserSession session =
        sessionRepository
            .findById(sessionId)
            .filter(s -> s.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.SESSION_NOT_FOUND));
    revoquer(session);
    log.info("Session révoquée à distance – userId={} sessionId={}", user.getId(), sessionId);
  }

  // ─── B22 : préférences ───────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public PreferencesDto getPreferences(String keycloakId) throws CustomException {
    User user = findUser(keycloakId);
    PreferencesDto prefs =
        user.getPreferences() != null ? user.getPreferences() : new PreferencesDto();
    // Tontine supprimée ou plus gérée depuis l'enregistrement : renvoyée à null (lecture seule)
    prefs.setDerniereTontineGeree(tontineGereeOuNull(user, prefs.getDerniereTontineGeree()));
    return prefs;
  }

  @Override
  @Transactional
  public PreferencesDto savePreferences(String keycloakId, PreferencesDto preferences)
      throws CustomException {
    User user = findUser(keycloakId);
    PreferencesDto prefs = preferences != null ? preferences : new PreferencesDto();
    prefs.setTontinesFavorites(
        prefs.getTontinesFavorites() == null
            ? new ArrayList<>()
            : new ArrayList<>(new LinkedHashSet<>(prefs.getTontinesFavorites())));
    prefs.setDerniereTontineGeree(tontineGereeOuNull(user, prefs.getDerniereTontineGeree()));
    user.setPreferences(prefs);
    userRepository.save(user);
    return prefs;
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  /** La tontine si l'utilisateur en est le gestionnaire (non supprimée), sinon null. */
  private UUID tontineGereeOuNull(User user, UUID tontineId) {
    if (tontineId == null
        || !tontineRepository.existsByIdAndCreePar_IdAndDeletedAtIsNull(tontineId, user.getId())) {
      return null;
    }
    return tontineId;
  }

  /** Révoque la session Keycloak (non bloquant) puis supprime la session locale. */
  private void revoquer(UserSession session) {
    try {
      keycloakAuthService.logout(userSessionService.decryptRefreshToken(session));
    } catch (Exception e) {
      log.warn("Révocation Keycloak impossible (session expirée ?) : {}", e.getMessage());
    }
    sessionRepository.delete(session);
  }

  /** Compare le sid du refresh token stocké avec celui du JWT de la requête. */
  private boolean estCourante(UserSession session, String sessionIdCourante) {
    if (sessionIdCourante == null) return false;
    try {
      String refreshToken = userSessionService.decryptRefreshToken(session);
      String[] parts = refreshToken.split("\\.");
      if (parts.length < 2) return false;
      JsonNode payload =
          JSON.readTree(
              new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
      String sid =
          payload.hasNonNull("sid")
              ? payload.get("sid").asText()
              : payload.path("session_state").asText(null);
      return Objects.equals(sid, sessionIdCourante);
    } catch (Exception e) {
      return false;
    }
  }

  private User findUser(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }
}
