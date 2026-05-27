package com.africa.dinthialma_backend.common.util;

import com.africa.dinthialma_backend.auth.dto.RequestUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Utilitaire pour extraire les informations de l'utilisateur courant depuis le JWT Keycloak. */
@Component
public class RequestHeaderParser {

  /**
   * Extrait le RequestUser depuis le JWT présent dans le SecurityContext.
   *
   * @return RequestUser contenant le sub (Keycloak ID) et les rôles realm
   */
  public RequestUser getRequestUser(HttpServletRequest request) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String sub = jwt.getSubject();

      // Extraire les rôles realm Keycloak
      java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
      List<String> roles = List.of();
      if (realmAccess != null && realmAccess.containsKey("roles")) {
        @SuppressWarnings("unchecked")
        List<String> rawRoles = (List<String>) realmAccess.get("roles");
        roles = rawRoles.stream().collect(Collectors.toList());
      }

      return new RequestUser(sub, roles);
    }

    return null;
  }

  /**
   * Extrait directement le Keycloak ID (sub) depuis le JWT.
   *
   * @return sub du JWT (UUID Keycloak de l'utilisateur connecté)
   * @throws IllegalStateException si aucun JWT n'est présent dans le contexte
   */
  public String extractKeycloakId(HttpServletRequest request) {
    RequestUser user = getRequestUser(request);
    if (user == null || user.getSub() == null) {
      throw new IllegalStateException("Aucun utilisateur authentifié dans le contexte");
    }
    return user.getSub();
  }
}
