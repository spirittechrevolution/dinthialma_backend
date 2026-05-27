package com.africa.dinthialma_backend.admin.dto;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserRoleAssignment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * Vue détaillée d'un utilisateur pour l'interface d'administration (SUPER_ADMIN).
 *
 * <p>Utilisé pour la liste paginée et le détail individuel.
 */
@Getter
@Builder
public class AdminUserResponse {

  private UUID id;
  private String keycloakId;
  private String firstName;
  private String lastName;
  private String phone;
  private String email;
  private String avatarUrl;
  private boolean active;
  private boolean pinConfigured;
  private List<String> roles;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /**
   * Construit un AdminUserResponse depuis une entité User.
   *
   * <p>Note : {@code user.getRoles()} doit être initialisé (EAGER ou dans une transaction).
   */
  public static AdminUserResponse from(User user) {
    List<String> roleNames =
        user.getRoles().stream()
            .map(UserRoleAssignment::getRole)
            .map(r -> r.getKeycloakRole())
            .sorted()
            .toList();

    return AdminUserResponse.builder()
        .id(user.getId())
        .keycloakId(user.getKeycloakId())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .phone(user.getPhone())
        .email(user.getEmail())
        .avatarUrl(user.getAvatarUrl())
        .active(user.isActive())
        .pinConfigured(user.getPinHash() != null)
        .roles(roleNames)
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }
}
