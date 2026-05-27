package com.africa.dinthialma_backend.common.audit;

import com.africa.dinthialma_backend.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Journal d'audit – trace toutes les modifications sur les entités tontine.
 *
 * <p>Les entrées sont <strong>immuables</strong> : pas d'updatedAt, pas de soft delete.
 *
 * <p>Usage recommandé via un service dédié {@code AuditService} :
 *
 * <pre>
 *   auditService.log(caller, "cotisations", cotisation.getId(),
 *       AuditAction.UPDATE, "statut", "EN_ATTENTE", "VALIDE");
 * </pre>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tontine_audit_log", schema = "dinthialma")
public class TontineAuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /** Nom de la table modifiée (ex : {@code "cotisations"}, {@code "tontines"}). */
  @Column(name = "table_name", nullable = false, length = 100)
  private String tableName;

  /** UUID de l'enregistrement modifié. */
  @Column(name = "record_id", nullable = false)
  private UUID recordId;

  /** Type d'action effectuée. */
  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 20)
  private AuditAction action;

  /** Champ modifié – null pour les actions CREATE et DELETE. */
  @Column(name = "champ", length = 100)
  private String champ;

  /** Valeur avant modification – null pour CREATE. */
  @Column(name = "ancienne_val", columnDefinition = "TEXT")
  private String ancienneVal;

  /** Valeur après modification – null pour DELETE. */
  @Column(name = "nouvelle_val", columnDefinition = "TEXT")
  private String nouvelleVal;

  /** Utilisateur ayant effectué l'action. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fait_par")
  private User faitPar;

  /** Date/heure de l'action – non modifiable. */
  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private LocalDateTime createdAt;
}
