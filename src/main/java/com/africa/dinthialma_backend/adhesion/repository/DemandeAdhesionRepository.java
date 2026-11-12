package com.africa.dinthialma_backend.adhesion.repository;

import com.africa.dinthialma_backend.adhesion.codeList.StatutDemande;
import com.africa.dinthialma_backend.adhesion.entity.DemandeAdhesion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DemandeAdhesionRepository extends JpaRepository<DemandeAdhesion, UUID> {

  boolean existsByTontine_IdAndUser_IdAndStatutAndDeletedAtIsNull(
      UUID tontineId, UUID userId, StatutDemande statut);

  @EntityGraph(attributePaths = {"tontine", "user"})
  Optional<DemandeAdhesion> findByIdAndTontine_IdAndDeletedAtIsNull(UUID id, UUID tontineId);

  /** Demandes reçues sur les tontines (non supprimées) d'un créateur, filtres optionnels. */
  @EntityGraph(attributePaths = {"tontine", "user"})
  @Query(
      value =
          "SELECT d FROM DemandeAdhesion d"
              + " WHERE d.deletedAt IS NULL"
              + "   AND d.tontine.deletedAt IS NULL"
              + "   AND d.tontine.creePar.id = :userId"
              + "   AND (:statut IS NULL OR d.statut = :statut)"
              + "   AND (:tontineId IS NULL OR d.tontine.id = :tontineId)",
      countQuery =
          "SELECT COUNT(d) FROM DemandeAdhesion d"
              + " WHERE d.deletedAt IS NULL"
              + "   AND d.tontine.deletedAt IS NULL"
              + "   AND d.tontine.creePar.id = :userId"
              + "   AND (:statut IS NULL OR d.statut = :statut)"
              + "   AND (:tontineId IS NULL OR d.tontine.id = :tontineId)")
  Page<DemandeAdhesion> findRecues(
      @Param("userId") UUID userId,
      @Param("statut") StatutDemande statut,
      @Param("tontineId") UUID tontineId,
      Pageable pageable);

  /** Mes demandes envoyées. */
  @EntityGraph(attributePaths = {"tontine"})
  Page<DemandeAdhesion> findByUser_IdAndDeletedAtIsNull(UUID userId, Pageable pageable);

  /** Demandes d'un statut reçues sur toutes les tontines (non supprimées) d'un créateur. */
  long countByTontine_CreePar_IdAndTontine_DeletedAtIsNullAndStatutAndDeletedAtIsNull(
      UUID createurId, StatutDemande statut);

  /** Demandes d'un statut reçues sur une tontine. */
  long countByTontine_IdAndStatutAndDeletedAtIsNull(UUID tontineId, StatutDemande statut);
}
