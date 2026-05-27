package com.africa.dinthialma_backend.auth.entity;

import com.africa.dinthialma_backend.common.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entité utilisateur Dinthialma.
 *
 * <p>L'authentification est déléguée à Keycloak. Cette entité stocke uniquement les informations
 * applicatives ({@code keycloakId} = lien avec Keycloak).
 *
 * <p>Les rôles sont portés par {@link UserRoleAssignment} (table {@code user_roles}) et
 * synchronisés avec le realm Keycloak.
 *
 * <p>La connexion rapide par PIN (6 chiffres) est disponible sur WEB et MOBILE. Le PIN est hashé en
 * Argon2id ({@code pinHash}). Les sessions actives sont tracées dans {@link UserSession} avec un
 * TTL différencié par {@link com.africa.dinthialma_backend.auth.codeList.ClientType}.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "users", schema = "dinthialma")
public class User extends BaseEntity {

  /** Identifiant Keycloak (sub du JWT) – lien entre l'app et l'IAM. */
  @Column(name = "keycloak_id", nullable = false, unique = true)
  private String keycloakId;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  /** Numéro de téléphone – identifiant principal pour la connexion (format international). */
  @Column(name = "phone", nullable = false, unique = true)
  private String phone;

  /** Email – optionnel (certains utilisateurs n'ont pas d'email). */
  @Column(name = "email")
  private String email;

  /** URL de l'avatar (optionnel). */
  @Column(name = "avatar_url")
  private String avatarUrl;

  /** Indique si le compte est actif (non suspendu). */
  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  // ─── PIN connexion rapide (mobile) ────────────────────────────────

  /**
   * Hash Argon2id du code PIN à 6 chiffres. {@code null} = PIN non encore configuré par
   * l'utilisateur.
   */
  @Column(name = "pin_hash")
  private String pinHash;

  /**
   * Nombre de tentatives PIN échouées consécutives. Remis à 0 après une tentative réussie ou un
   * reset via OTP.
   */
  @Column(name = "pin_attempts", nullable = false)
  private int pinAttempts = 0;

  /**
   * Date/heure jusqu'à laquelle le PIN est verrouillé. {@code null} = déverrouillé. Fixé après
   * {@code MAX_PIN_ATTEMPTS} échecs consécutifs.
   */
  @Column(name = "pin_locked_until")
  private LocalDateTime pinLockedUntil;

  // ─── Relations ────────────────────────────────────────────────────

  /** Soft delete – null = actif, non-null = compte désactivé. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /**
   * Rôles de l'utilisateur sur la plateforme. Synchronisés avec le realm Keycloak via {@link
   * UserRoleAssignment#isSyncedToKeycloak()}.
   */
  @OneToMany(
      mappedBy = "user",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private Set<UserRoleAssignment> roles = new HashSet<>();

  /**
   * Sessions actives de l'utilisateur (WEB et MOBILE). PIN valide → session → refresh Keycloak
   * token → JWT retourné au client.
   */
  @OneToMany(
      mappedBy = "user",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private Set<UserSession> sessions = new HashSet<>();
}
