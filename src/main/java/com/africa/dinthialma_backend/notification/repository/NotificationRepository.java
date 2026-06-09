package com.africa.dinthialma_backend.notification.repository;

import com.africa.dinthialma_backend.notification.entity.UserNotification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Accès base pour les notifications in-app. */
@Repository
public interface NotificationRepository extends JpaRepository<UserNotification, UUID> {

  /** Liste paginée des notifications non supprimées d'un utilisateur, plus récentes en premier. */
  Page<UserNotification> findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID userId, Pageable pageable);

  /** Nombre de notifications non lues d'un utilisateur. */
  long countByUser_IdAndIsReadFalseAndDeletedAtIsNull(UUID userId);

  /** Toutes les notifications non lues d'un utilisateur. */
  List<UserNotification> findByUser_IdAndIsReadFalseAndDeletedAtIsNull(UUID userId);

  /** Récupère une notification appartenant à un utilisateur donné. */
  Optional<UserNotification> findByIdAndUser_IdAndDeletedAtIsNull(UUID id, UUID userId);

  /** Marque toutes les notifications non lues d'un utilisateur comme lues. */
  @Modifying
  @Query(
      "UPDATE UserNotification n SET n.isRead = true"
          + " WHERE n.user.id = :userId AND n.isRead = false AND n.deletedAt IS NULL")
  int markAllReadByUserId(@Param("userId") UUID userId);
}
