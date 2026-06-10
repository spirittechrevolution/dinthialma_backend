package com.africa.dinthialma_backend.notification.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.dto.NotificationResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Service de gestion des notifications in-app. */
public interface NotificationService {

  /**
   * Crée et persiste une notification pour un utilisateur. Non-throwable : les exceptions sont
   * loggées en interne pour ne pas impacter les transactions métier appelantes.
   */
  void notify(UUID userId, NotificationType type, String title, String body, UUID tontineId);

  /** Liste paginée des notifications de l'utilisateur connecté (plus récentes en premier). */
  Page<NotificationResponse> getMyNotifications(String keycloakId, Pageable pageable)
      throws CustomException;

  /** Nombre de notifications non lues (badge cloche). */
  long countUnread(String keycloakId) throws CustomException;

  /** Marque une notification comme lue. */
  void markAsRead(String keycloakId, UUID notificationId) throws CustomException;

  /** Marque toutes les notifications non lues comme lues. */
  void markAllAsRead(String keycloakId) throws CustomException;
}
