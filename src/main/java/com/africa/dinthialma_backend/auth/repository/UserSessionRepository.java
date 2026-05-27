package com.africa.dinthialma_backend.auth.repository;

import com.africa.dinthialma_backend.auth.codeList.ClientType;
import com.africa.dinthialma_backend.auth.entity.UserSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

  /**
   * Recherche n'importe quelle session (active ou expirée) d'un utilisateur pour un type de client.
   * Utilisé par {@code createOrUpdateSession} pour savoir s'il faut créer ou mettre à jour.
   */
  Optional<UserSession> findByUserIdAndClientType(UUID userId, ClientType clientType);

  /**
   * Recherche la session active d'un utilisateur pour un type de client donné. Filtre sur {@code
   * expiresAt > now} pour ne retourner que les sessions valides.
   */
  Optional<UserSession> findByUserIdAndClientTypeAndExpiresAtAfter(
      UUID userId, ClientType clientType, LocalDateTime now);

  /** Toutes les sessions (actives et expirées) d'un utilisateur, paginées. */
  Page<UserSession> findByUserId(UUID userId, Pageable pageable);

  /** Supprime toutes les sessions d'un utilisateur (déconnexion totale / reset PIN). */
  void deleteAllByUserId(UUID userId);

  /** Supprime la session d'un utilisateur pour un type de client précis. */
  void deleteByUserIdAndClientType(UUID userId, ClientType clientType);

  /** Sessions expirées à purger (job de nettoyage). */
  List<UserSession> findAllByExpiresAtBefore(LocalDateTime threshold);
}
