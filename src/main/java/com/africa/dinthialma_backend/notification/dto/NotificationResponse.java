package com.africa.dinthialma_backend.notification.dto;

import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/** Réponse API pour une notification in-app. */
@Schema(description = "Notification in-app de l'utilisateur connecté")
@Getter
@Builder
public class NotificationResponse {

  @Schema(description = "UUID de la notification")
  private UUID id;

  @Schema(description = "Type d'événement (13 valeurs, cf. NotificationType)")
  private NotificationType type;

  @Schema(description = "Titre court", example = "Cotisation validée")
  private String title;

  @Schema(description = "Texte de la notification")
  private String body;

  /** Sérialisé {@code read} (nom figé : le front lit {@code read}). */
  @Schema(description = "true si la notification a été lue", example = "false")
  @JsonProperty("read")
  private boolean isRead;

  @Schema(description = "Tontine concernée – null si aucune", nullable = true)
  private UUID tontineId;

  @Schema(description = "Cycle (tour) concerné – null si aucun", nullable = true)
  private UUID cycleId;

  @Schema(description = "Cotisation concernée (reçu) – null si aucune", nullable = true)
  private UUID cotisationId;

  @Schema(
      description = "Membership concerné (tontine_membres.id, fiche membre) – null si aucun",
      nullable = true)
  private UUID membreId;

  @Schema(description = "Date de création")
  private LocalDateTime createdAt;
}
