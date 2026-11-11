package com.africa.dinthialma_backend.adhesion.entity;

import com.africa.dinthialma_backend.adhesion.codeList.StatutDemande;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.base.BaseEntity;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
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

/** Demande d'adhésion à une tontine, envoyée via son code d'invitation (B17). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "demandes_adhesion", schema = "dinthialma")
public class DemandeAdhesion extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tontine_id", nullable = false)
  private Tontine tontine;

  /** Demandeur. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "statut", nullable = false, length = 20)
  private StatutDemande statut;

  /** Message libre du demandeur. */
  @Column(name = "message", length = 500)
  private String message;

  @Column(name = "motif_refus", length = 500)
  private String motifRefus;

  /** Gestionnaire ayant accepté ou refusé la demande. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "traitee_par")
  private User traiteePar;

  @Column(name = "date_traitement")
  private LocalDateTime dateTraitement;

  /** Membership créé à l'acceptation. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "membre_id")
  private TontineMembre membre;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
