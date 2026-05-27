package com.africa.dinthialma_backend.auth.dto;

import com.africa.dinthialma_backend.auth.entity.User;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Réponse profil utilisateur (lecture). */
@Getter
@AllArgsConstructor
public class UserProfileResponse {

  private UUID id;
  private String firstName;
  private String lastName;
  private String phone;
  private String email;
  private String avatarUrl;
  private boolean active;
  private boolean pinConfigured;
  private boolean pinExpired;
  private Set<String> roles;
  private LocalDateTime createdAt;

  public static UserProfileResponse from(User user) {
    boolean pinExpired = false;
    if (user.getPinCreatedAt() != null) {
      pinExpired =
          user.getPinCreatedAt()
              .plusDays(com.africa.dinthialma_backend.common.constants.Constants.Pin.EXPIRY_DAYS)
              .isBefore(LocalDateTime.now());
    }

    Set<String> roleNames =
        user.getRoles().stream()
            .map(r -> r.getRole().getKeycloakRole())
            .collect(Collectors.toSet());

    return new UserProfileResponse(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getPhone(),
        user.getEmail(),
        user.getAvatarUrl(),
        user.isActive(),
        user.getPinHash() != null,
        pinExpired,
        roleNames,
        user.getCreatedAt());
  }
}
