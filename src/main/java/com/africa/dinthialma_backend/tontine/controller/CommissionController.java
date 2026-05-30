package com.africa.dinthialma_backend.tontine.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.tontine.dto.CommissionResponse;
import com.africa.dinthialma_backend.tontine.dto.CreateCommissionRequest;
import com.africa.dinthialma_backend.tontine.dto.UpdateCommissionRequest;
import com.africa.dinthialma_backend.tontine.service.interfaces.CommissionService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion des commissions d'une tontine.
 *
 * <p>Accès :
 *
 * <ul>
 *   <li>Lecture → membres de la tontine, créateur, SUPER_ADMIN
 *   <li>Création / modification / suppression → créateur ou SUPER_ADMIN
 * </ul>
 */
@RestController
@RequestMapping("/v1/tontines/{tontineId}/commissions")
@RequiredArgsConstructor
@Tag(name = "Commissions", description = "Gestion des commissions du gestionnaire par tontine")
@SecurityRequirement(name = "bearerAuth")
public class CommissionController {

  private static final String SUCCESS = "success";
  private static final int OK = 200;
  private static final int CREATED = 201;

  private final CommissionService commissionService;
  private final RequestHeaderParser headerParser;

  // ─── Création ────────────────────────────────────────────────────────────

  @PostMapping
  @Operation(
      summary = "Créer une commission",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Définit une règle de commission appliquée automatiquement à chaque clôture de"
              + " cycle.\n\n"
              + "**Types disponibles :**\n"
              + "- `POURCENTAGE_JACKPOT` : ex. 4 % → 20 000 FCFA prélevés sur un jackpot de 500 000"
              + " FCFA\n"
              + "- `FRAIS_FIXES_PAR_CYCLE` : montant fixe FCFA déduit à chaque clôture\n"
              + "- `FRAIS_ADHESION` : prélevé à l'entrée du membre, **ne réduit pas le jackpot**\n\n"
              + "**Contrainte** : maximum 1 commission par type (erreur 409 si doublon).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Commission créée",
        content = @Content(schema = @Schema(implementation = CommissionResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine introuvable"),
    @ApiResponse(responseCode = "409", description = "Commission de ce type déjà configurée"),
  })
  public ResponseEntity<CustomResponse> createCommission(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @RequestBody @Valid CreateCommissionRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    CommissionResponse response =
        commissionService.createCommission(keycloakId, tontineId, request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                SUCCESS, CREATED, ResponseMessageConstants.COMMISSION_CREATE_SUCCESS, response));
  }

  // ─── Liste ───────────────────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Lister les commissions actives",
      description =
          "🔒 Rôles : membres de la tontine, créateur, SUPER_ADMIN.\n\n"
              + "Retourne toutes les commissions non supprimées de la tontine."
              + " Les membres peuvent ainsi voir les frais de gestion appliqués.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Page de commissions",
        content = @Content(schema = @Schema(implementation = CommissionResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine introuvable"),
  })
  public ResponseEntity<CustomResponse> listCommissions(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    Page<CommissionResponse> commissions =
        commissionService.listCommissions(keycloakId, tontineId, pageable);

    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.COMMISSION_LIST_SUCCESS, commissions));
  }

  // ─── Mise à jour ─────────────────────────────────────────────────────────

  @PutMapping("/{commissionId}")
  @Operation(
      summary = "Modifier une commission",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Met à jour la valeur et/ou la description d'une commission existante."
              + " Les champs null ne sont pas modifiés.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Commission mise à jour",
        content = @Content(schema = @Schema(implementation = CommissionResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine ou commission introuvable"),
  })
  public ResponseEntity<CustomResponse> updateCommission(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @Parameter(
              description = "UUID de la commission",
              example = "990e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID commissionId,
      @RequestBody @Valid UpdateCommissionRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    CommissionResponse response =
        commissionService.updateCommission(keycloakId, tontineId, commissionId, request);

    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.COMMISSION_UPDATE_SUCCESS, response));
  }

  // ─── Suppression ─────────────────────────────────────────────────────────

  @DeleteMapping("/{commissionId}")
  @Operation(
      summary = "Supprimer une commission (soft delete)",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Suppression logique. La commission ne sera plus appliquée aux prochains cycles."
              + " Le même type peut être recréé ultérieurement.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Commission supprimée"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine ou commission introuvable"),
  })
  public ResponseEntity<CustomResponse> deleteCommission(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @Parameter(
              description = "UUID de la commission",
              example = "990e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID commissionId,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    commissionService.deleteCommission(keycloakId, tontineId, commissionId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.COMMISSION_DELETE_SUCCESS, null));
  }
}
