package com.africa.dinthialma_backend.member.repository;

import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Accès base pour les cotisants (membres) d'une tontine. */
@Repository
public interface TontineMembreRepository extends JpaRepository<TontineMembre, UUID> {

  /**
   * Membres non supprimés d'une tontine triés d'abord par ordre_jackpot, puis par date d'adhésion.
   */
  List<TontineMembre> findByTontine_IdAndDeletedAtIsNullOrderByOrdreJackpotAscCreatedAtAsc(
      UUID tontineId);

  /** Membres non supprimés d'une tontine paginés (tri via Pageable). */
  Page<TontineMembre> findByTontine_IdAndDeletedAtIsNull(UUID tontineId, Pageable pageable);

  /** Membres actifs d'une tontine (pour génération des cycles). */
  List<TontineMembre> findByTontine_IdAndStatutAndDeletedAtIsNull(
      UUID tontineId, MembreStatut statut);

  /** Vérifie si un utilisateur est déjà cotisant dans une tontine. */
  boolean existsByTontine_IdAndUser_IdAndDeletedAtIsNull(UUID tontineId, UUID userId);

  /** Récupère l'enregistrement cotisant d'un utilisateur dans une tontine. */
  Optional<TontineMembre> findByTontine_IdAndUser_IdAndDeletedAtIsNull(UUID tontineId, UUID userId);

  /** Récupère un membre par son id dans le contexte d'une tontine. */
  Optional<TontineMembre> findByIdAndTontine_IdAndDeletedAtIsNull(UUID id, UUID tontineId);

  /** Nombre de cotisants actifs dans une tontine. */
  long countByTontine_IdAndDeletedAtIsNull(UUID tontineId);

  /** Vérifie si un ordre jackpot est déjà pris dans une tontine. */
  boolean existsByTontine_IdAndOrdreJackpotAndDeletedAtIsNull(UUID tontineId, int ordreJackpot);

  /** Membre actif dont l'ordre jackpot est exact (futur bénéficiaire ROTATION). */
  Optional<TontineMembre> findByTontine_IdAndOrdreJackpotAndStatutAndDeletedAtIsNull(
      UUID tontineId, int ordreJackpot, MembreStatut statut);

  /** Ordre jackpot maximum attribué dans une tontine (0 si aucun). */
  @Query(
      "SELECT COALESCE(MAX(m.ordreJackpot), 0) FROM TontineMembre m"
          + " WHERE m.tontine.id = :tontineId AND m.deletedAt IS NULL")
  int findMaxOrdreJackpot(@Param("tontineId") UUID tontineId);
}
