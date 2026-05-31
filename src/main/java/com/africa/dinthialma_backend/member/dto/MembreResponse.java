package com.africa.dinthialma_backend.member.dto;

import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Représentation d'un cotisant membre d'une tontine")
@Getter
@Builder
public class MembreResponse {

  @Schema(description = "UUID du membership (tontine_membres.id)")
  private UUID id;

  @Schema(description = "UUID de la tontine parente")
  private UUID tontineId;

  @Schema(description = "Informations de l'utilisateur cotisant")
  private UserInfo user;

  @Schema(
      description = "Position dans la rotation des jackpots (1 = premier bénéficiaire)",
      example = "3")
  private Integer ordreJackpot;

  @Schema(
      description = "Statut du cotisant",
      allowableValues = {"ACTIF", "SUSPENDU", "SORTI"})
  private MembreStatut statut;

  @Schema(description = "Date d'adhésion à la tontine", example = "2024-06-15")
  private LocalDate dateAdhesion;

  @Schema(description = "Date d'ajout du membre")
  private LocalDateTime createdAt;

  @Schema(description = "Date de dernière modification du statut")
  private LocalDateTime updatedAt;

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

  @Schema(description = "Informations résumées de l'utilisateur cotisant")
  @Getter
  @Builder
  public static class UserInfo {

    @Schema(description = "UUID interne de l'utilisateur")
    private UUID id;

    @Schema(description = "Prénom", example = "Fatou")
    private String firstName;

    @Schema(description = "Nom de famille", example = "Sow")
    private String lastName;

    @Schema(description = "Numéro de téléphone normalisé", example = "221770000001")
    private String phone;

    @Schema(description = "Adresse email (peut être null)")
    private String email;
  }
}
