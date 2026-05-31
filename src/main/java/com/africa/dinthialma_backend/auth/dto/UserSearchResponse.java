package com.africa.dinthialma_backend.auth.dto;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Résultat de recherche d'un utilisateur par numéro de téléphone")
@Getter
@Builder
public class UserSearchResponse {

  @Schema(description = "UUID interne de l'utilisateur")
  private UUID id;

  @Schema(description = "Prénom", example = "Fatou")
  private String firstName;

  @Schema(description = "Nom de famille", example = "Sow")
  private String lastName;

  @Schema(description = "Numéro de téléphone normalisé", example = "221770000001")
  private String phone;

  @Schema(
      description = "Statut d'activation du compte",
      allowableValues = {"ACTIVE", "PRE_ENROLLED"})
  private AccountStatus accountStatus;

  public static UserSearchResponse from(User user) {
    return UserSearchResponse.builder()
        .id(user.getId())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .phone(user.getPhone())
        .accountStatus(
            user.getAccountStatus() != null ? user.getAccountStatus() : AccountStatus.ACTIVE)
        .build();
  }
}
