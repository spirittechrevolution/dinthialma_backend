package com.africa.dinthialma_backend.notification.dto;

import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.entity.UserNotification;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/** Réponse API pour une notification in-app. */
@Getter
@Builder
public class NotificationResponse {

  private UUID id;
  private NotificationType type;
  private String title;
  private String body;
  private boolean isRead;
  private UUID tontineId;
  private LocalDateTime createdAt;

  public static NotificationResponse from(UserNotification n) {
    return NotificationResponse.builder()
        .id(n.getId())
        .type(n.getType())
        .title(n.getTitle())
        .body(n.getBody())
        .isRead(n.isRead())
        .tontineId(n.getTontineId())
        .createdAt(n.getCreatedAt())
        .build();
  }
}
