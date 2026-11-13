package com.africa.dinthialma_backend.tontine.entity;

import com.africa.dinthialma_backend.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Échéance de cotisation d'un tour (B26) : date-heure limite à laquelle chaque membre doit avoir
 * déclaré son paiement. Un tour a aujourd'hui une seule échéance ; B32 permettra plusieurs
 * cotisations par tour (une échéance chacune).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "echeances_cycle", schema = "dinthialma")
public class EcheanceCycle extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id", nullable = false)
  private CycleTontine cycle;

  /** Rang de l'échéance dans le tour (1 à N). */
  @Column(name = "numero", nullable = false)
  private int numero;

  /** Premier jour de la période de cotisation de cette échéance. */
  @Column(name = "date_debut", nullable = false)
  private LocalDate dateDebut;

  /** Limite de paiement (heure de Dakar). */
  @Column(name = "echeance_at", nullable = false)
  private LocalDateTime echeanceAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
