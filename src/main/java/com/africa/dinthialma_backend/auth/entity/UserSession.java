package com.africa.dinthialma_backend.auth.entity;

import com.africa.dinthialma_backend.auth.codeList.ClientType;
import com.africa.dinthialma_backend.common.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Session active d'un utilisateur — valable sur WEB et MOBILE.
 *
 * <p>Créée lors de la première connexion standard (phone/email + mot de passe). Utilisée ensuite
 * pour la connexion rapide par PIN (6 chiffres), quelle que soit la plateforme :
 *
 * <ol>
 *   <li>Validation du PIN (hash Argon2id dans {@link User#getPinHash()})
 *   <li>Récupération de la session active pour ce client
 *   <li>Appel Keycloak avec le {@code refresh_token} → nouveau JWT
 *   <li>Mise à jour de {@link #lastUsedAt} et renouvellement de {@link #expiresAt}
 * </ol>
 *
 * <p>TTL par plateforme (défini dans le service) :
 *
 * <ul>
 *   <li>{@link ClientType#WEB} → 24 heures
 *   <li>{@link ClientType#MOBILE} → 30 jours
 * </ul>
 *
 * <p>Révocation (hard delete) lors de :
 *
 * <ul>
 *   <li>Déconnexion explicite
 *   <li>Réinitialisation du PIN via OTP
 *   <li>Désactivation du compte
 *   <li>Expiration ({@link #expiresAt})
 * </ul>
 *
 * <p>Le {@code refresh_token} Keycloak n'est <strong>jamais stocké en clair</strong> : seul son
 * hash SHA-256 est conservé ({@link #refreshTokenHash}).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "user_sessions", schema = "dinthialma")
public class UserSession extends BaseEntity {

  /** Utilisateur propriétaire de cette session. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * Type de client – détermine le TTL et les politiques de sécurité.
   *
   * @see ClientType
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "client_type", nullable = false, length = 20)
  private ClientType clientType;

  /**
   * Hash SHA-256 du Keycloak {@code refresh_token}. Permet de récupérer et rafraîchir le token sans
   * exposer la valeur réelle.
   */
  @Column(name = "refresh_token_hash", nullable = false)
  private String refreshTokenHash;

  /**
   * Informations sur le client.
   *
   * <ul>
   *   <li>WEB → user-agent du navigateur (extrait du header HTTP)
   *   <li>MOBILE → device_id fourni par l'application
   * </ul>
   *
   * Optionnel en MVP — utilisé pour "mes appareils connectés" (v2).
   */
  @Column(name = "device_info")
  private String deviceInfo;

  /**
   * Dernière utilisation de cette session via le PIN. Mis à jour à chaque connexion PIN réussie.
   */
  @Column(name = "last_used_at", nullable = false)
  private LocalDateTime lastUsedAt;

  /**
   * Date d'expiration de la session.
   *
   * <ul>
   *   <li>WEB : {@code NOW() + 24 heures}
   *   <li>MOBILE : {@code NOW() + 30 jours}
   * </ul>
   *
   * Renouvelée à chaque connexion PIN réussie.
   */
  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;
}
