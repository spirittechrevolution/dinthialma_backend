package com.africa.dinthialma_backend.contribution.entity;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.base.BaseEntity;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Cotisation d'un membre pour un cycle donné.
 *
 * <p>Une seule cotisation par membre par cycle (contrainte {@code uq_cot_cycle_membre}).
 *
 * <p>Flux de validation :
 *
 * <pre>
 *   Membre enregistre paiement → EN_ATTENTE
 *   Admin valide              → VALIDE  (remplit validePar + dateValidation)
 *   Délai cycle dépassé       → EN_RETARD
 * </pre>
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "cotisations", schema = "dinthialma")
public class Cotisation extends BaseEntity {

  /** Tontine concernée (dénormalisation pour faciliter les requêtes). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tontine_id", nullable = false)
  private Tontine tontine;

  /** Membre ayant effectué (ou devant effectuer) cette cotisation. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "membre_id", nullable = false)
  private TontineMembre membre;

  /** Cycle auquel cette cotisation appartient. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id", nullable = false)
  private CycleTontine cycle;

  /** Montant cotisé. */
  @Column(name = "montant", nullable = false, precision = 12, scale = 2)
  private BigDecimal montant;

  /** Méthode de paiement – réf. {@code la_code_list} type {@code METHODE_PAIEMENT}. */
  @Column(name = "methode_paiement", length = 50)
  private String methodePaiement;

  /** Référence de la transaction externe (ex : ID Wave, Orange Money). */
  @Column(name = "reference_transaction", length = 100)
  private String referenceTransaction;

  /** Statut de la cotisation. */
  @Enumerated(EnumType.STRING)
  @Column(name = "statut", nullable = false, length = 30)
  private CotisationStatut statut;

  /** Note libre de l'admin ou du membre. */
  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

  /** Admin ayant validé le paiement – null si pas encore validé. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "valide_par")
  private User validePar;

  /** Date/heure de validation par l'admin. */
  @Column(name = "date_validation")
  private LocalDateTime dateValidation;

  /** Soft delete. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
