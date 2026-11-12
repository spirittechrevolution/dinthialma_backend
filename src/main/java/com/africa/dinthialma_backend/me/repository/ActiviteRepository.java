package com.africa.dinthialma_backend.me.repository;

import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Fil d'activité (B09) : fusion paginée des cotisations et des jackpots en une seule requête SQL
 * {@code UNION ALL}, triée par date d'événement décroissante.
 *
 * <p>Chaque cotisation apparaît une fois, dans son état courant (déclarée, validée, encaissée par
 * le gestionnaire, en retard, refusée) ; chaque jackpot versé apparaît comme JACKPOT_RECU.
 *
 * <p>Périmètre : {@code gere=false} → mes propres cotisations et jackpots ; {@code gere=true} →
 * ceux des tontines que j'ai créées.
 */
public interface ActiviteRepository extends Repository<Cotisation, UUID> {

  String SOURCE =
      " FROM ("
          + " SELECT c.id AS id,"
          + "   CASE"
          + "     WHEN c.statut = 'EN_ATTENTE' THEN 'COTISATION_DECLAREE'"
          + "     WHEN c.statut = 'VALIDE' AND c.enregistre_par IS NOT NULL"
          + "          AND c.enregistre_par <> m.user_id THEN 'COTISATION_ENCAISSEE'"
          + "     WHEN c.statut = 'VALIDE' THEN 'COTISATION_VALIDEE'"
          + "     WHEN c.statut = 'EN_RETARD' THEN 'COTISATION_EN_RETARD'"
          + "     ELSE 'COTISATION_REFUSEE'"
          + "   END AS type,"
          + "   CAST(COALESCE(c.date_refus, c.date_validation, c.created_at) AS timestamp)"
          + "     AS date_evenement,"
          + "   c.montant AS montant, 'EPARGNE' AS sens, c.statut AS statut,"
          + "   t.id AS tontine_id, t.nom AS tontine_nom, cy.id AS cycle_id,"
          + "   cy.numero_cycle AS numero_cycle, c.id AS cotisation_id, m.id AS membre_id,"
          + "   u.first_name AS membre_prenom, u.last_name AS membre_nom"
          + " FROM dinthialma.cotisations c"
          + " JOIN dinthialma.tontines t ON t.id = c.tontine_id"
          + " JOIN dinthialma.tontine_membres m ON m.id = c.membre_id"
          + " JOIN dinthialma.users u ON u.id = m.user_id"
          + " JOIN dinthialma.cycles_tontine cy ON cy.id = c.cycle_id"
          + " WHERE c.deleted_at IS NULL AND t.deleted_at IS NULL"
          + "   AND ((:gere = true AND t.cree_par = :userId)"
          + "     OR (:gere = false AND m.user_id = :userId))"
          + " UNION ALL"
          + " SELECT g.id AS id, 'JACKPOT_RECU' AS type,"
          + "   CAST(COALESCE(CAST(cy.date_remise AS timestamp), g.created_at) AS timestamp)"
          + "     AS date_evenement,"
          + "   g.montant_recu AS montant, 'GAIN' AS sens, NULL AS statut,"
          + "   t.id AS tontine_id, t.nom AS tontine_nom, cy.id AS cycle_id,"
          + "   cy.numero_cycle AS numero_cycle, NULL AS cotisation_id, m.id AS membre_id,"
          + "   u.first_name AS membre_prenom, u.last_name AS membre_nom"
          + " FROM dinthialma.cycle_gagnants g"
          + " JOIN dinthialma.cycles_tontine cy ON cy.id = g.cycle_id"
          + " JOIN dinthialma.tontines t ON t.id = cy.tontine_id"
          + " JOIN dinthialma.tontine_membres m ON m.id = g.membre_id"
          + " JOIN dinthialma.users u ON u.id = m.user_id"
          + " WHERE g.deleted_at IS NULL AND cy.deleted_at IS NULL AND t.deleted_at IS NULL"
          + "   AND ((:gere = true AND t.cree_par = :userId)"
          + "     OR (:gere = false AND m.user_id = :userId))"
          + " ) a"
          + " WHERE (CAST(:tontineId AS uuid) IS NULL OR a.tontine_id = CAST(:tontineId AS uuid))"
          + "   AND (:filtrerType = false OR a.type IN (:types))"
          + "   AND (CAST(:statut AS varchar) IS NULL OR a.statut = CAST(:statut AS varchar))";

  @Query(
      nativeQuery = true,
      value =
          "SELECT a.id AS \"id\", a.type AS \"type\", a.date_evenement AS \"dateEvenement\","
              + " a.montant AS \"montant\", a.sens AS \"sens\", a.statut AS \"statut\","
              + " a.tontine_id AS \"tontineId\", a.tontine_nom AS \"tontineNom\","
              + " a.cycle_id AS \"cycleId\", a.numero_cycle AS \"numeroCycle\","
              + " a.cotisation_id AS \"cotisationId\", a.membre_id AS \"membreId\","
              + " a.membre_prenom AS \"membrePrenom\", a.membre_nom AS \"membreNom\""
              + SOURCE
              + " ORDER BY a.date_evenement DESC, a.id",
      countQuery = "SELECT COUNT(*)" + SOURCE)
  Page<ActiviteRow> findActivite(
      @Param("userId") UUID userId,
      @Param("gere") boolean gere,
      @Param("tontineId") UUID tontineId,
      @Param("filtrerType") boolean filtrerType,
      @Param("types") Collection<String> types,
      @Param("statut") String statut,
      Pageable pageable);

  /** Ligne brute du fil d'activité (projection de la requête native). */
  interface ActiviteRow {
    UUID getId();

    String getType();

    LocalDateTime getDateEvenement();

    BigDecimal getMontant();

    String getSens();

    String getStatut();

    UUID getTontineId();

    String getTontineNom();

    UUID getCycleId();

    Integer getNumeroCycle();

    UUID getCotisationId();

    UUID getMembreId();

    String getMembrePrenom();

    String getMembreNom();
  }
}
