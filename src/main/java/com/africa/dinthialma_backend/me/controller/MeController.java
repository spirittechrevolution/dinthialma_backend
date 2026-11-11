package com.africa.dinthialma_backend.me.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.me.dto.MeSummaryResponse;
import com.africa.dinthialma_backend.me.dto.ValidationGroupeeRequest;
import com.africa.dinthialma_backend.me.dto.ValidationGroupeeResponse;
import com.africa.dinthialma_backend.me.service.interfaces.MeService;
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
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Espace personnel de l'utilisateur connecté : vues agrégées sur toutes ses tontines.
 *
 * <p>Base path : {@code /v1/me}. Tout utilisateur authentifié ; les données sont toujours
 * restreintes à l'appelant.
 */
@RestController
@RequestMapping("/v1/me")
@RequiredArgsConstructor
@Tag(name = "Mon espace", description = "Vues transverses de l'utilisateur connecté (/v1/me)")
@SecurityRequirement(name = "bearerAuth")
public class MeController {

  private static final String SUCCESS = "success";
  private static final int OK = 200;

  private final MeService meService;
  private final RequestHeaderParser headerParser;

  // ─── Je cotise : résumé toutes tontines ──────────────────────────────────

  @GetMapping("/summary")
  @Operation(
      summary = "Résumé « Je cotise » toutes tontines",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\n"
              + "Agrège en un seul appel, pour chaque tontine où l'appelant est **inscrit comme"
              + " cotisant** : mon membership (`monMembreId`, statut, ordre, `aRecuJackpot`), mes"
              + " totaux VALIDÉ / EN_ATTENTE, tours payés / générés, le tour EN_COURS, la"
              + " cotisation due (`A_PAYER` / `EN_ATTENTE` / `VALIDE`) et le gestionnaire.\n\n"
              + "Racine : `totalCotiseValide`, `totalEnAttente`, `totalJackpotsRecus`,"
              + " `nombreCotisationsDues`, `montantDu`.\n\n"
              + "`cotisationDue` vaut null si la tontine n'est pas ACTIVE, s'il n'y a pas de tour"
              + " en cours ou si mon membership n'est pas ACTIF. Liste non paginée (triée par date"
              + " d'adhésion décroissante).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Résumé cotisant",
        content = @Content(schema = @Schema(implementation = MeSummaryResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
  })
  public ResponseEntity<CustomResponse> getSummary(HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    MeSummaryResponse summary = meService.getSummary(keycloakId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.ME_SUMMARY_SUCCESS, summary));
  }

  // ─── Je gère : validation groupée ────────────────────────────────────────

  @PostMapping("/gestion/cotisations/valider")
  @Operation(
      summary = "Valider plusieurs cotisations en une fois (« Tout valider »)",
      description =
          "🔒 Rôles : tout utilisateur authentifié — chaque cotisation doit appartenir à une"
              + " tontine dont l'appelant est le **créateur** (ou être SUPER_ADMIN).\n\n"
              + "Corps : `{ \"cotisationIds\": [...] }` (1 à 100, doublons ignorés, plusieurs"
              + " tontines possibles).\n\n"
              + "Chaque cotisation est validée **indépendamment** (EN_ATTENTE → VALIDE, mêmes"
              + " notifications qu'une validation unitaire) : un échec n'annule pas les autres."
              + " La réponse est toujours 200 et détaille le résultat par identifiant, avec un"
              + " `code` stable en cas d'échec (`COTISATION_NOT_PENDING`, `FORBIDDEN`,"
              + " `NOT_FOUND`…).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Résultat par cotisation",
        content = @Content(schema = @Schema(implementation = ValidationGroupeeResponse.class))),
    @ApiResponse(responseCode = "400", description = "Liste vide ou > 100 (VALIDATION_ERROR)"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
  })
  public ResponseEntity<CustomResponse> validerCotisations(
      @RequestBody @Valid ValidationGroupeeRequest request, HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    ValidationGroupeeResponse response = meService.validerCotisations(keycloakId, request);

    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.ME_VALIDATION_GROUPEE_SUCCESS, response));
  }

  // ─── Je gère : paiements de mes tontines ─────────────────────────────────

  @GetMapping("/gestion/cotisations")
  @Operation(
      summary = "Paiements de toutes mes tontines gérées (file « À valider »)",
      description =
          "🔒 Rôles : tout utilisateur authentifié — ne renvoie que les tontines dont l'appelant"
              + " est le **créateur**.\n\n"
              + "Paramètres optionnels :\n"
              + "- `statut` : un ou plusieurs statuts (`?statut=EN_ATTENTE` pour la file"
              + " « À valider »), absent = tous\n"
              + "- `tontineId` : restreint à une tontine gérée (une tontine non gérée renvoie une"
              + " page vide)\n"
              + "- `q` : recherche sur le prénom, le nom, le téléphone du cotisant ou la référence"
              + " de transaction\n\n"
              + "Chaque paiement porte `tontineId`, `tontineNom`, `cycleId` et `numeroCycle`.\n\n"
              + "Pagination : `?page=0&size=20&sort=createdAt,desc` (utiliser"
              + " `sort=createdAt,asc` pour traiter les plus anciens d'abord)")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Page de cotisations",
        content = @Content(schema = @Schema(implementation = CotisationResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
  })
  public ResponseEntity<CustomResponse> listCotisationsGerees(
      @Parameter(description = "Filtrer par statut (multi-valeurs, optionnel)")
          @RequestParam(required = false)
          List<CotisationStatut> statut,
      @Parameter(description = "Restreindre à une tontine gérée (optionnel)")
          @RequestParam(required = false)
          UUID tontineId,
      @Parameter(description = "Recherche : nom, téléphone du cotisant ou référence de transaction")
          @RequestParam(required = false)
          String q,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    Page<CotisationResponse> cotisations =
        meService.listCotisationsGerees(keycloakId, statut, tontineId, q, pageable);

    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.ME_GESTION_COTISATIONS_SUCCESS, cotisations));
  }
}
