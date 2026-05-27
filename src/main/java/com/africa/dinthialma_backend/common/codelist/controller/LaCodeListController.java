package com.africa.dinthialma_backend.common.codelist.controller;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.common.codelist.dto.LaCodeListDto;
import com.africa.dinthialma_backend.common.codelist.service.interfaces.LaCodeListService;
import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.common.util.RoleGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST des référentiels de valeurs (code lists).
 *
 * <p><b>Endpoints publics</b> (aucun token requis) :
 *
 * <ul>
 *   <li>{@code GET /v1/code-list/type/{type}} – valeurs d'un type pour peupler les selects frontend
 * </ul>
 *
 * <p><b>Endpoints authentifiés</b> (tout utilisateur connecté) :
 *
 * <ul>
 *   <li>{@code GET /v1/code-list} – liste paginée de tous les code lists
 *   <li>{@code GET /v1/code-list/{id}} – détail d'un code list
 * </ul>
 *
 * <p><b>Endpoints admin</b> (rôle {@code ADMIN} ou {@code SUPER_ADMIN} requis) :
 *
 * <ul>
 *   <li>{@code GET /v1/code-list/admin/type/{type}} – accès aux types restreints
 *   <li>{@code POST /v1/code-list} – création
 *   <li>{@code PUT /v1/code-list/{id}} – mise à jour
 * </ul>
 */
@RestController
@RequestMapping("/v1/code-list")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CodeList", description = "Référentiels de valeurs persistées (selects frontend)")
public class LaCodeListController {

  /** Types restreints non accessibles via l'endpoint public. */
  private static final String RESTRICTED_TYPE = "ROLE_INTERNE";

  private final LaCodeListService codeListService;
  private final RequestHeaderParser requestHeaderParser;

  // ── Public ──────────────────────────────────────────────────────

  /**
   * Retourne toutes les valeurs d'un type. Endpoint principal pour peupler les selects frontend.
   * Certains types internes sont restreints (utiliser {@code /admin/type/}).
   *
   * <p>Exemples de types disponibles :
   *
   * <ul>
   *   <li>{@code FREQUENCE_TONTINE} → {@code HEBDOMADAIRE}, {@code MENSUEL}, {@code BIMENSUEL}
   *   <li>{@code METHODE_PAIEMENT} → {@code WAVE}, {@code ORANGE_MONEY}, {@code FREE_MONEY}, {@code
   *       CASH}
   *   <li>{@code STATUT_COTISATION} → {@code EN_ATTENTE}, {@code VALIDE}, {@code EN_RETARD}
   * </ul>
   */
  @GetMapping("/type/{type}")
  @Operation(
      summary = "Valeurs d'un type de code list (public)",
      description =
          "🌐 **Public** — Retourne toutes les entrées d'un type donné. Aucun token requis.\n\n"
              + "---\n\n"
              + "### Types disponibles\n\n"
              + "| Type | Valeurs |\n"
              + "|------|---------|\n"
              + "| `FREQUENCE_TONTINE` | `HEBDOMADAIRE` · `BIMENSUEL` · `MENSUEL` · `TRIMESTRIEL` |\n"
              + "| `METHODE_PAIEMENT` | `WAVE` · `ORANGE_MONEY` · `FREE_MONEY` · `CASH` |\n"
              + "| `STATUT_COTISATION` | `EN_ATTENTE` · `VALIDE` · `EN_RETARD` |\n"
              + "| `ORDRE_BENEFICIAIRE` | `ALEATOIRE` · `MANUEL` · `ROTATION` |\n\n"
              + "> ⚠️ **`ROLE_INTERNE`** est restreint — utiliser `GET /v1/code-list/admin/type/ROLE_INTERNE`.",
      tags = {"CodeList"})
  @ApiResponse(responseCode = "200", description = "Liste récupérée")
  @ApiResponse(responseCode = "403", description = "Type restreint – utiliser /admin/type/")
  public ResponseEntity<CustomResponse> findAllByType(@PathVariable String type)
      throws CustomException {
    if (RESTRICTED_TYPE.equals(type)) {
      throw new ForbiddenException(
          "Le type '"
              + type
              + "' est restreint. Utiliser GET /v1/code-list/admin/type/"
              + type
              + " (ADMIN requis).");
    }
    log.info("Code lists par type : {}", type);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CODELIST_GET_SUCCESS,
            codeListService.findAllByType(type).stream().map(LaCodeListDto::from).toList()));
  }

  /**
   * Retourne les valeurs d'un type – accès admin. Permet d'accéder aux types restreints non exposés
   * publiquement (ex : {@code ROLE_INTERNE}).
   */
  @GetMapping("/admin/type/{type}")
  @Operation(
      summary = "Valeurs d'un type (admin)",
      description =
          "🛡 **Rôle requis :** `ADMIN` ou `SUPER_ADMIN`.\n\n"
              + "Identique à l'endpoint public mais donne accès aux types restreints.",
      tags = {"CodeList"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Liste récupérée")
  @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
  @ApiResponse(responseCode = "403", description = "Rôle insuffisant")
  public ResponseEntity<CustomResponse> findAllByTypeAdmin(
      @PathVariable String type, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    log.info("Code lists par type (admin) : {}", type);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CODELIST_GET_SUCCESS,
            codeListService.findAllByType(type).stream().map(LaCodeListDto::from).toList()));
  }

  // ── Authentifié ─────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Liste paginée de tous les code lists",
      description = "Accessible à tout utilisateur authentifié. Supporte pagination et tri.",
      tags = {"CodeList"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Liste récupérée")
  @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
  public ResponseEntity<CustomResponse> findAll(
      @ParameterObject @PageableDefault(size = 25, sort = "type", direction = Sort.Direction.ASC)
          Pageable pageable) {
    log.info("Liste paginée des code lists");
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CODELIST_GET_SUCCESS,
            codeListService.findAll(pageable).map(LaCodeListDto::from)));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Détail d'un code list par id",
      tags = {"CodeList"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Code list trouvé",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = LaCodeListDto.class)))
  @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
  @ApiResponse(responseCode = "404", description = "Code list introuvable")
  public ResponseEntity<CustomResponse> findById(@PathVariable UUID id) throws CustomException {
    log.info("Détail code list id : {}", id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CODELIST_GET_SUCCESS,
            LaCodeListDto.from(codeListService.findById(id))));
  }

  // ── Admin ────────────────────────────────────────────────────────

  @PostMapping
  @Operation(
      summary = "Créer un code list (admin)",
      description =
          "Crée une nouvelle valeur de référentiel. Le couple type/value doit être unique.",
      tags = {"CodeList"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "201",
      description = "Code list créé",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = LaCodeListDto.class)))
  @ApiResponse(responseCode = "400", description = "Données invalides")
  @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
  @ApiResponse(responseCode = "403", description = "Rôle insuffisant")
  @ApiResponse(responseCode = "409", description = "Ce couple type/value existe déjà")
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid LaCodeListDto dto, HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    log.info("Création code list type={} value={}", dto.getType(), dto.getValue());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.CODELIST_POST_SUCCESS,
                LaCodeListDto.from(codeListService.createCodeList(dto.toEntity()))));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Modifier un code list (admin)",
      description = "Met à jour un code list existant.",
      tags = {"CodeList"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Code list mis à jour")
  @ApiResponse(responseCode = "400", description = "Données invalides")
  @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
  @ApiResponse(responseCode = "403", description = "Rôle insuffisant")
  @ApiResponse(responseCode = "404", description = "Code list introuvable")
  @ApiResponse(responseCode = "409", description = "Ce couple type/value existe déjà")
  public ResponseEntity<CustomResponse> update(
      @PathVariable UUID id, @RequestBody @Valid LaCodeListDto dto, HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    log.info("Mise à jour code list id : {}", id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CODELIST_PUT_SUCCESS,
            LaCodeListDto.from(codeListService.updateCodeList(dto.toEntity(), id))));
  }
}
