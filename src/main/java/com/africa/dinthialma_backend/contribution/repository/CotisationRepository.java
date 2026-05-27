package com.africa.dinthialma_backend.contribution.repository;

import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Accès base pour les cotisations. */
@Repository
public interface CotisationRepository extends JpaRepository<Cotisation, UUID> {

  /** Cotisations d'un cycle. */
  List<Cotisation> findByCycle_IdAndDeletedAtIsNull(UUID cycleId);

  /** Cotisations d'une tontine (tous cycles confondus). */
  List<Cotisation> findByTontine_IdAndDeletedAtIsNull(UUID tontineId);

  /** Cotisations d'un membre (tous cycles confondus). */
  List<Cotisation> findByMembre_IdAndDeletedAtIsNull(UUID membreId);

  /** Cotisations d'un membre pour un tontine donnée. */
  List<Cotisation> findByTontine_IdAndMembre_IdAndDeletedAtIsNull(UUID tontineId, UUID membreId);

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
}
