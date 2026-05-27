package com.africa.dinthialma_backend.member.dto;

import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/** Représentation d'un cotisant retournée par l'API. */
@Getter
@Builder
public class MembreResponse {

  private UUID id;
  private UUID tontineId;
  private UserInfo user;
  private Integer ordreJackpot;
  private MembreStatut statut;
  private LocalDate dateAdhesion;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /** Construit un MembreResponse depuis une entité TontineMembre. */
  public static MembreResponse from(TontineMembre membre) {
    UserInfo userInfo = null;
    if (membre.getUser() != null) {
      var u = membre.getUser();
      userInfo =
          UserInfo.builder()
              .id(u.getId())
              .firstName(u.getFirstName())
              .lastName(u.getLastName())
              .phone(u.getPhone())
              .email(u.getEmail())
              .build();
    }

    return MembreResponse.builder()
        .id(membre.getId())
        .tontineId(membre.getTontine() != null ? membre.getTontine().getId() : null)
        .user(userInfo)
        .ordreJackpot(membre.getOrdreJackpot())
        .statut(membre.getStatut())
        .dateAdhesion(membre.getDateAdhesion())
        .createdAt(membre.getCreatedAt())
        .updatedAt(membre.getUpdatedAt())
        .build();
  }

  /** Informations résumées de l'utilisateur cotisant. */
  @Getter
  @Builder
  public static class UserInfo {
    private UUID id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
  }
}
