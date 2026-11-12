package com.africa.dinthialma_backend.jackpot.repository;

import com.africa.dinthialma_backend.jackpot.codeList.StatutPriorite;
import com.africa.dinthialma_backend.jackpot.entity.PrioriteJackpot;
import java.util.Collection;
import java.util.List;
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
public interface PrioriteJackpotRepository extends JpaRepository<PrioriteJackpot, UUID> {

  boolean existsByMembre_IdAndStatutInAndDeletedAtIsNull(
      UUID membreId, Collection<StatutPriorite> statuts);

  @EntityGraph(attributePaths = {"tontine", "membre", "membre.user"})
  Optional<PrioriteJackpot> findByIdAndTontine_IdAndDeletedAtIsNull(UUID id, UUID tontineId);

  /** Priorités d'une tontine, statut optionnel. */
  @EntityGraph(attributePaths = {"membre", "membre.user"})
  @Query(
      value =
          "SELECT p FROM PrioriteJackpot p WHERE p.tontine.id = :tontineId AND p.deletedAt IS NULL"
              + " AND (:statut IS NULL OR p.statut = :statut)",
      countQuery =
          "SELECT COUNT(p) FROM PrioriteJackpot p WHERE p.tontine.id = :tontineId"
              + " AND p.deletedAt IS NULL AND (:statut IS NULL OR p.statut = :statut)")
  Page<PrioriteJackpot> findByTontine(
      @Param("tontineId") UUID tontineId,
      @Param("statut") StatutPriorite statut,
      Pageable pageable);

  /** Priorités d'un statut, dans l'ordre de passage (date d'acceptation, puis de création). */
  @EntityGraph(attributePaths = {"membre", "membre.user"})
  List<PrioriteJackpot>
      findByTontine_IdAndStatutAndDeletedAtIsNullOrderByDateTraitementAscCreatedAtAsc(
          UUID tontineId, StatutPriorite statut);

  /** Priorités de ces memberships ayant l'un de ces statuts. */
  List<PrioriteJackpot> findByMembre_IdInAndStatutInAndDeletedAtIsNull(
      Collection<UUID> membreIds, Collection<StatutPriorite> statuts);

  /** Priorités d'un statut sur une tontine (tableau de bord gestionnaire). */
  long countByTontine_IdAndStatutAndDeletedAtIsNull(UUID tontineId, StatutPriorite statut);
}
