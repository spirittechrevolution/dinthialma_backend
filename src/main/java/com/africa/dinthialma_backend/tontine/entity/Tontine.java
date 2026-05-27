package com.africa.dinthialma_backend.tontine.entity;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.base.BaseEntity;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Groupe de tontine – épargne collective rotative.
 *
 * <p>Tout utilisateur peut créer une tontine et en devient automatiquement l'ADMIN (enregistré dans
 * {@link TontineMembre} avec role=ADMIN).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "tontines", schema = "dinthialma")
public class Tontine extends BaseEntity {

  /** Nom du groupe de tontine. */
  @Column(name = "nom", nullable = false, length = 150)
  private String nom;

  /** Description optionnelle de la tontine. */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /** Montant que chaque membre doit cotiser par cycle. */
  @Column(name = "montant", nullable = false, precision = 12, scale = 2)
  private BigDecimal montant;

  /** Fréquence de cotisation – réf. {@code la_code_list} type {@code FREQUENCE_TONTINE}. */
  @Column(name = "frequence", nullable = false, length = 50)
  private String frequence;

  /**
   * Ordre de désignation des bénéficiaires – réf. {@code la_code_list} type {@code
   * ORDRE_BENEFICIAIRE}.
   */
  @Column(name = "ordre_beneficiaire", nullable = false, length = 50)
  private String ordreBeneficiaire;

  /**
   * Mode de gestion des cycles.
   *
   * <ul>
   *   <li>{@link ModeCycle#AUTOMATIQUE} – tous les cycles sont générés à l'activation.
   *   <li>{@link ModeCycle#MANUEL} – l'admin ouvre chaque cycle au moment voulu.
   * </ul>
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "mode_cycle", nullable = false, length = 20)
  private ModeCycle modeCycle;

  /** Date de début du premier cycle. */
  @Column(name = "date_debut", nullable = false)
  private LocalDate dateDebut;

  /** Nombre total de membres attendus (= nombre de cycles à générer). */
  @Column(name = "nombre_membres", nullable = false)
  private int nombreMembres;

  /** Statut courant de la tontine. */
  @Enumerated(EnumType.STRING)
  @Column(name = "statut", nullable = false, length = 30)
  private TontineStatut statut;

  /** Utilisateur ayant créé la tontine – automatiquement ADMIN dans {@link TontineMembre}. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cree_par", nullable = false)
  private User creePar;

  /** Soft delete – null = active, non-null = supprimée. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
