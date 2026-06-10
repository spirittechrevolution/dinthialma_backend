package com.africa.dinthialma_backend.notification.service.impl;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.dto.NotificationResponse;
import com.africa.dinthialma_backend.notification.entity.UserNotification;
import com.africa.dinthialma_backend.notification.repository.NotificationRepository;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implémentation du service de notifications in-app. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;

  @Override
  public void notify(
      UUID userId, NotificationType type, String title, String body, UUID tontineId) {
    try {
      User user =
          userRepository
              .findById(userId)
              .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
      UserNotification notif =
          UserNotification.builder()
              .user(user)
              .type(type)
              .title(title)
              .body(body)
              .isRead(false)
              .tontineId(tontineId)
              .build();
      notificationRepository.save(notif);
    } catch (Exception e) {
      log.warn("Notification non créée – userId={} type={} : {}", userId, type, e.getMessage());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Page<NotificationResponse> getMyNotifications(String keycloakId, Pageable pageable)
      throws CustomException {
    User user = findUser(keycloakId);
    return notificationRepository
        .findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId(), pageable)
        .map(NotificationResponse::from);
  }

  @Override
  @Transactional(readOnly = true)
  public long countUnread(String keycloakId) throws CustomException {
    User user = findUser(keycloakId);
    return notificationRepository.countByUser_IdAndIsReadFalseAndDeletedAtIsNull(user.getId());
  }

  @Override
  public void markAsRead(String keycloakId, UUID notificationId) throws CustomException {
    User user = findUser(keycloakId);
    UserNotification notif =
        notificationRepository
            .findByIdAndUser_IdAndDeletedAtIsNull(notificationId, user.getId())
            .orElseThrow(() -> new NotFoundException("Notification introuvable"));
    notif.setRead(true);
    notificationRepository.save(notif);
  }

  @Override
  public void markAllAsRead(String keycloakId) throws CustomException {
    User user = findUser(keycloakId);
    notificationRepository.markAllReadByUserId(user.getId());
  }

  private User findUser(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }
}
