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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
          "🔒 Rôle requis : **SUPER_ADMIN**.\n\n"
              + "Métriques globales en temps réel :\n\n"
              + "- **Utilisateurs** : total, actifs, désactivés, nouveaux ce mois\n"
              + "- **Tontines** : total, par statut (BROUILLON, ACTIVE, SUSPENDUE, TERMINEE)\n"
              + "- **Finances** : cotisations EN_ATTENTE, EN_RETARD, montant validé ce mois\n"
              + "- **Activité 24h** : nouveaux inscrits, cotisations enregistrées")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Métriques globales",
        content = @Content(schema = @Schema(implementation = GlobalDashboardResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit – SUPER_ADMIN requis"),
  })
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
          "🔒 Rôle requis : **SUPER_ADMIN**.\n\n"
              + "Retourne tous les utilisateurs non supprimés de la plateforme.\n\n"
              + "Pagination : `?page=0&size=20&sort=createdAt,desc`")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Page d'utilisateurs",
        content = @Content(schema = @Schema(implementation = AdminUserResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit – SUPER_ADMIN requis"),
  })
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
  @Operation(
      summary = "Détail d'un utilisateur",
      description =
          "🔒 Rôle requis : **SUPER_ADMIN**.\n\nRetourne le profil complet d'un utilisateur.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Détail utilisateur",
        content = @Content(schema = @Schema(implementation = AdminUserResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit – SUPER_ADMIN requis"),
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
  })
  public ResponseEntity<CustomResponse> getUserDetail(
      @Parameter(
              description = "UUID de l'utilisateur",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID userId,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    AdminUserResponse response = adminDashboardService.getUserDetail(keycloakId, userId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.ADMIN_USER_GET_SUCCESS, response));
  }

  @PostMapping("/dashboard/users/{userId}/disable")
  @Operation(
      summary = "Désactiver un compte utilisateur",
      description =
          "🔒 Rôle requis : **SUPER_ADMIN**.\n\n"
              + "Désactive le compte (active = false) et le désactive également dans Keycloak."
              + " L'utilisateur ne peut plus se connecter.\n\n"
              + "Un SUPER_ADMIN ne peut pas se désactiver lui-même.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Compte désactivé",
        content = @Content(schema = @Schema(implementation = AdminUserResponse.class))),
    @ApiResponse(responseCode = "400", description = "Impossible de se désactiver soi-même"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit – SUPER_ADMIN requis"),
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
  })
  public ResponseEntity<CustomResponse> disableUser(
      @Parameter(
              description = "UUID de l'utilisateur à désactiver",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID userId,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    AdminUserResponse response = adminDashboardService.disableUser(keycloakId, userId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.ADMIN_USER_DISABLED, response));
  }

  @PostMapping("/dashboard/users/{userId}/enable")
  @Operation(
      summary = "Réactiver un compte utilisateur",
      description =
          "🔒 Rôle requis : **SUPER_ADMIN**.\n\n"
              + "Réactive un compte désactivé (active = true) et le réactive dans Keycloak.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Compte réactivé",
        content = @Content(schema = @Schema(implementation = AdminUserResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit – SUPER_ADMIN requis"),
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
  })
  public ResponseEntity<CustomResponse> enableUser(
      @Parameter(
              description = "UUID de l'utilisateur à réactiver",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID userId,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    AdminUserResponse response = adminDashboardService.enableUser(keycloakId, userId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.ADMIN_USER_ENABLED, response));
  }

  @PutMapping("/dashboard/users/{userId}/roles")
  @Operation(
      summary = "Modifier les rôles d'un utilisateur",
      description =
          "🔒 Rôle requis : **SUPER_ADMIN**.\n\n"
              + "**Replace-all idempotent** : les rôles fournis remplacent complètement les rôles"
              + " existants.\n\n"
              + "Règles :\n"
              + "- Le rôle USER est toujours conservé même s'il n'est pas dans la liste\n"
              + "- Les rôles Keycloak sont synchronisés simultanément\n\n"
              + "Exemple : `{\"roles\": [\"USER\", \"ADMIN\"]}` → attribue ADMIN, retire MEMBER si"
              + " présent")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Rôles mis à jour",
        content = @Content(schema = @Schema(implementation = AdminUserResponse.class))),
    @ApiResponse(responseCode = "400", description = "Rôles invalides"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit – SUPER_ADMIN requis"),
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
  })
  public ResponseEntity<CustomResponse> updateUserRoles(
      @Parameter(
              description = "UUID de l'utilisateur",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID userId,
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
          "🔒 Rôles : tout utilisateur authentifié ayant créé au moins une tontine.\n\n"
              + "Métriques personnelles pour chaque tontine gérée par l'appelant :\n\n"
              + "- Cotisations EN_ATTENTE et EN_RETARD\n"
              + "- Montant total validé\n"
              + "- Cycle EN_COURS avec bénéficiaire désigné")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Tableau de bord de l'admin",
        content = @Content(schema = @Schema(implementation = MyDashboardResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
  })
  public ResponseEntity<CustomResponse> getMyDashboard(HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    MyDashboardResponse response = adminDashboardService.getMyDashboard(keycloakId);

    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.ADMIN_MY_DASHBOARD_SUCCESS, response));
  }
}
