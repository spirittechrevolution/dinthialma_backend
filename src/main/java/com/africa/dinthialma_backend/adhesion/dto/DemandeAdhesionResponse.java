package com.africa.dinthialma_backend.adhesion.dto;

import com.africa.dinthialma_backend.adhesion.codeList.StatutDemande;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Demande d'adhésion à une tontine")
@Getter
@Builder
public class DemandeAdhesionResponse {

  @Schema(description = "UUID de la demande")
  private UUID id;

  @Schema(description = "UUID de la tontine")
  private UUID tontineId;

  @Schema(description = "Nom de la tontine")
  private String tontineNom;

  @Schema(
      description = "Statut",
      allowableValues = {"EN_ATTENTE", "ACCEPTEE", "REFUSEE"})
  private StatutDemande statut;

  @Schema(description = "UUID du demandeur")
  private UUID userId;

  @Schema(description = "Prénom du demandeur")
  private String firstName;

  @Schema(description = "Nom du demandeur")
  private String lastName;

  @Schema(
      description = "Téléphone du demandeur (visible du gestionnaire uniquement)",
      nullable = true)
  private String phone;

  @Schema(description = "Message du demandeur", nullable = true)
  private String message;

  @Schema(description = "Motif du refus", nullable = true)
  private String motifRefus;

  @Schema(description = "Membership créé à l'acceptation", nullable = true)
  private UUID membreId;

  @Schema(description = "Date de la demande")
  private LocalDateTime createdAt;

  @Schema(description = "Date d'acceptation ou de refus", nullable = true)
  private LocalDateTime dateTraitement;
}
