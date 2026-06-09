package com.africa.dinthialma_backend.tontine.repository;

import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Accès base pour les cycles de tontine. */
@Repository
public interface CycleTontineRepository extends JpaRepository<CycleTontine, UUID> {

  /** Cycles d'une tontine triés par numéro croissant. */
  List<CycleTontine> findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(UUID tontineId);

  /** Cycles d'une tontine paginés (tri via Pageable). */
  Page<CycleTontine> findByTontine_IdAndDeletedAtIsNull(UUID tontineId, Pageable pageable);

  /** Cycle par id dans le contexte d'une tontine. */
  Optional<CycleTontine> findByIdAndTontine_IdAndDeletedAtIsNull(UUID id, UUID tontineId);

  /** Vérifie si des cycles existent pour une tontine (utile avant regénération). */
  boolean existsByTontine_IdAndDeletedAtIsNull(UUID tontineId);

  /** Nombre de cycles d'une tontine. */
  long countByTontine_IdAndDeletedAtIsNull(UUID tontineId);

  /** Dernier numéro de cycle utilisé (pour générer le suivant en mode MANUEL). */
  Optional<CycleTontine> findTopByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleDesc(
      UUID tontineId);

  /** Cycle en cours d'une tontine. */
  Optional<CycleTontine> findByTontine_IdAndStatutAndDeletedAtIsNull(
      UUID tontineId, CycleStatut statut);

  /** Tous les cycles d'un statut donné sur toute la plateforme (usage scheduler). */
  List<CycleTontine> findByStatutAndDeletedAtIsNull(CycleStatut statut);

  /** Cycles EN_COURS dont la date de fin est exactement à une date donnée (rappels scheduler). */
  @Query(
      "SELECT c FROM CycleTontine c WHERE c.statut = :statut"
          + " AND c.dateFin = :dateFin AND c.deletedAt IS NULL")
  List<CycleTontine> findByStatutAndDateFinAndDeletedAtIsNull(
      @Param("statut") CycleStatut statut, @Param("dateFin") LocalDate dateFin);

  /** Cycles TERMINÉ ayant au moins un gagnant désigné, pour l'historique jackpots d'une tontine. */
  @Query(
      value =
          "SELECT c FROM CycleTontine c WHERE c.tontine.id = :tontineId"
              + " AND c.statut = :statut AND c.deletedAt IS NULL"
              + " AND EXISTS (SELECT g FROM CycleGagnant g WHERE g.cycle = c AND g.deletedAt IS NULL)",
      countQuery =
          "SELECT COUNT(c) FROM CycleTontine c WHERE c.tontine.id = :tontineId"
              + " AND c.statut = :statut AND c.deletedAt IS NULL"
              + " AND EXISTS (SELECT g FROM CycleGagnant g WHERE g.cycle = c AND g.deletedAt IS NULL)")
  Page<CycleTontine> findTerminesAvecGagnants(
      @Param("tontineId") UUID tontineId, @Param("statut") CycleStatut statut, Pageable pageable);
}
