package com.africa.dinthialma_backend.contribution.repository;

import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Accès base pour les cotisations. */
@Repository
public interface CotisationRepository extends JpaRepository<Cotisation, UUID> {

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
   * Cotisation d'un membre pour un cycle précis (contrainte unicité {@code uq_cot_cycle_membre}).
   */
  Optional<Cotisation> findByCycle_IdAndMembre_IdAndDeletedAtIsNull(UUID cycleId, UUID membreId);

  /** Vérifie si un membre a déjà enregistré une cotisation pour un cycle. */
  boolean existsByCycle_IdAndMembre_IdAndDeletedAtIsNull(UUID cycleId, UUID membreId);

  /** Cotisations par statut pour une tontine. */
  List<Cotisation> findByTontine_IdAndStatutAndDeletedAtIsNull(
      UUID tontineId, CotisationStatut statut);

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
}
