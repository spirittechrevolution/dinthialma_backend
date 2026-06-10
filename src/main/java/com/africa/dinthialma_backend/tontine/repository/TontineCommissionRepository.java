package com.africa.dinthialma_backend.tontine.repository;

import com.africa.dinthialma_backend.tontine.entity.TontineCommission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Accès base pour les commissions de tontine. */
@Repository
public interface TontineCommissionRepository extends JpaRepository<TontineCommission, UUID> {

  /** Commissions actives d'une tontine. */
  List<TontineCommission> findByTontine_IdAndDeletedAtIsNull(UUID tontineId);

  /** Commissions actives d'une tontine paginées (tri via Pageable). */
  Page<TontineCommission> findByTontine_IdAndDeletedAtIsNull(UUID tontineId, Pageable pageable);

  /** Commission par id dans le contexte d'une tontine. */
  Optional<TontineCommission> findByIdAndTontine_IdAndDeletedAtIsNull(UUID id, UUID tontineId);
}
