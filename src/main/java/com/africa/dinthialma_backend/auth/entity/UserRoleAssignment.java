package com.africa.dinthialma_backend.auth.entity;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Rôle attribué à un utilisateur.
 *
 * <p>Un utilisateur peut cumuler plusieurs rôles (ex : MEMBER + ADMIN). Chaque entrée est
 * synchronisée avec le realm Keycloak via {@code syncedToKeycloak}.
 *
 * <p>Règle d'attribution :
 *
 * <ul>
 *   <li>Inscription → {@link UserRole#USER} automatiquement
 *   <li>Créer une tontine → {@link UserRole#ADMIN} si pas déjà présent
 *   <li>Rejoindre (cotisant) → {@link UserRole#MEMBER} si pas déjà présent
 * </ul>
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "user_roles", schema = "dinthialma")
public class UserRoleAssignment extends BaseEntity {

  /** Utilisateur auquel ce rôle appartient. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Rôle Keycloak attribué. */
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 50)
  private UserRole role;

  /**
   * Indique si le rôle a été propagé dans le realm Keycloak. {@code false} = en attente de
   * synchronisation (Keycloak down, retry nécessaire).
   */
  @Column(name = "synced_to_keycloak", nullable = false)
  private boolean syncedToKeycloak;
}
