package com.africa.dinthialma_backend.tontine.entity;

import com.africa.dinthialma_backend.common.base.BaseEntity;
import com.africa.dinthialma_backend.tontine.codeList.CommissionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Commission définie par l'admin pour une tontine.
 *
 * <p>Un admin peut définir 0, 1 ou plusieurs commissions par tontine. La contrainte {@code
 * UNIQUE(tontine_id, type)} garantit qu'il n'existe qu'une seule règle par type de commission par
 * tontine.
 *
 * <p>Exemples :
 *
 * <ul>
 *   <li>{@link CommissionType#POURCENTAGE_JACKPOT} + {@code valeur = 5.00} → 5% de chaque jackpot
 *   <li>{@link CommissionType#FRAIS_FIXES_PAR_CYCLE} + {@code valeur = 1000.00} → 1 000 FCFA/cycle
 *   <li>{@link CommissionType#FRAIS_ADHESION} + {@code valeur = 500.00} → 500 FCFA à l'entrée
 * </ul>
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "tontine_commissions", schema = "dinthialma")
public class TontineCommission extends BaseEntity {

  /** Tontine à laquelle s'applique cette commission. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tontine_id", nullable = false)
  private Tontine tontine;

  /** Type de commission. */
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 50)
  private CommissionType type;

  /**
   * Valeur de la commission.
   *
   * <ul>
   *   <li>Pour {@link CommissionType#POURCENTAGE_JACKPOT} : taux en % (ex : {@code 5.00})
   *   <li>Pour les autres types : montant en FCFA (ex : {@code 1000.00})
   * </ul>
   */
  @Column(name = "valeur", nullable = false, precision = 10, scale = 2)
  private BigDecimal valeur;

  /** Note optionnelle affichée aux membres (ex : "Frais de gestion du groupe"). */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /** Soft delete. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
