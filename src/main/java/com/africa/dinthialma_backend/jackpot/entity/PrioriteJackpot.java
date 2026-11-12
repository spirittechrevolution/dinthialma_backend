package com.africa.dinthialma_backend.jackpot.entity;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.base.BaseEntity;
import com.africa.dinthialma_backend.jackpot.codeList.OriginePriorite;
import com.africa.dinthialma_backend.jackpot.codeList.StatutPriorite;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Priorité de jackpot d'un membre (tontine ROTATIVE) : une fois ACCEPTEE, le membre passe avant la
 * règle convenue au prochain tour où des gagnants sont désignés.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "priorites_jackpot", schema = "dinthialma")
public class PrioriteJackpot extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tontine_id", nullable = false)
  private Tontine tontine;

  /** Membership bénéficiaire de la priorité. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "membre_id", nullable = false)
  private TontineMembre membre;

  @Enumerated(EnumType.STRING)
  @Column(name = "statut", nullable = false, length = 20)
  private StatutPriorite statut;

  @Enumerated(EnumType.STRING)
  @Column(name = "origine", nullable = false, length = 20)
  private OriginePriorite origine;

  /** Raison du besoin (visible du gestionnaire et du membre concerné uniquement). */
  @Column(name = "motif", length = 500)
  private String motif;

  @Column(name = "motif_refus", length = 500)
  private String motifRefus;

  /** Gestionnaire ayant accepté, attribué, refusé ou annulé la priorité. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "traitee_par")
  private User traiteePar;

  /** Date d'acceptation (ordre de passage entre priorités), de refus ou d'annulation. */
  @Column(name = "date_traitement")
  private LocalDateTime dateTraitement;

  /** Tour où la priorité a été servie. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id")
  private CycleTontine cycle;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
