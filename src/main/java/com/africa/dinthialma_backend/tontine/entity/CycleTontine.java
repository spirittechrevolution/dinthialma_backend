package com.africa.dinthialma_backend.tontine.entity;

import com.africa.dinthialma_backend.common.base.BaseEntity;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Cycle (période) d'une tontine.
 *
 * <p>Chaque cycle représente une période de cotisation. À la fin du cycle, le bénéficiaire désigné
 * reçoit le jackpot (somme de toutes les cotisations validées).
 *
 * <p>Génération :
 *
 * <ul>
 *   <li>Mode {@code AUTOMATIQUE} : tous les cycles sont créés à l'activation de la tontine.
 *   <li>Mode {@code MANUEL} : l'admin ouvre chaque cycle individuellement.
 * </ul>
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "cycles_tontine", schema = "dinthialma")
public class CycleTontine extends BaseEntity {

  /** Tontine à laquelle appartient ce cycle. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tontine_id", nullable = false)
  private Tontine tontine;

  /** Numéro séquentiel du cycle (1 = premier cycle). */
  @Column(name = "numero_cycle", nullable = false)
  private int numeroCycle;

  /**
   * Membre bénéficiaire du jackpot pour ce cycle. Peut être null si le bénéficiaire n'est pas
   * encore désigné (mode ALEATOIRE ou MANUEL en attente de décision).
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "beneficiaire_id")
  private TontineMembre beneficiaire;

  /** Date de début du cycle. */
  @Column(name = "date_debut", nullable = false)
  private LocalDate dateDebut;

  /** Date de fin du cycle. */
  @Column(name = "date_fin", nullable = false)
  private LocalDate dateFin;

  /** Montant total collecté et remis au bénéficiaire. */
  @Column(name = "montant_jackpot", precision = 12, scale = 2)
  private BigDecimal montantJackpot;

  /** Statut courant du cycle. */
  @Enumerated(EnumType.STRING)
  @Column(name = "statut", nullable = false, length = 30)
  private CycleStatut statut;

  /** Date à laquelle le jackpot a été physiquement remis au bénéficiaire. */
  @Column(name = "date_remise")
  private LocalDate dateRemise;

  /** Soft delete. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
