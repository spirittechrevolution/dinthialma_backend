package com.africa.dinthialma_backend.tontine.repository;

import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.entity.EcheanceCycle;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EcheanceCycleRepository extends JpaRepository<EcheanceCycle, UUID> {

  /**
   * Échéances actives dont la limite tombe dans [de, a], sur un tour et une tontine aux statuts
   * donnés (rappels et cotisations manquantes, B26). Tour et tontine chargés avec l'échéance.
   */
  @Query(
      "SELECT e FROM EcheanceCycle e JOIN FETCH e.cycle c JOIN FETCH c.tontine t"
          + " WHERE e.deletedAt IS NULL AND c.deletedAt IS NULL AND t.deletedAt IS NULL"
          + " AND c.statut = :statutTour AND t.statut = :statutTontine"
          + " AND e.echeanceAt BETWEEN :de AND :a")
  List<EcheanceCycle> findActivesEntre(
      @Param("de") LocalDateTime de,
      @Param("a") LocalDateTime a,
      @Param("statutTour") CycleStatut statutTour,
      @Param("statutTontine") TontineStatut statutTontine);
}
