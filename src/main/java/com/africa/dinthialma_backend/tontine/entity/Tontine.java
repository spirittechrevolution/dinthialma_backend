package com.africa.dinthialma_backend.tontine.entity;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.base.BaseEntity;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.tontine.calendrier.Rythme;
import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.codeList.UniteRythme;
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
import java.time.LocalTime;
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
   * ORDRE_BENEFICIAIRE}. Null pour les tontines EVENEMENTIELLE.
   */
  @Column(name = "ordre_beneficiaire", length = 50)
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

  /** Nombre de membres qui reçoivent le jackpot à chaque cycle (défaut 1). */
  @Column(name = "nombre_gagnants", nullable = false)
  private int nombreGagnants = 1;

  /**
   * Type de tontine.
   *
   * <ul>
   *   <li>{@link TontineType#ROTATIVE} – jackpot tournant entre membres.
   *   <li>{@link TontineType#EVENEMENTIELLE} – épargne collective vers un événement (Tabaski,
   *       Korité…).
   * </ul>
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "tontine_type", nullable = false, length = 30)
  private TontineType tontineType = TontineType.ROTATIVE;

  /** Date cible de l'événement (EVENEMENTIELLE uniquement). */
  @Column(name = "date_echeance")
  private LocalDate dateEcheance;

  /** Nom de l'événement cible (ex : "Tabaski 2026"). Optionnel. */
  @Column(name = "nom_evenement", length = 200)
  private String nomEvenement;

  /**
   * Si {@code true}, chaque membre cotise librement le montant qu'il souhaite (EVENEMENTIELLE).
   * {@code false} = montant fixe imposé par l'admin.
   */
  @Column(name = "montant_libre", nullable = false)
  private boolean montantLibre = false;

  /**
   * Plancher de cotisation en mode libre (EVENEMENTIELLE + montantLibre). Null = aucun minimum
   * imposé.
   */
  @Column(name = "montant_minimum", precision = 12, scale = 2)
  private BigDecimal montantMinimum;

  // ─── Rythme et échéance (B26) ───────────────────────────────────────────

  /** « Tous les N … » : intervalle du rythme de cotisation. */
  @Column(name = "rythme_intervalle", nullable = false)
  private int rythmeIntervalle;

  /** Unité du rythme : JOUR, SEMAINE ou MOIS. */
  @Enumerated(EnumType.STRING)
  @Column(name = "rythme_unite", nullable = false, length = 10)
  private UniteRythme rythmeUnite;

  /** SEMAINE : LUNDI…DIMANCHE ; MOIS : « 1 »…« 31 » ou DERNIER ; null = aucun jour imposé. */
  @Column(name = "jour_echeance", length = 10)
  private String jourEcheance;

  /** Heure limite de paiement le jour de l'échéance (heure de Dakar). */
  @Column(name = "heure_echeance", nullable = false)
  private LocalTime heureEcheance;

  /** Rappel la veille de l'échéance, à 18h00. */
  @Column(name = "rappel_veille", nullable = false)
  private boolean rappelVeille;

  /** Rappel N heures avant l'échéance (0 = aucun, 1 à 24). */
  @Column(name = "rappel_heures_avant", nullable = false)
  private int rappelHeuresAvant;

  /** Code d'invitation partageable (B16) – null = aucune invitation active. */
  @Column(name = "code_invitation", length = 20)
  private String codeInvitation;

  /** Soft delete – null = active, non-null = supprimée. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /**
   * Montant attendu d'un membre pour un cycle : le montant fixe, ou le plancher en mode libre.
   *
   * @return null si le montant est libre sans plancher (aucun attendu calculable)
   */
  public BigDecimal getMontantAttenduParMembre() {
    return montantAttenduParMembre(montantLibre, montant, montantMinimum);
  }

  /**
   * Rythme de cotisation (B26). Sans rythme renseigné (objet non enregistré), repli sur la
   * fréquence historique, puis sur un rythme mensuel.
   */
  public Rythme getRythme() {
    if (rythmeUnite == null) {
      return Rythme.depuisFrequence(frequence)
          .orElse(new Rythme(1, UniteRythme.MOIS, null, Rythme.HEURE_PAR_DEFAUT));
    }
    return new Rythme(
        rythmeIntervalle,
        rythmeUnite,
        jourEcheance,
        heureEcheance != null ? heureEcheance : Rythme.HEURE_PAR_DEFAUT);
  }

  /**
   * Applique un rythme : ses champs et la fréquence équivalente (code historique ou PERSONNALISEE).
   */
  public void appliquerRythme(Rythme rythme) {
    this.rythmeIntervalle = rythme.intervalle();
    this.rythmeUnite = rythme.unite();
    this.jourEcheance = rythme.jourEcheance();
    this.heureEcheance = rythme.heureEcheance();
    this.frequence = rythme.frequence();
  }

  /** Variante statique utilisable sur des colonnes issues d'une projection JPQL. */
  public static BigDecimal montantAttenduParMembre(
      boolean montantLibre, BigDecimal montant, BigDecimal montantMinimum) {
    return montantLibre ? montantMinimum : montant;
  }
}
