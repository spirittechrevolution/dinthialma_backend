package com.africa.dinthialma_backend.auth.service.impl;

import com.africa.dinthialma_backend.auth.dto.LoginRequest;
import com.africa.dinthialma_backend.auth.dto.LoginResponse;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.auth.service.interfaces.LoginService;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.UnAuthorizedException;
import com.africa.dinthialma_backend.common.util.PhoneUtils;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Résout le {@code username} (téléphone OU email) vers le username Keycloak exact avant d'initier
 * l'authentification ROPC.
 *
 * <p>Stratégie de résolution :
 *
 * <ol>
 *   <li>Normalisation du téléphone : suppression du {@code +} initial via {@link PhoneUtils}.
 *   <li>Recherche en base par téléphone normalisé.
 *   <li>Recherche en base par email (si {@code @} dans l'identifiant).
 *   <li>Si non trouvé en base (base vide / bootstrap incomplet) → tentative directe sur Keycloak
 *       avec l'identifiant normalisé. Keycloak reste la source de vérité pour les credentials.
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

  private final UserRepository userRepository;
  private final KeycloakAuthService keycloakAuthService;

  @Override
  public LoginResponse login(LoginRequest request) throws CustomException {
    // Normalise : strip "+", trim espaces
    String raw = request.getUsername().trim();
    String identifier = PhoneUtils.normalize(raw);
    log.debug("[Login] Tentative de connexion – identifiant normalisé : {}", identifier);

    // ── 1. Recherche en base ──────────────────────────────────────────
    Optional<User> maybeUser = findUser(identifier);

    if (maybeUser.isPresent()) {
      User user = maybeUser.get();

      if (user.getDeletedAt() != null) {
        log.warn("[Login] Compte supprimé – identifiant : {}", identifier);
        throw new UnAuthorizedException("Identifiants incorrects");
      }
      if (!user.isActive()) {
        log.warn("[Login] Compte désactivé – userId : {}", user.getId());
        throw new CustomException(HttpStatus.FORBIDDEN, "Votre compte est désactivé");
      }

      // user.getPhone() est le username exact dans Keycloak (déjà normalisé sans +)
      log.debug("[Login] Utilisateur trouvé en base → username Keycloak : {}", user.getPhone());
      LoginResponse tokens = keycloakAuthService.login(user.getPhone(), request.getPassword());
      log.info("[Login] Connexion réussie – userId : {}", user.getId());
      return tokens;
    }

    // ── 2. Fallback : base vide ou désynchronisée → direct Keycloak ───
    log.warn("[Login] '{}' absent de la base locale – tentative directe Keycloak", identifier);
    return keycloakAuthService.login(identifier, request.getPassword());
  }

  // ─── Helpers ─────────────────────────────────────────────────────────

  private Optional<User> findUser(String identifier) {
    // Recherche par téléphone (déjà normalisé sans +)
    Optional<User> byPhone = userRepository.findByPhone(identifier);
    if (byPhone.isPresent()) {
      log.debug("[Login] Résolution par téléphone");
      return byPhone;
    }

    // Recherche par email
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
