package com.africa.dinthialma_backend.tontine.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.tontine.dto.CycleResponse;
import com.africa.dinthialma_backend.tontine.dto.OpenCycleRequest;
import com.africa.dinthialma_backend.tontine.dto.SelectionnerBeneficiaireRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Service métier pour la gestion des cycles de tontine. */
public interface CycleService {

  /**
   * Liste tous les cycles d'une tontine.
   *
   * <p>Accès : membres + admin + SUPER_ADMIN.
   */
  Page<CycleResponse> listCycles(String keycloakId, UUID tontineId, Pageable pageable)
      throws CustomException;

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

  /**
   * Sélectionne le bénéficiaire du jackpot d'un cycle TERMINE (mode MANUEL).
   *
   * <p>Si {@code request.membreId} est fourni : désignation manuelle. Si null : sélection aléatoire
   * parmi les membres ACTIF n'ayant pas encore reçu de jackpot. Le membre sélectionné est marqué
   * {@code aRecuJackpot=true} et reçoit une notification WhatsApp. Réservé au créateur et au
   * SUPER_ADMIN.
   */
  CycleResponse selectionnerBeneficiaire(
      String keycloakId, UUID tontineId, UUID cycleId, SelectionnerBeneficiaireRequest request)
      throws CustomException;
}
