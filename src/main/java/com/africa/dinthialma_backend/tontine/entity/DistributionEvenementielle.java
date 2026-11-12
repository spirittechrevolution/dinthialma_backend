package com.africa.dinthialma_backend.tontine.entity;

import com.africa.dinthialma_backend.common.base.BaseEntity;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Part d'un membre dans la distribution finale d'une tontine ÉVÉNEMENTIELLE (V034) : enregistrée à
 * la clôture du dernier sous-cycle. {@code montantNet = montantCotise - montantCommission}.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "distributions_evenementielles", schema = "dinthialma")
public class DistributionEvenementielle extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tontine_id", nullable = false)
  private Tontine tontine;

  /** Dernier sous-cycle, dont la clôture a déclenché la distribution. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id", nullable = false)
  private CycleTontine cycle;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "membre_id", nullable = false)
  private TontineMembre membre;

  /** Somme des cotisations VALIDÉES du membre, tous sous-cycles confondus. */
  @Column(name = "montant_cotise", nullable = false, precision = 12, scale = 2)
  private BigDecimal montantCotise;

  /** Part de commission prélevée sur sa mise. */
  @Column(name = "montant_commission", nullable = false, precision = 12, scale = 2)
  private BigDecimal montantCommission;

  /** Montant effectivement remis au membre. */
  @Column(name = "montant_net", nullable = false, precision = 12, scale = 2)
  private BigDecimal montantNet;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
