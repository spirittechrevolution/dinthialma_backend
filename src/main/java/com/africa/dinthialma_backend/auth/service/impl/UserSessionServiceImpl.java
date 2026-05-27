package com.africa.dinthialma_backend.auth.service.impl;

import com.africa.dinthialma_backend.auth.codeList.ClientType;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserSession;
import com.africa.dinthialma_backend.auth.repository.UserSessionRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.UserSessionService;
import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.util.TokenEncryptionUtil;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des sessions PIN – crée, met à jour et révoque les sessions WEB/MOBILE.
 *
 * <p>Le refresh token Keycloak est chiffré avec AES-256-GCM avant persistance. La clé est fournie
 * via {@code app.security.token-encryption-key}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

  private final UserSessionRepository userSessionRepository;

  @Value("${app.security.token-encryption-key}")
  private String tokenEncryptionKey;

  // ─── Création / mise à jour ────────────────────────────────────────

  @Override
  @Transactional
  public UserSession createOrUpdateSession(
      User user, ClientType clientType, String refreshToken, String deviceInfo)
      throws CustomException {

    log.debug("createOrUpdateSession – userId={} clientType={}", user.getId(), clientType);

    // Chiffrement du refresh token avant stockage
    String encryptedToken = TokenEncryptionUtil.encrypt(refreshToken, tokenEncryptionKey);

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime expiresAt = computeExpiry(clientType, now);

    // Chercher une session existante (active ou expirée) pour ce couple user/client
    Optional<UserSession> existing =
        userSessionRepository.findByUserIdAndClientType(user.getId(), clientType);

    UserSession session;
    if (existing.isPresent()) {
      // Mise à jour de la session existante (refresh token + TTL)
      session = existing.get();
      session.setRefreshTokenHash(encryptedToken);
      session.setLastUsedAt(now);
      session.setExpiresAt(expiresAt);
      if (deviceInfo != null && !deviceInfo.isBlank()) {
        session.setDeviceInfo(deviceInfo);
      }
      log.debug("Session existante mise à jour – sessionId={}", session.getId());
    } else {
      // Nouvelle session
      session =
          UserSession.builder()
              .user(user)
              .clientType(clientType)
              .refreshTokenHash(encryptedToken)
              .deviceInfo(deviceInfo)
              .lastUsedAt(now)
              .expiresAt(expiresAt)
              .build();
      log.debug("Nouvelle session créée pour userId={} clientType={}", user.getId(), clientType);
    }

    UserSession saved = userSessionRepository.save(session);
    log.info(
        "Session PIN {} – userId={} clientType={} expiresAt={}",
        existing.isPresent() ? "mise à jour" : "créée",
        user.getId(),
        clientType,
        expiresAt);
    return saved;
  }

  // ─── Recherche ────────────────────────────────────────────────────

  @Override
  public Optional<UserSession> findActiveSession(UUID userId, ClientType clientType) {
    return userSessionRepository.findByUserIdAndClientTypeAndExpiresAtAfter(
        userId, clientType, LocalDateTime.now());
  }

  // ─── Révocation ───────────────────────────────────────────────────

  @Override
  @Transactional
  public void revokeSession(UUID sessionId) throws CustomException {
    UserSession session =
        userSessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Session introuvable"));

    userSessionRepository.delete(session);
    log.info("Session révoquée – sessionId={}", sessionId);
  }

  @Override
  @Transactional
  public void revokeAllSessions(UUID userId) {
    userSessionRepository.deleteAllByUserId(userId);
    log.info("Toutes les sessions révoquées – userId={}", userId);
  }

  // ─── Déchiffrement ────────────────────────────────────────────────

  @Override
  public String decryptRefreshToken(UserSession session) throws CustomException {
    try {
      return TokenEncryptionUtil.decrypt(session.getRefreshTokenHash(), tokenEncryptionKey);
    } catch (Exception e) {
      log.error("Impossible de déchiffrer le refresh token pour sessionId={}", session.getId());
      throw new CustomException(
          org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
          "Erreur lors de la récupération de la session");
    }
  }

  // ─── Helpers ──────────────────────────────────────────────────────

  private LocalDateTime computeExpiry(ClientType clientType, LocalDateTime from) {
    return switch (clientType) {
      case WEB -> from.plusHours(Constants.Session.WEB_TTL_HOURS);
      case MOBILE -> from.plusDays(Constants.Session.MOBILE_TTL_DAYS);
    };
  }
}
