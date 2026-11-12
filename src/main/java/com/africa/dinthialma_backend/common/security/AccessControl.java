package com.africa.dinthialma_backend.common.security;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Contrôles d'accès applicatifs, utilisés par la couche service (jamais par les contrôleurs).
 *
 * <p>Les rôles sont lus en base ({@code user_roles}), comme dans le reste des services : la base
 * est la référence applicative, Keycloak en est la copie pour les jetons.
 */
@Component
@RequiredArgsConstructor
public class AccessControl {

  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository roleAssignmentRepository;

  /** Utilisateur correspondant au {@code sub} du JWT. */
  public User utilisateur(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }

  public boolean aUnRole(User user, UserRole... roles) {
    return Arrays.stream(roles)
        .anyMatch(r -> roleAssignmentRepository.existsByUserIdAndRole(user.getId(), r));
  }

  public boolean estSuperAdmin(User user) {
    return aUnRole(user, UserRole.SUPER_ADMIN);
  }

  /**
   * Exige au moins un des rôles.
   *
   * @return l'utilisateur appelant
   * @throws ForbiddenException si aucun rôle ne correspond (403)
   */
  public User exigerUnRole(String keycloakId, UserRole... roles) throws CustomException {
    User user = utilisateur(keycloakId);
    if (!aUnRole(user, roles)) {
      throw new ForbiddenException(
          "Accès refusé – rôle requis : "
              + Arrays.stream(roles)
                  .map(UserRole::getKeycloakRole)
                  .collect(Collectors.joining(" ou ")));
    }
    return user;
  }
}
