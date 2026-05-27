package com.africa.dinthialma_backend.tontine.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.tontine.dto.CycleResponse;
import com.africa.dinthialma_backend.tontine.dto.OpenCycleRequest;
import java.util.List;
import java.util.UUID;

/** Service métier pour la gestion des cycles de tontine. */
public interface CycleService {

  /**
   * Liste tous les cycles d'une tontine.
   *
   * <p>Accès : membres + admin + SUPER_ADMIN.
   */
  List<CycleResponse> listCycles(String keycloakId, UUID tontineId) throws CustomException;

  /**
   * Récupère un cycle par son identifiant.
   *
   * <p>Accès : membres + admin + SUPER_ADMIN.
   */
  CycleResponse getCycle(String keycloakId, UUID tontineId, UUID cycleId) throws CustomException;

  /**
   * Ouvre un nouveau cycle manuellement (mode {@code MANUEL} uniquement).
   *
   * <p>Réservé au créateur de la tontine et au SUPER_ADMIN.
   */
  CycleResponse openCycle(String keycloakId, UUID tontineId, OpenCycleRequest request)
      throws CustomException;

  /**
   * Clôture un cycle en cours ({@code EN_COURS} → {@code TERMINE}).
   *
   * <p>Calcule le montant jackpot, enregistre la date de remise et met le cycle suivant en {@code
   * EN_COURS} si applicable. Réservé au créateur et au SUPER_ADMIN.
   */
  CycleResponse closeCycle(String keycloakId, UUID tontineId, UUID cycleId) throws CustomException;
}
