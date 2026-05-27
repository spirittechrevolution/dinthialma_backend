package com.africa.dinthialma_backend.auth.repository;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByKeycloakId(String keycloakId);

  Optional<User> findByPhone(String phone);

  Optional<User> findByEmail(String email);

  boolean existsByPhone(String phone);

  boolean existsByEmail(String email);

  /** Retourne les utilisateurs possédant au moins un des rôles donnés (non supprimés). */
  @Query(
      "SELECT DISTINCT u FROM User u JOIN u.roles r"
          + " WHERE r.role IN :roles AND u.deletedAt IS NULL")
  List<User> findAdminUsers(@Param("roles") Set<UserRole> roles);
}
