package com.africa.dinthialma_backend.common.audit;

import com.africa.dinthialma_backend.auth.entity.User;
import java.util.UUID;

/** Service d'écriture du journal d'audit tontine. */
public interface AuditService {

  /**
   * Enregistre une action de type UPDATE sur un champ précis.
   *
   * @param caller utilisateur ayant effectué l'action
   * @param tableName nom de la table concernée (ex. {@code "tontines"})
   * @param recordId UUID de l'enregistrement modifié
   * @param champ nom du champ modifié
   * @param ancienneVal valeur avant modification (peut être null)
   * @param nouvelleVal valeur après modification (peut être null)
   */
  void log(
      User caller,
      String tableName,
      UUID recordId,
      String champ,
      String ancienneVal,
      String nouvelleVal);

  /** Enregistre la création d'un enregistrement ({@code CREATE}). */
  void logCreate(User caller, String tableName, UUID recordId);

  /** Enregistre la suppression (soft delete) d'un enregistrement ({@code DELETE}). */
  void logDelete(User caller, String tableName, UUID recordId);
}
