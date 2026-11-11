package com.africa.dinthialma_backend.admin.service.interfaces;

import com.africa.dinthialma_backend.admin.dto.AdminUserResponse;
import com.africa.dinthialma_backend.admin.dto.GlobalDashboardResponse;
import com.africa.dinthialma_backend.admin.dto.MyDashboardResponse;
import com.africa.dinthialma_backend.admin.dto.UpdateUserRolesRequest;
import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Service d'administration – métriques globales et gestion des utilisateurs. */
public interface AdminDashboardService {

  // ─── SUPER_ADMIN ─────────────────────────────────────────────────────────

  /**
   * Retourne les métriques globales de la plateforme.
   *
   * <p>Réservé au SUPER_ADMIN.
   */
  GlobalDashboardResponse getGlobalDashboard(String keycloakId) throws CustomException;

  /**
   * Liste paginée de tous les utilisateurs.
   *
   * <p>Réservé au SUPER_ADMIN.
   *
   * @param accountStatus filtre optionnel (null = tous ; ACTIVE inclut les comptes sans statut)
   * @param pageable pagination (page, size, sort)
   */
  Page<AdminUserResponse> listUsers(
      String keycloakId, AccountStatus accountStatus, UserRole role, String q, Pageable pageable)
      throws CustomException;

  /**
   * Détail complet d'un utilisateur.
   *
   * <p>Réservé au SUPER_ADMIN.
   */
  AdminUserResponse getUserDetail(String keycloakId, UUID userId) throws CustomException;

  /**
   * Désactive un compte utilisateur ({@code active = false} + Keycloak disabled).
   *
   * <p>Réservé au SUPER_ADMIN. Un SUPER_ADMIN ne peut pas se désactiver lui-même.
   */
  AdminUserResponse disableUser(String keycloakId, UUID userId) throws CustomException;

  /**
   * Réactive un compte utilisateur désactivé.
   *
   * <p>Réservé au SUPER_ADMIN.
   */
  AdminUserResponse enableUser(String keycloakId, UUID userId) throws CustomException;

  /**
   * Remplace l'ensemble des rôles d'un utilisateur (replace-all idempotent).
   *
   * <p>Réservé au SUPER_ADMIN. Le rôle {@code USER} ne peut pas être retiré.
   */
  AdminUserResponse updateUserRoles(String keycloakId, UUID userId, UpdateUserRolesRequest request)
      throws CustomException;

  /**
   * Liste paginée de toutes les tontines de la plateforme, filtrable par statut.
   *
   * <p>Réservé au SUPER_ADMIN.
   *
   * @param statut filtre optionnel (null = tous les statuts)
   * @param pageable pagination (tri par défaut : createdAt DESC)
   */
  Page<TontineResponse> listAllTontines(
      String keycloakId, TontineStatut statut, String q, Pageable pageable) throws CustomException;

  /**
   * Liste paginée de toutes les cotisations de la plateforme, filtrable par statut et tontine.
   *
   * <p>Réservé au SUPER_ADMIN.
   *
   * @param statut filtre optionnel (null = tous les statuts)
   * @param tontineId filtre optionnel (null = toutes les tontines)
   * @param pageable pagination (tri par défaut : createdAt DESC)
   */
  Page<CotisationResponse> listAllContributions(
      String keycloakId, CotisationStatut statut, UUID tontineId, Pageable pageable)
      throws CustomException;

  // ─── ADMIN (créateur tontines) ───────────────────────────────────────────

  /**
   * Retourne le tableau de bord de l'admin pour ses propres tontines, paginé par tontine.
   *
   * <p>Accessible à tout utilisateur ayant au moins une tontine créée. Les stats (EN_ATTENTE,
   * EN_RETARD, cycle en cours) sont calculées par tontine, uniquement pour la page demandée.
   *
   * @param pageable pagination des tontines (tri par défaut : createdAt DESC)
   */
  MyDashboardResponse getMyDashboard(String keycloakId, Pageable pageable) throws CustomException;
}
