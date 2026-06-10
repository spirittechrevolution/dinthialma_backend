package com.africa.dinthialma_backend.tontine.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.tontine.dto.CommissionResponse;
import com.africa.dinthialma_backend.tontine.dto.CreateCommissionRequest;
import com.africa.dinthialma_backend.tontine.dto.UpdateCommissionRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Service métier pour la gestion des commissions d'une tontine. */
public interface CommissionService {

  /**
   * Crée une commission sur une tontine.
   *
   * <p>Seul un type de commission par tontine est autorisé ({@code UNIQUE(tontine_id, type)}).
   * Réservé au créateur de la tontine et au SUPER_ADMIN.
   *
   * @param keycloakId sub JWT de l'appelant
   * @param tontineId identifiant de la tontine
   * @param request données de la commission
   */
  CommissionResponse createCommission(
      String keycloakId, UUID tontineId, CreateCommissionRequest request) throws CustomException;

  /**
   * Liste les commissions actives d'une tontine.
   *
   * <p>Accès : membres de la tontine, créateur, SUPER_ADMIN.
   */
  Page<CommissionResponse> listCommissions(String keycloakId, UUID tontineId, Pageable pageable)
      throws CustomException;

  /**
   * Modifie la valeur et/ou la description d'une commission.
   *
   * <p>Réservé au créateur de la tontine et au SUPER_ADMIN.
   */
  CommissionResponse updateCommission(
      String keycloakId, UUID tontineId, UUID commissionId, UpdateCommissionRequest request)
      throws CustomException;

  /**
   * Supprime (soft delete) une commission.
   *
   * <p>Réservé au créateur de la tontine et au SUPER_ADMIN.
   */
  void deleteCommission(String keycloakId, UUID tontineId, UUID commissionId)
      throws CustomException;
}
