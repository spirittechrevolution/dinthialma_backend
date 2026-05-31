package com.africa.dinthialma_backend.contribution.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.RecordCotisationRequest;
import com.africa.dinthialma_backend.contribution.service.interfaces.CotisationService;
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
          "🔒 Rôles : membres, créateur, SUPER_ADMIN.\n\n"
              + "- **SUPER_ADMIN** et **créateur** : voient toutes les cotisations de la tontine.\n"
              + "- **MEMBER** : voit uniquement ses propres cotisations.\n\n"
              + "Paramètre optionnel `cycleId` pour filtrer par cycle.\n\n"
              + "Pagination : `?page=0&size=20&sort=createdAt,desc&cycleId=<uuid>`")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Page de cotisations",
        content = @Content(schema = @Schema(implementation = CotisationResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine introuvable"),
  })
  public ResponseEntity<CustomResponse> listCotisations(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @Parameter(
              description = "Filtrer par cycle (optionnel)",
              example = "660e8400-e29b-41d4-a716-446655440000")
          @RequestParam(required = false)
          UUID cycleId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    Page<CotisationResponse> cotisations =
        cotisationService.listCotisations(keycloakId, tontineId, cycleId, pageable);

    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.CONTRIBUTION_LIST_SUCCESS, cotisations));
  }

  // ─── Récupération ────────────────────────────────────────────────────────

  @GetMapping("/{cotisationId}")
  @Operation(
      summary = "Récupérer une cotisation",
      description =
          "🔒 Rôles : propriétaire de la cotisation (MEMBER), créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Retourne le détail complet : montant, méthode de paiement, référence,"
              + " statut, valideur et date de validation.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Détail de la cotisation",
        content = @Content(schema = @Schema(implementation = CotisationResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine ou cotisation introuvable"),
  })
  public ResponseEntity<CustomResponse> getCotisation(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @Parameter(
              description = "UUID de la cotisation",
              example = "880e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID cotisationId,
      HttpServletRequest httpRequest)
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
          "🔒 Rôles : cotisant (MEMBER) de la tontine.\n\n"
              + "Le membre signale son paiement. La cotisation est créée en statut **EN_ATTENTE**"
              + " – l'admin devra la valider.\n\n"
              + "Contrainte : **une seule cotisation par membre par cycle** – une 2ème tentative"
              + " retourne une erreur 409.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Cotisation enregistrée en statut EN_ATTENTE",
        content = @Content(schema = @Schema(implementation = CotisationResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides ou cycle pas EN_COURS"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit (pas cotisant de la tontine)"),
    @ApiResponse(responseCode = "404", description = "Tontine ou cycle introuvable"),
    @ApiResponse(responseCode = "409", description = "Cotisation déjà enregistrée pour ce cycle"),
  })
  public ResponseEntity<CustomResponse> recordCotisation(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
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
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "L'admin confirme la réception du paiement : statut **EN_ATTENTE → VALIDÉ**.\n\n"
              + "Remplit automatiquement :\n"
              + "- `validePar` : l'admin appelant\n"
              + "- `dateValidation` : date/heure courante\n\n"
              + "Une cotisation VALIDÉE est prise en compte dans le jackpot à la clôture du cycle.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Cotisation validée",
        content = @Content(schema = @Schema(implementation = CotisationResponse.class))),
    @ApiResponse(responseCode = "400", description = "Cotisation pas en statut EN_ATTENTE"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine ou cotisation introuvable"),
  })
  public ResponseEntity<CustomResponse> validateCotisation(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @Parameter(
              description = "UUID de la cotisation à valider",
              example = "880e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID cotisationId,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    CotisationResponse response =
        cotisationService.validateCotisation(keycloakId, tontineId, cotisationId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.CONTRIBUTION_VALIDATED, response));
  }
}
