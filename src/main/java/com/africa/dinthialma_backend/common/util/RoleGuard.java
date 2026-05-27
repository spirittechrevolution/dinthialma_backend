package com.africa.dinthialma_backend.common.util;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.dto.RequestUser;
import com.africa.dinthialma_backend.common.exception.UnAuthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/** Utilitaire de vérification des rôles Keycloak dans les contrôleurs. */
public final class RoleGuard {

  private RoleGuard() {}

  /**
   * Exige que l'appelant possède au moins un des rôles Keycloak spécifiés.
   *
   * @throws UnAuthorizedException si aucun rôle ne correspond
   */
  public static RequestUser requireAnyRole(
      RequestHeaderParser parser, HttpServletRequest request, UserRole... roles)
      throws UnAuthorizedException {

    RequestUser caller = parser.getRequestUser(request);

    if (caller == null) {
      throw new UnAuthorizedException("Authentification requise");
    }

    boolean hasRole =
        Arrays.stream(roles).anyMatch(role -> caller.getRoles().contains(role.getKeycloakRole()));

    if (!hasRole) {
      throw new UnAuthorizedException(
          "Accès refusé – rôle requis : "
              + Arrays.stream(roles)
                  .map(UserRole::getKeycloakRole)
                  .reduce((a, b) -> a + " ou " + b)
                  .orElse(""));
    }

    return caller;
  }

  /**
   * Exige le rôle SUPER_ADMIN ou ADMIN.
   *
   * @throws UnAuthorizedException si l'utilisateur n'est pas admin
   */
  public static RequestUser requireAdmin(RequestHeaderParser parser, HttpServletRequest request)
      throws UnAuthorizedException {
    return requireAnyRole(parser, request, UserRole.SUPER_ADMIN, UserRole.ADMIN);
  }

  /**
   * Exige le rôle SUPER_ADMIN.
   *
   * @throws UnAuthorizedException si l'utilisateur n'est pas super admin
   */
  public static RequestUser requireSuperAdmin(
      RequestHeaderParser parser, HttpServletRequest request) throws UnAuthorizedException {
    return requireAnyRole(parser, request, UserRole.SUPER_ADMIN);
  }
}
