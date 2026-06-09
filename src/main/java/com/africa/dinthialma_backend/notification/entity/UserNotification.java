package com.africa.dinthialma_backend.notification.entity;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.base.BaseEntity;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Notification in-app persistée pour un utilisateur. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "user_notifications", schema = "dinthialma")
public class UserNotification extends BaseEntity {

  /** Destinataire de la notification. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Catégorie de la notification (icône + couleur côté frontend). */
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 50)
  private NotificationType type;

  /** Titre affiché en gras dans la liste. */
  @Column(name = "title", nullable = false, length = 200)
  private String title;

  /** Sous-titre descriptif. */
  @Column(name = "body", nullable = false, length = 500)
  private String body;

  /** Indique si la notification a été lue. */
  @Column(name = "is_read", nullable = false)
  private boolean isRead = false;

  /** Tontine concernée – permet la navigation depuis la notif. Nullable. */
  @Column(name = "tontine_id")
  private UUID tontineId;

  /** Soft delete. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
