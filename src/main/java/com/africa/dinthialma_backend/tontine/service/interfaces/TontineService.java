package com.africa.dinthialma_backend.tontine.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.tontine.dto.CreateTontineRequest;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import com.africa.dinthialma_backend.tontine.dto.UpdateTontineRequest;
import java.util.List;
import java.util.UUID;

/** Service métier pour la gestion des tontines. */
public interface TontineService {

  /**
   * Crée une nouvelle tontine. L'appelant devient automatiquement l'admin (créateur) et se voit
   * assigner le rôle {@code DINTHIALMA_ADMIN} s'il ne l'a pas déjà.
   *
   * @param keycloakId sub JWT de l'utilisateur connecté
   * @param request données de création
   */
  TontineResponse createTontine(String keycloakId, CreateTontineRequest request)
      throws CustomException;

  /**
   * Récupère une tontine par son identifiant.
   *
   * <p>Accès : membres de la tontine + admin + SUPER_ADMIN.
   */
  TontineResponse getTontine(String keycloakId, UUID id) throws CustomException;

  /**
   * Liste les tontines visibles pour l'utilisateur connecté.
   *
   * <ul>
   *   <li>SUPER_ADMIN → toutes les tontines
   *   <li>Autres → tontines créées + tontines où l'utilisateur est cotisant
   * </ul>
   */
  List<TontineResponse> listTontines(String keycloakId) throws CustomException;

  /**
   * Met à jour les informations d'une tontine.
   *
   * <p>Autorisé uniquement si la tontine est en statut {@code BROUILLON}. Réservé au créateur et au
   * SUPER_ADMIN.
   */
  TontineResponse updateTontine(String keycloakId, UUID id, UpdateTontineRequest request)
      throws CustomException;

  /**
   * Suppression logique (soft delete) d'une tontine.
   *
   * <p>Autorisé uniquement si la tontine est en statut {@code BROUILLON}. Réservé au créateur et au
   * SUPER_ADMIN.
   */
  void deleteTontine(String keycloakId, UUID id) throws CustomException;

  /**
   * Active une tontine ({@code BROUILLON} → {@code ACTIVE}).
   *
   * <p>En mode {@code AUTOMATIQUE}, génère tous les cycles. Réservé au créateur et au SUPER_ADMIN.
   */
  TontineResponse activateTontine(String keycloakId, UUID id) throws CustomException;

  /**
   * Suspend une tontine ({@code ACTIVE} → {@code SUSPENDUE}).
   *
   * <p>Réservé au créateur, à l'ADMIN et au SUPER_ADMIN.
   */
  TontineResponse suspendTontine(String keycloakId, UUID id) throws CustomException;
}
