package com.africa.dinthialma_backend.member.dto;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.fasterxml.jackson.annotation.JsonProperty;
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

  @Schema(
      description =
          "true si le membre a déjà été gagnant d'au moins un cycle de cette tontine"
              + " (un membre ne peut gagner qu'une fois par tontine)",
      example = "false")
  @JsonProperty("aRecuJackpot")
  private boolean aRecuJackpot;

  /**
   * Getter explicite : celui généré par Lombok ({@code isARecuJackpot}) serait sérialisé par
   * Jackson sous {@code arecuJackpot}, en plus de {@code aRecuJackpot}. Le front attend exactement
   * {@code aRecuJackpot}.
   */
  @JsonProperty("aRecuJackpot")
  public boolean isARecuJackpot() {
    return aRecuJackpot;
  }

  @Schema(
      description = "Date de son jackpot dans cette tontine – null s'il n'a pas encore gagné",
      nullable = true)
  private LocalDateTime dateJackpot;

  @Schema(description = "Date d'ajout du membre")
  private LocalDateTime createdAt;

  @Schema(description = "Date de dernière modification du statut")
  private LocalDateTime updatedAt;

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

    @Schema(
        description =
            "Statut d'activation du compte. PRE_ENROLLED = ajouté par l'admin, pas encore"
                + " inscrit sur l'application.",
        allowableValues = {"ACTIVE", "PRE_ENROLLED"})
    private AccountStatus accountStatus;
  }
}
