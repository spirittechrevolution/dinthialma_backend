package com.africa.dinthialma_backend.auth.repository;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByKeycloakId(String keycloakId);

  Optional<User> findByPhone(String phone);

  Optional<User> findByEmail(String email);

  Optional<User> findByIdAndDeletedAtIsNull(UUID id);

  boolean existsByPhone(String phone);

  boolean existsByEmail(String email);

  /** Tous les utilisateurs non supprimés (pageable) – usage SUPER_ADMIN. */
  Page<User> findByDeletedAtIsNull(Pageable pageable);

  /** Utilisateurs non supprimés avec filtre optionnel sur le statut actif. */
  Page<User> findByActiveTrueAndDeletedAtIsNull(Pageable pageable);

  Page<User> findByActiveFalseAndDeletedAtIsNull(Pageable pageable);

  /** Total utilisateurs non supprimés. */
  long countByDeletedAtIsNull();

  /** Comptes actifs non supprimés. */
  long countByActiveTrueAndDeletedAtIsNull();

  /** Comptes désactivés (active = false) non supprimés. */
  long countByActiveFalseAndDeletedAtIsNull();

  /** Nouveaux inscrits depuis une date (non supprimés). */
  long countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(LocalDateTime since);

  /** Retourne les utilisateurs possédant au moins un des rôles donnés (non supprimés). */
  @Query("SELECT u FROM User u JOIN u.roles r" + " WHERE r.role IN :roles AND u.deletedAt IS NULL")
  List<User> findAdminUsers(@Param("roles") Set<UserRole> roles);
}
