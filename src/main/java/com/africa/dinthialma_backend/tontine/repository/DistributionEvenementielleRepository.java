package com.africa.dinthialma_backend.tontine.repository;

import com.africa.dinthialma_backend.tontine.entity.DistributionEvenementielle;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributionEvenementielleRepository
    extends JpaRepository<DistributionEvenementielle, UUID> {

  /** Distribution déjà enregistrée pour cette tontine (protection contre une double clôture). */
  boolean existsByTontine_IdAndDeletedAtIsNull(UUID tontineId);

  /**
   * Montant net et nombre de distributions reçues, par adhésion : lignes {@code [membreId (UUID),
   * total (BigDecimal), nombre (Long)]} ; une adhésion sans distribution n'a pas de ligne.
   */
  @Query(
      "SELECT d.membre.id, COALESCE(SUM(d.montantNet), 0), COUNT(d) FROM DistributionEvenementielle d"
          + " WHERE d.membre.id IN :membreIds"
          + "   AND d.deletedAt IS NULL"
          + "   AND d.tontine.deletedAt IS NULL"
          + " GROUP BY d.membre.id")
  List<Object[]> sumAndCountByMembreIds(@Param("membreIds") Collection<UUID> membreIds);

  /** Total net reçu par un utilisateur, toutes tontines événementielles confondues. */
  @Query(
      "SELECT COALESCE(SUM(d.montantNet), 0) FROM DistributionEvenementielle d"
          + " WHERE d.membre.user.id = :userId"
          + "   AND d.deletedAt IS NULL"
          + "   AND d.tontine.deletedAt IS NULL")
  BigDecimal sumMontantNetByUserId(@Param("userId") UUID userId);

  /** Distributions reçues par un utilisateur, cycle et tontine chargés (plus récentes d'abord). */
  @Query(
      "SELECT d FROM DistributionEvenementielle d"
          + " JOIN FETCH d.cycle c"
          + " JOIN FETCH d.tontine t"
          + " WHERE d.membre.user.id = :userId"
          + "   AND d.deletedAt IS NULL"
          + "   AND t.deletedAt IS NULL"
          + " ORDER BY c.dateFin DESC")
  List<DistributionEvenementielle> findRecuesByUserId(@Param("userId") UUID userId);
}
