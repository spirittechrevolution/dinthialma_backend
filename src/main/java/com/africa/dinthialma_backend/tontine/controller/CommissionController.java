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
          "Définit une règle de commission sur une tontine. "
              + "Un seul enregistrement par type est autorisé (POURCENTAGE_JACKPOT, "
              + "FRAIS_FIXES_PAR_CYCLE, FRAIS_ADHESION). "
              + "🔒 Réservé au créateur de la tontine et au SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> createCommission(
      @PathVariable UUID tontineId,
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
          "Retourne toutes les commissions non supprimées de la tontine. "
              + "🔒 Accès : membres, créateur, SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> listCommissions(
      @PathVariable UUID tontineId,
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
          "Met à jour la valeur et/ou la description d'une commission. "
              + "🔒 Réservé au créateur de la tontine et au SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> updateCommission(
      @PathVariable UUID tontineId,
      @PathVariable UUID commissionId,
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
          "Suppression logique de la commission. Elle ne sera plus appliquée aux prochains cycles. "
              + "🔒 Réservé au créateur de la tontine et au SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> deleteCommission(
      @PathVariable UUID tontineId, @PathVariable UUID commissionId, HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    commissionService.deleteCommission(keycloakId, tontineId, commissionId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.COMMISSION_DELETE_SUCCESS, null));
  }
}
