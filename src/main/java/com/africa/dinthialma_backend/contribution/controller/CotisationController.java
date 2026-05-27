package com.africa.dinthialma_backend.contribution.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.RecordCotisationRequest;
import com.africa.dinthialma_backend.contribution.service.interfaces.CotisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion des cotisations d'une tontine.
 *
 * <p>Base path : {@code /v1/tontines/{tontineId}/cotisations}
 *
 * <p>Accès :
 *
 * <ul>
 *   <li>Enregistrement → cotisant (MEMBER) de la tontine
 *   <li>Validation → créateur de la tontine, SUPER_ADMIN
 *   <li>Lecture → SUPER_ADMIN et créateur voient tout ; MEMBER voit ses propres cotisations
 * </ul>
 */
@RestController
@RequestMapping("/v1/tontines/{tontineId}/cotisations")
@RequiredArgsConstructor
@Tag(name = "Cotisations", description = "Enregistrement et validation des cotisations (paiements)")
@SecurityRequirement(name = "bearerAuth")
public class CotisationController {

  private static final String SUCCESS = "success";
  private static final int OK = 200;
  private static final int CREATED = 201;

  private final CotisationService cotisationService;
  private final RequestHeaderParser headerParser;

  // ─── Liste ───────────────────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Lister les cotisations",
      description =
          "Liste les cotisations d'une tontine. Paramètre optionnel : cycleId pour filtrer par "
              + "cycle. SUPER_ADMIN et créateur voient tout ; un MEMBER ne voit que les siennes.")
  public ResponseEntity<CustomResponse> listCotisations(
      @PathVariable UUID tontineId,
      @RequestParam(required = false) UUID cycleId,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    List<CotisationResponse> cotisations =
        cotisationService.listCotisations(keycloakId, tontineId, cycleId);

    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.CONTRIBUTION_LIST_SUCCESS, cotisations));
  }

  // ─── Récupération ────────────────────────────────────────────────────────

  @GetMapping("/{cotisationId}")
  @Operation(
      summary = "Récupérer une cotisation",
      description = "Accès : propriétaire de la cotisation, créateur de la tontine, SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> getCotisation(
      @PathVariable UUID tontineId, @PathVariable UUID cotisationId, HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    CotisationResponse response =
        cotisationService.getCotisation(keycloakId, tontineId, cotisationId);

    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.CONTRIBUTION_GET_SUCCESS, response));
  }

  // ─── Enregistrement ──────────────────────────────────────────────────────

  @PostMapping
  @Operation(
      summary = "Enregistrer une cotisation",
      description =
          "Le cotisant signale son paiement. Statut initial : EN_ATTENTE. "
              + "Une seule cotisation par membre par cycle. Réservé aux cotisants de la tontine.")
  public ResponseEntity<CustomResponse> recordCotisation(
      @PathVariable UUID tontineId,
      @RequestBody @Valid RecordCotisationRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    CotisationResponse response =
        cotisationService.recordCotisation(keycloakId, tontineId, request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                SUCCESS, CREATED, ResponseMessageConstants.CONTRIBUTION_RECORD_SUCCESS, response));
  }

  // ─── Validation ──────────────────────────────────────────────────────────

  @PutMapping("/{cotisationId}/valider")
  @Operation(
      summary = "Valider une cotisation",
      description =
          "L'admin confirme le paiement (EN_ATTENTE → VALIDE). Remplit validePar + dateValidation. "
              + "Réservé au créateur de la tontine et au SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> validateCotisation(
      @PathVariable UUID tontineId, @PathVariable UUID cotisationId, HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    CotisationResponse response =
        cotisationService.validateCotisation(keycloakId, tontineId, cotisationId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.CONTRIBUTION_VALIDATED, response));
  }
}
