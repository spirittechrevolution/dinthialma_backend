package com.africa.dinthialma_backend.tontine.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.tontine.dto.CycleResponse;
import com.africa.dinthialma_backend.tontine.dto.OpenCycleRequest;
import com.africa.dinthialma_backend.tontine.service.interfaces.CycleService;
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
          "🔒 Rôles : membres de la tontine, créateur, SUPER_ADMIN.\n\n"
              + "Retourne tous les cycles triés par numéro croissant (1, 2, 3, ...).\n\n"
              + "Statuts possibles : EN_ATTENTE (généré mais pas encore démarré),"
              + " EN_COURS (actif), TERMINE (clôturé).\n\n"
              + "Pagination : `?page=0&size=20&sort=numeroCycle,asc`")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Page de cycles",
        content = @Content(schema = @Schema(implementation = CycleResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine introuvable"),
  })
  public ResponseEntity<CustomResponse> listCycles(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
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
      description =
          "🔒 Rôles : membres de la tontine, créateur, SUPER_ADMIN.\n\n"
              + "Retourne le détail complet d'un cycle : dates, statut, bénéficiaire,"
              + " montant jackpot brut, commissions et montant net (renseignés à la clôture).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Détail du cycle",
        content = @Content(schema = @Schema(implementation = CycleResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine ou cycle introuvable"),
  })
  public ResponseEntity<CustomResponse> getCycle(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @Parameter(description = "UUID du cycle", example = "660e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID cycleId,
      HttpServletRequest httpRequest)
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
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "**Mode MANUEL uniquement.** Crée et ouvre un nouveau cycle avec les dates et le"
              + " bénéficiaire spécifiés.\n\n"
              + "Conditions : tontine ACTIVE, aucun autre cycle EN_COURS, dateDebut ≤ dateFin.\n\n"
              + "Si `beneficiaireId` est fourni, un SMS d'annonce est envoyé au bénéficiaire.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Cycle ouvert",
        content = @Content(schema = @Schema(implementation = CycleResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Mode pas MANUEL, tontine pas ACTIVE, ou cycle déjà EN_COURS"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine ou bénéficiaire introuvable"),
  })
  public ResponseEntity<CustomResponse> openCycle(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
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
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Passe le cycle **EN_COURS → TERMINÉ** avec le traitement automatique suivant :\n\n"
              + "1. Calcul du jackpot brut = somme des cotisations **VALIDÉES** du cycle\n"
              + "2. Calcul des commissions actives (POURCENTAGE_JACKPOT, FRAIS_FIXES_PAR_CYCLE)\n"
              + "3. Stockage de montantJackpot, montantCommission, montantNet\n"
              + "4. Cotisations **EN_ATTENTE** restantes → marquées **EN_RETARD**\n"
              + "5. En mode AUTOMATIQUE : le cycle suivant (EN_ATTENTE) passe **EN_COURS** et un"
              + " SMS est envoyé au bénéficiaire suivant")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Cycle clôturé avec jackpot calculé",
        content = @Content(schema = @Schema(implementation = CycleResponse.class))),
    @ApiResponse(responseCode = "400", description = "Cycle pas EN_COURS"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine ou cycle introuvable"),
  })
  public ResponseEntity<CustomResponse> closeCycle(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @Parameter(
              description = "UUID du cycle à clôturer",
              example = "660e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID cycleId,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    CycleResponse response = cycleService.closeCycle(keycloakId, tontineId, cycleId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.CYCLE_CLOSED, response));
  }
}
