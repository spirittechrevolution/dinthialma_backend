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

/** Gagnant du jackpot pour un cycle donné. Un cycle peut avoir N gagnants. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "cycle_gagnants", schema = "dinthialma")
public class CycleGagnant extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id", nullable = false)
  private CycleTontine cycle;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "membre_id", nullable = false)
  private TontineMembre membre;

  /** Montant effectivement reçu = montantNet du cycle / nombreGagnants. */
  @Column(name = "montant_recu", precision = 12, scale = 2)
  private BigDecimal montantRecu;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
