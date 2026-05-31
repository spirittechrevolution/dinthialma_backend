package com.africa.dinthialma_backend.auth.service.impl;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.dto.LoginRequest;
import com.africa.dinthialma_backend.auth.dto.LoginResponse;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.auth.service.interfaces.LoginService;
import com.africa.dinthialma_backend.auth.service.interfaces.UserSessionService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.UnAuthorizedException;
import com.africa.dinthialma_backend.common.util.PhoneUtils;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Résout le {@code username} (téléphone OU email) vers le username Keycloak exact, authentifie via
 * ROPC, puis crée/met à jour la session PIN en base.
 *
 * <p>Flux complet :
 *
 * <ol>
 *   <li>Normalisation du téléphone ({@link PhoneUtils#normalize}).
 *   <li>Recherche en base : par téléphone → par email.
 *   <li>Vérifications du compte (actif, non supprimé).
 *   <li>Appel Keycloak ROPC → tokens JWT.
 *   <li>Persistance de la session PIN (refresh token chiffré AES-256-GCM).
 * </ol>
 *
 * <p>Fallback : si l'utilisateur est absent de la base (bootstrap partiel), le login Keycloak est
 * tenté directement – les tokens sont retournés mais aucune session PIN n'est créée.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

  private final UserRepository userRepository;
  private final KeycloakAuthService keycloakAuthService;
  private final UserSessionService userSessionService;

  @Override
  @Transactional
  public LoginResponse login(LoginRequest request) throws CustomException {
    String identifier = PhoneUtils.normalize(request.getUsername().trim());
    log.debug("[Login] Identifiant normalisé : {}", identifier);

    // ── 1. Recherche en base ──────────────────────────────────────────
    Optional<User> maybeUser = findUser(identifier);

    if (maybeUser.isPresent()) {
      User user = maybeUser.get();

      if (user.getDeletedAt() != null) {
        log.warn("[Login] Compte supprimé – identifiant : {}", identifier);
        throw new UnAuthorizedException("Identifiants incorrects");
      }
      if (user.getAccountStatus() == AccountStatus.PRE_ENROLLED) {
        log.warn(
            "[Login] Tentative de connexion sur compte PRE_ENROLLED – userId : {}", user.getId());
        throw new CustomException(
            HttpStatus.FORBIDDEN, ResponseMessageConstants.AUTH_ACCOUNT_NOT_ACTIVATED);
      }
      if (!user.isActive()) {
        log.warn("[Login] Compte désactivé – userId : {}", user.getId());
        throw new CustomException(
            HttpStatus.FORBIDDEN, ResponseMessageConstants.AUTH_ACCOUNT_DISABLED);
      }

      // ── 2. Authentification Keycloak ──────────────────────────────
      log.debug("[Login] username Keycloak : {}", user.getPhone());
      LoginResponse tokens = keycloakAuthService.login(user.getPhone(), request.getPassword());

      // ── 3. Création / mise à jour de la session PIN ───────────────
      userSessionService.createOrUpdateSession(
          user, request.getClientType(), tokens.getRefreshToken(), request.getDeviceInfo());

      log.info(
          "[Login] Connexion réussie – userId={} clientType={}",
          user.getId(),
          request.getClientType());
      return tokens;
    }

    // ── Fallback : base vide / bootstrap partiel ──────────────────────
    // Keycloak reste la source de vérité mais aucune session PIN n'est créée.
    log.warn(
        "[Login] Utilisateur '{}' absent de la base locale – tentative directe Keycloak (pas de session PIN)",
        identifier);
    return keycloakAuthService.login(identifier, request.getPassword());
  }

  // ─── Helpers ─────────────────────────────────────────────────────────

  private Optional<User> findUser(String identifier) {
    Optional<User> byPhone = userRepository.findByPhone(identifier);
    if (byPhone.isPresent()) {
      log.debug("[Login] Résolution par téléphone");
      return byPhone;
    }
    if (identifier.contains("@")) {
      Optional<User> byEmail = userRepository.findByEmail(identifier);
      if (byEmail.isPresent()) {
        log.debug("[Login] Résolution par email");
        return byEmail;
      }
    }
    return Optional.empty();
  }
}
