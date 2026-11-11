package com.africa.dinthialma_backend.tontine.repository;

import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
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

/** Accès base pour les tontines – filtre systématique sur {@code deleted_at IS NULL}. */
@Repository
public interface TontineRepository
    extends JpaRepository<Tontine, UUID>, JpaSpecificationExecutor<Tontine> {

  /** Liste filtrée (cf. {@link TontineSpecifications}) avec le créateur chargé. */
  @Override
  @EntityGraph(attributePaths = {"creePar"})
  Page<Tontine> findAll(Specification<Tontine> spec, Pageable pageable);

  /** Tontines créées par un utilisateur (admin de ces tontines). */
  List<Tontine> findByCreePar_IdAndDeletedAtIsNull(UUID userId);

  /** Tontines créées par un utilisateur paginées (dashboard admin). */
  Page<Tontine> findByCreePar_IdAndDeletedAtIsNull(UUID userId, Pageable pageable);

  /** Tontines non supprimées d'un statut donné paginées (usage SUPER_ADMIN). */
  Page<Tontine> findByStatutAndDeletedAtIsNull(TontineStatut statut, Pageable pageable);

  /** Toutes les tontines non supprimées (usage SUPER_ADMIN). */
  List<Tontine> findByDeletedAtIsNull();

  /** Toutes les tontines non supprimées paginées (usage SUPER_ADMIN). */
  Page<Tontine> findByDeletedAtIsNull(Pageable pageable);

  /** Tontine non supprimée par code d'invitation (B16). */
  Optional<Tontine> findByCodeInvitationAndDeletedAtIsNull(String codeInvitation);

  boolean existsByCodeInvitation(String codeInvitation);

  /** Tontine non supprimée par id. */
  Optional<Tontine> findByIdAndDeletedAtIsNull(UUID id);

  /**
   * Tontines auxquelles l'utilisateur cotise (via {@code tontine_membres}).
   *
   * <p>Inclut aussi les tontines qu'il administre s'il est inscrit comme cotisant.
   */
  @Query(
      "SELECT t FROM Tontine t"
          + " JOIN TontineMembre m ON m.tontine = t"
          + " WHERE m.user.id = :userId"
          + "   AND m.deletedAt IS NULL"
          + "   AND t.deletedAt IS NULL")
  List<Tontine> findByMembreUserId(@Param("userId") UUID userId);

  /** Tontines auxquelles l'utilisateur cotise, paginées (filtre {@code ?role=COTISE}). */
  @Query(
      value =
          "SELECT t FROM Tontine t"
              + " WHERE t.deletedAt IS NULL"
              + "   AND EXISTS ("
              + "     SELECT 1 FROM TontineMembre m"
              + "     WHERE m.tontine = t AND m.user.id = :userId AND m.deletedAt IS NULL"
              + "   )",
      countQuery =
          "SELECT COUNT(t) FROM Tontine t"
              + " WHERE t.deletedAt IS NULL"
              + "   AND EXISTS ("
              + "     SELECT 1 FROM TontineMembre m"
              + "     WHERE m.tontine = t AND m.user.id = :userId AND m.deletedAt IS NULL"
              + "   )")
  Page<Tontine> findByMembreUserId(@Param("userId") UUID userId, Pageable pageable);

  /**
   * Tontines visibles par un utilisateur : celles qu'il a créées + celles où il est cotisant.
   *
   * <p>Utilisé pour construire la liste "Mes tontines" (ADMIN + MEMBER).
   */
  @Query(
      "SELECT t FROM Tontine t"
          + " WHERE t.deletedAt IS NULL"
          + "   AND ("
          + "     t.creePar.id = :userId"
          + "     OR EXISTS ("
          + "       SELECT 1 FROM TontineMembre m"
          + "       WHERE m.tontine = t AND m.user.id = :userId AND m.deletedAt IS NULL"
          + "     )"
          + "   )")
  List<Tontine> findVisibleByUserId(@Param("userId") UUID userId);

  @Query(
      value =
          "SELECT t FROM Tontine t"
              + " WHERE t.deletedAt IS NULL"
              + "   AND ("
              + "     t.creePar.id = :userId"
              + "     OR EXISTS ("
              + "       SELECT 1 FROM TontineMembre m"
              + "       WHERE m.tontine = t AND m.user.id = :userId AND m.deletedAt IS NULL"
              + "     )"
              + "   )",
      countQuery =
          "SELECT COUNT(t) FROM Tontine t"
              + " WHERE t.deletedAt IS NULL"
              + "   AND ("
              + "     t.creePar.id = :userId"
              + "     OR EXISTS ("
              + "       SELECT 1 FROM TontineMembre m"
              + "       WHERE m.tontine = t AND m.user.id = :userId AND m.deletedAt IS NULL"
              + "     )"
              + "   )")
  Page<Tontine> findVisibleByUserId(@Param("userId") UUID userId, Pageable pageable);

  /** Compte les tontines non supprimées par statut – pour les stats dashboard. */
  long countByStatutAndDeletedAtIsNull(TontineStatut statut);

  /** Total tontines non supprimées. */
  long countByDeletedAtIsNull();
}
