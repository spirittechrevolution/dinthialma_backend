package com.africa.dinthialma_backend.admin.controller;

import com.africa.dinthialma_backend.admin.dto.AdminUserResponse;
import com.africa.dinthialma_backend.admin.dto.GlobalDashboardResponse;
import com.africa.dinthialma_backend.admin.dto.MyDashboardResponse;
import com.africa.dinthialma_backend.admin.dto.UpdateUserRolesRequest;
import com.africa.dinthialma_backend.admin.service.interfaces.AdminDashboardService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints d'administration.
 *
 * <p>Deux groupes :
 *
 * <ul>
 *   <li>{@code /v1/admin/dashboard/**} – métriques globales + gestion utilisateurs (SUPER_ADMIN)
 *   <li>{@code /v1/admin/my-dashboard} – métriques personnelles pour l'admin d'une tontine
 * </ul>
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Dashboard et gestion des utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

  private static final String SUCCESS = "success";
  private static final int OK = 200;

  private final AdminDashboardService adminDashboardService;
  private final RequestHeaderParser headerParser;

  // ─── Dashboard global (SUPER_ADMIN) ──────────────────────────────────────

  @GetMapping("/dashboard")
  @Operation(
      summary = "Dashboard global plateforme",
      description =
          "Métriques globales : utilisateurs, tontines, finances, activité 24h. "
              + "🔒 Rôle requis : SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> getGlobalDashboard(HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    GlobalDashboardResponse response = adminDashboardService.getGlobalDashboard(keycloakId);

    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.ADMIN_DASHBOARD_SUCCESS, response));
  }

  // ─── Gestion des utilisateurs (SUPER_ADMIN) ──────────────────────────────

  @GetMapping("/dashboard/users")
  @Operation(
      summary = "Liste paginée des utilisateurs",
      description =
          "Retourne tous les utilisateurs non supprimés (pageable). "
              + "🔒 Rôle requis : SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> listUsers(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    Page<AdminUserResponse> page = adminDashboardService.listUsers(keycloakId, pageable);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.ADMIN_USERS_LIST_SUCCESS, page));
  }

  @GetMapping("/dashboard/users/{userId}")
  @Operation(summary = "Détail d'un utilisateur", description = "🔒 Rôle requis : SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> getUserDetail(
      @PathVariable UUID userId, HttpServletRequest httpRequest) throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    AdminUserResponse response = adminDashboardService.getUserDetail(keycloakId, userId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.ADMIN_USER_GET_SUCCESS, response));
  }

  @PostMapping("/dashboard/users/{userId}/disable")
  @Operation(
      summary = "Désactiver un compte utilisateur",
      description =
          "Désactive le compte (active = false + Keycloak disabled). "
              + "Un SUPER_ADMIN ne peut pas se désactiver lui-même. "
              + "🔒 Rôle requis : SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> disableUser(
      @PathVariable UUID userId, HttpServletRequest httpRequest) throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    AdminUserResponse response = adminDashboardService.disableUser(keycloakId, userId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.ADMIN_USER_DISABLED, response));
  }

  @PostMapping("/dashboard/users/{userId}/enable")
  @Operation(
      summary = "Réactiver un compte utilisateur",
      description = "Réactive un compte désactivé. 🔒 Rôle requis : SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> enableUser(
      @PathVariable UUID userId, HttpServletRequest httpRequest) throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    AdminUserResponse response = adminDashboardService.enableUser(keycloakId, userId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.ADMIN_USER_ENABLED, response));
  }

  @PutMapping("/dashboard/users/{userId}/roles")
  @Operation(
      summary = "Modifier les rôles d'un utilisateur",
      description =
          "Remplace l'ensemble des rôles (replace-all idempotent). Le rôle USER est "
              + "toujours conservé. 🔒 Rôle requis : SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> updateUserRoles(
      @PathVariable UUID userId,
      @RequestBody @Valid UpdateUserRolesRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    AdminUserResponse response = adminDashboardService.updateUserRoles(keycloakId, userId, request);

    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.ADMIN_USER_ROLES_UPDATED, response));
  }

  // ─── Dashboard personnel (ADMIN créateur) ────────────────────────────────

  @GetMapping("/my-dashboard")
  @Operation(
      summary = "Mon tableau de bord admin",
      description =
          "Métriques de chaque tontine gérée par l'admin appelant : cotisations en attente, "
              + "en retard, cycle en cours, montant total validé. "
              + "🔒 Rôle requis : tout utilisateur authentifié ayant créé au moins une tontine.")
  public ResponseEntity<CustomResponse> getMyDashboard(HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    MyDashboardResponse response = adminDashboardService.getMyDashboard(keycloakId);

    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.ADMIN_MY_DASHBOARD_SUCCESS, response));
  }
}
