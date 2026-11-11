package com.africa.dinthialma_backend.me.dto;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(
    description =
        "Résolution d'un QR scanné. Le numéro de téléphone n'est jamais renvoyé : le gestionnaire"
            + " voit seulement l'identité et, pour ses propres tontines, la situation du membre.")
@Getter
@Builder
public class QrResolutionResponse {

  @Schema(
      description = "Nature du QR (TONTINE réservé aux invitations, à venir)",
      allowableValues = {"MEMBRE"})
  private String type;

  @Schema(description = "Personne identifiée par le QR")
  private Personne user;

  @Schema(
      description =
          "Tontines gérées par l'appelant dans lesquelles cette personne est inscrite (vide sinon)")
  private List<MembreDansMaTontine> dansMesTontines;

  @Schema(description = "Identité de la personne scannée")
  @Getter
  @Builder
  public static class Personne {

    @Schema(description = "UUID de l'utilisateur")
    private UUID id;

    @Schema(description = "Prénom", example = "Aïcha")
    private String firstName;

    @Schema(description = "Nom de famille", example = "Ndiaye")
    private String lastName;

    @Schema(
        description = "Statut du compte",
        allowableValues = {"ACTIVE", "PRE_ENROLLED"})
    private AccountStatus accountStatus;
  }

  @Schema(description = "Situation de la personne dans une tontine que je gère")
  @Getter
  @Builder
  public static class MembreDansMaTontine {

    @Schema(description = "UUID de la tontine")
    private UUID tontineId;

    @Schema(description = "Nom de la tontine")
    private String tontineNom;

    @Schema(description = "UUID du membership (pour encaisser : POST /cotisations/admin)")
    private UUID membreId;

    @Schema(
        description = "Statut du membership",
        allowableValues = {"ACTIF", "SUSPENDU", "SORTI"})
    private MembreStatut statutMembre;

    @Schema(description = "UUID du tour EN_COURS – null si aucun", nullable = true)
    private UUID cycleEnCoursId;

    @Schema(
        description =
            "true si une cotisation active (EN_ATTENTE ou VALIDE) existe sur le tour en cours ;"
                + " null s'il n'y a pas de tour en cours",
        nullable = true)
    private Boolean aPayeCycleEnCours;

    /** Getter explicite : celui de Lombok serait sérialisé « apayeCycleEnCours ». */
    @JsonProperty("aPayeCycleEnCours")
    public Boolean getAPayeCycleEnCours() {
      return aPayeCycleEnCours;
    }
  }
}
