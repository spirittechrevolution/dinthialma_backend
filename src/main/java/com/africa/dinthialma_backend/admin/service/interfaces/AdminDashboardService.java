package com.africa.dinthialma_backend.admin.service.interfaces;

import com.africa.dinthialma_backend.admin.dto.AdminUserResponse;
import com.africa.dinthialma_backend.admin.dto.GlobalDashboardResponse;
import com.africa.dinthialma_backend.admin.dto.MyDashboardResponse;
import com.africa.dinthialma_backend.admin.dto.UpdateUserRolesRequest;
import com.africa.dinthialma_backend.common.exception.CustomException;
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
   * @param pageable pagination (page, size, sort)
   */
  Page<AdminUserResponse> listUsers(String keycloakId, Pageable pageable) throws CustomException;

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

  // ─── ADMIN (créateur tontines) ───────────────────────────────────────────

  /**
   * Retourne le tableau de bord de l'admin pour ses propres tontines.
   *
   * <p>Accessible à tout utilisateur ayant au moins une tontine créée. Les stats (EN_ATTENTE,
   * EN_RETARD, cycle en cours) sont calculées par tontine.
   */
  MyDashboardResponse getMyDashboard(String keycloakId) throws CustomException;
}
