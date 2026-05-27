package com.africa.dinthialma_backend.auth.repository;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.UserRoleAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {

  /** Trouve l'attribution d'un rôle précis pour un utilisateur. */
  Optional<UserRoleAssignment> findByUserIdAndRole(UUID userId, UserRole role);

  /** Vérifie si un utilisateur possède déjà un rôle. */
  boolean existsByUserIdAndRole(UUID userId, UserRole role);

  /** Récupère tous les rôles d'un utilisateur. */
  List<UserRoleAssignment> findAllByUserId(UUID userId);

  /**
   * Récupère tous les rôles non encore synchronisés avec Keycloak. Utilisé par le job de
   * re-synchronisation en cas de Keycloak indisponible.
   */
  List<UserRoleAssignment> findAllBySyncedToKeycloakFalse();
}
