package com.africa.dinthialma_backend.tontine.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.tontine.dto.CycleResponse;
import com.africa.dinthialma_backend.tontine.dto.OpenCycleRequest;
import com.africa.dinthialma_backend.tontine.service.interfaces.CycleService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion des cycles d'une tontine.
 *
 * <p>Base path : {@code /v1/tontines/{tontineId}/cycles}
 *
 * <p>Accès :
 *
 * <ul>
 *   <li>Lecture → membres, créateur, SUPER_ADMIN
 *   <li>Ouverture / clôture → créateur, SUPER_ADMIN
 * </ul>
 */
@RestController
@RequestMapping("/v1/tontines/{tontineId}/cycles")
@RequiredArgsConstructor
@Tag(name = "Cycles", description = "Gestion des cycles de cotisation d'une tontine")
@SecurityRequirement(name = "bearerAuth")
public class CycleController {

  private static final String SUCCESS = "success";
  private static final int OK = 200;
  private static final int CREATED = 201;

  private final CycleService cycleService;
  private final RequestHeaderParser headerParser;

  // ─── Liste ───────────────────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Lister les cycles d'une tontine",
      description =
          "Retourne tous les cycles triés par numéro croissant. Accès : membres + créateur + SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> listCycles(
      @PathVariable UUID tontineId,
      @PageableDefault(size = 20, sort = "numeroCycle", direction = Sort.Direction.ASC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    Page<CycleResponse> cycles = cycleService.listCycles(keycloakId, tontineId, pageable);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.CYCLE_LIST_SUCCESS, cycles));
  }

  // ─── Récupération ────────────────────────────────────────────────────────

  @GetMapping("/{cycleId}")
  @Operation(
      summary = "Récupérer un cycle",
      description = "Accès : membres + créateur + SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> getCycle(
      @PathVariable UUID tontineId, @PathVariable UUID cycleId, HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    CycleResponse response = cycleService.getCycle(keycloakId, tontineId, cycleId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.CYCLE_GET_SUCCESS, response));
  }

  // ─── Ouverture manuelle ──────────────────────────────────────────────────

  @PostMapping
  @Operation(
      summary = "Ouvrir un cycle manuellement",
      description =
          "Mode MANUEL uniquement. Ouvre un nouveau cycle avec les dates et bénéficiaire spécifiés. "
              + "Réservé au créateur et au SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> openCycle(
      @PathVariable UUID tontineId,
      @RequestBody @Valid OpenCycleRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    CycleResponse response = cycleService.openCycle(keycloakId, tontineId, request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(SUCCESS, CREATED, ResponseMessageConstants.CYCLE_OPENED, response));
  }

  // ─── Clôture ─────────────────────────────────────────────────────────────

  @PutMapping("/{cycleId}/cloturer")
  @Operation(
      summary = "Clôturer un cycle",
      description =
          "Passe le cycle EN_COURS à TERMINE. Calcule le jackpot (cotisations VALIDEES), "
              + "marque les cotisations EN_ATTENTE comme EN_RETARD, et active le cycle suivant "
              + "en mode AUTOMATIQUE. Réservé au créateur et au SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> closeCycle(
      @PathVariable UUID tontineId, @PathVariable UUID cycleId, HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    CycleResponse response = cycleService.closeCycle(keycloakId, tontineId, cycleId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.CYCLE_CLOSED, response));
  }
}
