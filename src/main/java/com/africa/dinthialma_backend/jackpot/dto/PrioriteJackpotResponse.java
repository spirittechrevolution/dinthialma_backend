package com.africa.dinthialma_backend.jackpot.dto;

import com.africa.dinthialma_backend.jackpot.codeList.OriginePriorite;
import com.africa.dinthialma_backend.jackpot.codeList.StatutPriorite;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Priorité de jackpot d'un membre")
@Getter
@Builder(toBuilder = true)
public class PrioriteJackpotResponse {

  @Schema(description = "UUID de la priorité")
  private UUID id;

  @Schema(description = "UUID de la tontine")
  private UUID tontineId;

  @Schema(description = "UUID du membership bénéficiaire")
  private UUID membreId;

  @Schema(description = "UUID de l'utilisateur bénéficiaire")
  private UUID userId;

  @Schema(description = "Prénom du membre")
  private String firstName;

  @Schema(description = "Nom du membre")
  private String lastName;

  @Schema(
      description = "Statut",
      allowableValues = {"EN_ATTENTE", "ACCEPTEE", "REFUSEE", "ANNULEE", "SERVIE"})
  private StatutPriorite statut;

  @Schema(
      description = "Origine",
      allowableValues = {"DEMANDE_MEMBRE", "GESTIONNAIRE"})
  private OriginePriorite origine;

  @Schema(
      description =
          "Raison du besoin : visible du gestionnaire et du membre concerné uniquement (null pour"
              + " les autres membres)",
      nullable = true)
  private String motif;

  @Schema(description = "Motif du refus ou de l'annulation", nullable = true)
  private String motifRefus;

  @Schema(description = "Tour où la priorité a été servie", nullable = true)
  private UUID cycleId;

  @Schema(description = "Date de la demande ou de l'attribution")
  private LocalDateTime createdAt;

  @Schema(description = "Date d'acceptation, de refus ou d'annulation", nullable = true)
  private LocalDateTime dateTraitement;
}
