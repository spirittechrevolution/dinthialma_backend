package com.africa.dinthialma_backend.member.entity;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.base.BaseEntity;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Cotisant d'une tontine.
 *
 * <p>Cette table contient <strong>uniquement les membres qui cotisent</strong>. L'administrateur de
 * la tontine est identifié par {@link Tontine#getCreePar()} et peut figurer ici s'il choisit
 * également de cotiser.
 *
 * <p>Contraintes gérées en base :
 *
 * <ul>
 *   <li>{@code UNIQUE(tontine_id, user_id)} – un utilisateur ne peut cotiser qu'une fois par
 *       tontine
 *   <li>{@code UNIQUE(tontine_id, ordre_jackpot)} – deux cotisants ne peuvent partager le même rang
 * </ul>
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "tontine_membres", schema = "dinthialma")
public class TontineMembre extends BaseEntity {

  /** Tontine à laquelle appartient ce cotisant. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tontine_id", nullable = false)
  private Tontine tontine;

  /** Utilisateur cotisant. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * Position dans la rotation des jackpots (1 = premier bénéficiaire). Null jusqu'à l'activation de
   * la tontine.
   */
  @Column(name = "ordre_jackpot")
  private Integer ordreJackpot;

  /** Statut du cotisant dans la tontine. */
  @Enumerated(EnumType.STRING)
  @Column(name = "statut", nullable = false, length = 30)
  private MembreStatut statut;

  /** Date d'adhésion à la tontine. */
  @Column(name = "date_adhesion", nullable = false)
  private LocalDate dateAdhesion;

  /** Indique si ce membre a déjà reçu un jackpot dans cette tontine. */
  @Column(name = "a_recu_jackpot", nullable = false)
  private boolean aRecuJackpot = false;

  /** Date à laquelle le jackpot a été remis à ce membre. */
  @Column(name = "date_jackpot")
  private LocalDateTime dateJackpot;

  /** Soft delete. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
