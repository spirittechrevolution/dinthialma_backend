package com.africa.dinthialma_backend.tontine.repository;

import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
