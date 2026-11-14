package com.africa.dinthialma_backend.contribution.repository;

import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Accès base pour les cotisations. */
@Repository
public interface CotisationRepository
    extends JpaRepository<Cotisation, UUID>, JpaSpecificationExecutor<Cotisation> {

  /**
   * Liste filtrée (cf. {@link CotisationSpecifications}) avec chargement anticipé de la tontine, du
   * cycle et du cotisant pour éviter les requêtes N+1 lors du mapping en DTO.
   */
  @Override
  @EntityGraph(attributePaths = {"tontine", "cycle", "membre", "membre.user"})
  Page<Cotisation> findAll(Specification<Cotisation> spec, Pageable pageable);

  /** Cotisations d'un cycle. */
  List<Cotisation> findByCycle_IdAndDeletedAtIsNull(UUID cycleId);

  /** Cotisations d'un cycle paginées. */
  Page<Cotisation> findByCycle_IdAndDeletedAtIsNull(UUID cycleId, Pageable pageable);

  /** Cotisations d'une tontine (tous cycles confondus). */
  List<Cotisation> findByTontine_IdAndDeletedAtIsNull(UUID tontineId);

  /** Cotisations d'une tontine paginées. */
  Page<Cotisation> findByTontine_IdAndDeletedAtIsNull(UUID tontineId, Pageable pageable);

  /** Cotisations d'un membre (tous cycles confondus). */
  List<Cotisation> findByMembre_IdAndDeletedAtIsNull(UUID membreId);

  /** Cotisations d'un membre pour une tontine donnée. */
  List<Cotisation> findByTontine_IdAndMembre_IdAndDeletedAtIsNull(UUID tontineId, UUID membreId);

  /** Cotisations d'un membre pour une tontine donnée paginées. */
  Page<Cotisation> findByTontine_IdAndMembre_IdAndDeletedAtIsNull(
      UUID tontineId, UUID membreId, Pageable pageable);

  /** Cotisations d'un membre pour un cycle donné paginées. */
  Page<Cotisation> findByCycle_IdAndMembre_IdAndDeletedAtIsNull(
      UUID cycleId, UUID membreId, Pageable pageable);

  /** Cotisation par id sans soft-delete. */
  Optional<Cotisation> findByIdAndDeletedAtIsNull(UUID id);

  /**
   * Vrai si le membre a au moins une cotisation <b>active</b> sur le cycle (tout statut sauf
   * REFUSE), toutes échéances confondues.
   */
  default boolean existsActiveByCycleAndMembre(UUID cycleId, UUID membreId) {
    return existsByCycle_IdAndMembre_IdAndStatutNotAndDeletedAtIsNull(
        cycleId, membreId, CotisationStatut.REFUSE);
  }

  boolean existsByCycle_IdAndMembre_IdAndStatutNotAndDeletedAtIsNull(
      UUID cycleId, UUID membreId, CotisationStatut statut);

  /**
   * Vrai si le membre a une cotisation <b>active</b> (tout statut sauf REFUSE) sur l'échéance :
   * pendant applicatif de l'index unique partiel {@code uq_cot_echeance_membre_actif} (V042, B32).
   */
  default boolean existsActiveByEcheanceAndMembre(UUID echeanceId, UUID membreId) {
    return existsByEcheance_IdAndMembre_IdAndStatutNotAndDeletedAtIsNull(
        echeanceId, membreId, CotisationStatut.REFUSE);
  }

  boolean existsByEcheance_IdAndMembre_IdAndStatutNotAndDeletedAtIsNull(
      UUID echeanceId, UUID membreId, CotisationStatut statut);

  /** Cotisations d'une échéance (rappels et cotisations manquantes, B32). */
  List<Cotisation> findByEcheance_IdAndDeletedAtIsNull(UUID echeanceId);

  /** Cotisations par statut pour une tontine. */
  List<Cotisation> findByTontine_IdAndStatutAndDeletedAtIsNull(
      UUID tontineId, CotisationStatut statut);

  /** Cotisations par statut pour une tontine paginées. */
  Page<Cotisation> findByTontine_IdAndStatutAndDeletedAtIsNull(
      UUID tontineId, CotisationStatut statut, Pageable pageable);

  /** Cotisations d'un cycle restreintes à un ensemble de membres (récap paginé). */
  List<Cotisation> findByCycle_IdAndMembre_IdInAndDeletedAtIsNull(
      UUID cycleId, Collection<UUID> membreIds);

  // ─── Listes plateforme (SUPER_ADMIN) ──────────────────────────────────────

  /** Toutes les cotisations non supprimées de la plateforme paginées. */
  Page<Cotisation> findByDeletedAtIsNull(Pageable pageable);

  /** Toutes les cotisations non supprimées d'un statut donné paginées. */
  Page<Cotisation> findByStatutAndDeletedAtIsNull(CotisationStatut statut, Pageable pageable);

  /** Cotisations par statut pour un cycle. */
  List<Cotisation> findByCycle_IdAndStatutAndDeletedAtIsNull(UUID cycleId, CotisationStatut statut);

  // ─── Stats dashboard ──────────────────────────────────────────────────────

  /** Nombre de cotisations par statut (toute la plateforme). */
  long countByStatutAndDeletedAtIsNull(CotisationStatut statut);

  /** Nombre de cotisations d'une tontine par statut. */
  long countByTontine_IdAndStatutAndDeletedAtIsNull(UUID tontineId, CotisationStatut statut);

  /** Montant total des cotisations VALIDÉES depuis une date (plateforme). */
  @Query(
      "SELECT COALESCE(SUM(c.montant), 0) FROM Cotisation c"
          + " WHERE c.statut = 'VALIDE'"
          + "   AND c.deletedAt IS NULL"
          + "   AND c.createdAt >= :since")
  BigDecimal sumMontantValideSince(@Param("since") LocalDateTime since);

  /** Montant total des cotisations VALIDÉES d'une tontine. */
  @Query(
      "SELECT COALESCE(SUM(c.montant), 0) FROM Cotisation c"
          + " WHERE c.tontine.id = :tontineId"
          + "   AND c.statut = 'VALIDE'"
          + "   AND c.deletedAt IS NULL")
  BigDecimal sumMontantValideByTontine(@Param("tontineId") UUID tontineId);

  /** Nombre de membres distincts ayant au moins une cotisation VALIDÉE dans une tontine. */
  @Query(
      "SELECT COUNT(DISTINCT c.membre.id) FROM Cotisation c"
          + " WHERE c.tontine.id = :tontineId"
          + "   AND c.statut = 'VALIDE'"
          + "   AND c.deletedAt IS NULL")
  long countMembresAyantCotiseValide(@Param("tontineId") UUID tontineId);

  /** Montant total VALIDÉ sur toutes les tontines (non supprimées) créées par un utilisateur. */
  @Query(
      "SELECT COALESCE(SUM(c.montant), 0) FROM Cotisation c"
          + " WHERE c.tontine.creePar.id = :userId"
          + "   AND c.tontine.deletedAt IS NULL"
          + "   AND c.statut = 'VALIDE'"
          + "   AND c.deletedAt IS NULL")
  BigDecimal sumMontantValideByCreateur(@Param("userId") UUID userId);

  /** Nombre de cotisations d'un statut sur toutes les tontines (non supprimées) d'un créateur. */
  long countByTontine_CreePar_IdAndTontine_DeletedAtIsNullAndStatutAndDeletedAtIsNull(
      UUID userId, CotisationStatut statut);

  /**
   * Nombre et somme des cotisations d'un cycle, groupés par statut. Une cotisation max par membre
   * et par cycle : le nombre est donc aussi un nombre de membres. Chaque ligne : {@code [statut
   * (CotisationStatut), nombre (Long), montant (BigDecimal)]}.
   */
  @Query(
      "SELECT c.statut, COUNT(c), COALESCE(SUM(c.montant), 0) FROM Cotisation c"
          + " WHERE c.cycle.id = :cycleId"
          + "   AND c.deletedAt IS NULL"
          + " GROUP BY c.statut")
  List<Object[]> countAndSumByStatutForCycle(@Param("cycleId") UUID cycleId);

  /**
   * Montant VALIDÉ sur les cycles EN_COURS de toutes les tontines (non supprimées) d'un créateur.
   */
  @Query(
      "SELECT COALESCE(SUM(c.montant), 0) FROM Cotisation c"
          + " WHERE c.tontine.creePar.id = :userId"
          + "   AND c.tontine.deletedAt IS NULL"
          + "   AND c.cycle.statut = com.africa.dinthialma_backend.tontine.codeList.CycleStatut.EN_COURS"
          + "   AND c.cycle.deletedAt IS NULL"
          + "   AND c.statut = 'VALIDE'"
          + "   AND c.deletedAt IS NULL")
  BigDecimal sumMontantValideToursEnCoursByCreateur(@Param("userId") UUID userId);

  /**
   * Nombre et somme des cotisations par membership et par statut, pour un ensemble de memberships.
   * Chaque ligne : {@code [membreId (UUID), statut (CotisationStatut), nombre (Long), montant
   * (BigDecimal)]}.
   */
  @Query(
      "SELECT c.membre.id, c.statut, COUNT(c), COALESCE(SUM(c.montant), 0) FROM Cotisation c"
          + " WHERE c.membre.id IN :membreIds"
          + "   AND c.deletedAt IS NULL"
          + " GROUP BY c.membre.id, c.statut")
  List<Object[]> countAndSumByMembreAndStatut(@Param("membreIds") Collection<UUID> membreIds);

  /** Cotisations d'un ensemble de memberships sur un ensemble de cycles (tours en cours). */
  List<Cotisation> findByCycle_IdInAndMembre_IdInAndDeletedAtIsNull(
      Collection<UUID> cycleIds, Collection<UUID> membreIds);

  /** Cotisations enregistrées depuis une date (pour activité récente). */
  long countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(LocalDateTime since);

  /** Montant total des cotisations VALIDÉES d'un membre dans une tontine (tous cycles). */
  @Query(
      "SELECT COALESCE(SUM(c.montant), 0) FROM Cotisation c"
          + " WHERE c.tontine.id = :tontineId"
          + "   AND c.membre.id = :membreId"
          + "   AND c.statut = 'VALIDE'"
          + "   AND c.deletedAt IS NULL")
  BigDecimal sumMontantValideByTontineAndMembre(
      @Param("tontineId") UUID tontineId, @Param("membreId") UUID membreId);

  /**
   * Total cotisé (VALIDÉ) et nombre de cotisations validées, groupés par membre, pour une tontine.
   * Chaque ligne : {@code [membreId (UUID), totalMontant (BigDecimal), nombre (Long)]}.
   */
  @Query(
      "SELECT c.membre.id, COALESCE(SUM(c.montant), 0), COUNT(c) FROM Cotisation c"
          + " WHERE c.tontine.id = :tontineId"
          + "   AND c.statut = 'VALIDE'"
          + "   AND c.deletedAt IS NULL"
          + " GROUP BY c.membre.id")
  List<Object[]> sumAndCountValideGroupByMembre(@Param("tontineId") UUID tontineId);

  /**
   * Variante de {@link #sumAndCountValideGroupByMembre(UUID)} restreinte à un ensemble de membres
   * (page courante du récapitulatif). Chaque ligne : {@code [membreId, totalMontant, nombre]}.
   */
  @Query(
      "SELECT c.membre.id, COALESCE(SUM(c.montant), 0), COUNT(c) FROM Cotisation c"
          + " WHERE c.tontine.id = :tontineId"
          + "   AND c.membre.id IN :membreIds"
          + "   AND c.statut = 'VALIDE'"
          + "   AND c.deletedAt IS NULL"
          + " GROUP BY c.membre.id")
  List<Object[]> sumAndCountValideGroupByMembreIn(
      @Param("tontineId") UUID tontineId, @Param("membreIds") Collection<UUID> membreIds);
}
