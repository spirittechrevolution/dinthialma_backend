package com.africa.dinthialma_backend.tontine.service.interfaces;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.tontine.dto.CommissionResponse;
import com.africa.dinthialma_backend.tontine.dto.CreateCommissionRequest;
import com.africa.dinthialma_backend.tontine.dto.UpdateCommissionRequest;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import java.util.List;
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
   * Vérifie les commissions demandées à la création d'une tontine (B30), avant tout enregistrement.
   * Règles : un type au plus une fois, pourcentage du jackpot ≤ 100 %.
   *
   * <p>À appeler <b>avant</b> d'enregistrer la tontine : une {@link CustomException} (vérifiée)
   * n'annule pas la transaction.
   *
   * @param commissions commissions demandées ({@code null} ou vide = aucune)
   */
  void verifierCommissionsInitiales(List<CreateCommissionRequest> commissions)
      throws CustomException;

  /**
   * Enregistre, dans la transaction de création de la tontine, les commissions déjà vérifiées par
   * {@link #verifierCommissionsInitiales(List)}.
   *
   * @param createur créateur de la tontine (auteur de l'audit)
   * @param tontine tontine qui vient d'être enregistrée
   * @param commissions commissions demandées ({@code null} ou vide = aucune)
   */
  void enregistrerCommissionsInitiales(
      User createur, Tontine tontine, List<CreateCommissionRequest> commissions);

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
