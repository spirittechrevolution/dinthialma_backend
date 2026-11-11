package com.africa.dinthialma_backend.tontine.repository;

import com.africa.dinthialma_backend.tontine.entity.CycleGagnant;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CycleGagnantRepository extends JpaRepository<CycleGagnant, UUID> {

  List<CycleGagnant> findByCycle_IdAndDeletedAtIsNull(UUID cycleId);

  boolean existsByCycle_IdAndDeletedAtIsNull(UUID cycleId);

  /**
   * Total des jackpots reçus par un utilisateur, toutes tontines (non supprimées) confondues. Les
   * memberships sortis depuis sont inclus : le jackpot a bien été perçu.
   */
  @Query(
      "SELECT COALESCE(SUM(g.montantRecu), 0) FROM CycleGagnant g"
          + " WHERE g.membre.user.id = :userId"
          + "   AND g.deletedAt IS NULL"
          + "   AND g.cycle.deletedAt IS NULL"
          + "   AND g.cycle.tontine.deletedAt IS NULL")
  BigDecimal sumMontantRecuByUserId(@Param("userId") UUID userId);
}
