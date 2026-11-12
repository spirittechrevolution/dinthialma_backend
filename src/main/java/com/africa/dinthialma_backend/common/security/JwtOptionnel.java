package com.africa.dinthialma_backend.common.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Identification facultative sur une route publique (chaîne de sécurité sans JWT).
 *
 * <p>Si un en-tête {@code Authorization: Bearer …} porte un jeton valide (signature, émetteur,
 * expiration vérifiés par le décodeur Keycloak de l'application), renvoie son {@code sub}
 * (keycloakId). Jeton absent, mal formé ou expiré : {@link Optional#empty()} — jamais d'erreur, la
 * route reste publique.
 */
@Component
@RequiredArgsConstructor
public class JwtOptionnel {

  private static final String PREFIXE = "Bearer ";

  private final JwtDecoder jwtDecoder;

  public Optional<String> keycloakId(HttpServletRequest request) {
    String entete = request.getHeader("Authorization");
    if (entete == null || !entete.regionMatches(true, 0, PREFIXE, 0, PREFIXE.length())) {
      return Optional.empty();
    }
    String jeton = entete.substring(PREFIXE.length()).trim();
    if (jeton.isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(jwtDecoder.decode(jeton).getSubject());
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
