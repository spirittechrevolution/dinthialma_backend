package com.africa.dinthialma_backend.contribution.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.contribution.dto.AdminRecordCotisationRequest;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.CycleRecapCotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.RecordCotisationRequest;
import com.africa.dinthialma_backend.contribution.dto.UpdateCotisationRequest;
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
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
              + "Paramètres optionnels :\n"
              + "- `cycleId` : filtre par cycle\n"
              + "- `membreId` : filtre par membre (admin/SUPER_ADMIN uniquement, ignoré pour MEMBER)\n\n"
              + "Pagination : `?page=0&size=20&sort=createdAt,desc&cycleId=<uuid>&membreId=<uuid>`")
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
      @Parameter(
              description =
                  "Filtrer par membre (optionnel, admin/SUPER_ADMIN uniquement — ignoré pour MEMBER)",
              example = "770e8400-e29b-41d4-a716-446655440000")
          @RequestParam(required = false)
          UUID membreId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    Page<CotisationResponse> cotisations =
        cotisationService.listCotisations(keycloakId, tontineId, cycleId, membreId, pageable);

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

  // ─── Enregistrement admin (cash / PRE_ENROLLED) ──────────────────────────

  @PostMapping("/admin")
  @Operation(
      summary = "Enregistrer et valider une cotisation (admin)",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Permet à l'admin d'enregistrer un paiement reçu directement (cash ou mobile money)"
              + " pour n'importe quel membre de la tontine, y compris les membres **PRE_ENROLLED**"
              + " qui n'ont pas encore de compte.\n\n"
              + "La cotisation est créée **directement en statut VALIDÉ** – pas de passage par"
              + " EN_ATTENTE. Les champs `enregistrePar`, `validePar` et `dateValidation` sont"
              + " remplis automatiquement avec les informations de l'admin appelant.\n\n"
              + "Une notification WhatsApp est envoyée au membre à l'issue de l'enregistrement.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Cotisation enregistrée et validée",
        content = @Content(schema = @Schema(implementation = CotisationResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides ou cycle pas EN_COURS"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(
        responseCode = "403",
        description = "Accès interdit (pas créateur ni SUPER_ADMIN)"),
    @ApiResponse(responseCode = "404", description = "Tontine, cycle ou membre introuvable"),
    @ApiResponse(responseCode = "409", description = "Cotisation déjà enregistrée pour ce membre"),
  })
  public ResponseEntity<CustomResponse> adminRecordCotisation(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @RequestBody @Valid AdminRecordCotisationRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    CotisationResponse response =
        cotisationService.adminRecordCotisation(keycloakId, tontineId, request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                SUCCESS,
                CREATED,
                ResponseMessageConstants.CONTRIBUTION_ADMIN_RECORD_SUCCESS,
                response));
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

  // ─── Modification ─────────────────────────────────────────────────────────

  @PatchMapping("/{cotisationId}")
  @Operation(
      summary = "Modifier une cotisation (admin)",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Corrige les champs d'une cotisation en cas d'erreur de saisie."
              + " Sémantique PATCH : seuls les champs non-null sont mis à jour.\n\n"
              + "**Conditions d'édition :**\n"
              + "- Statut `EN_ATTENTE` → toujours modifiable\n"
              + "- Statut `VALIDE` + cycle `EN_COURS` → modifiable, reste VALIDE\n"
              + "- Statut `VALIDE` + cycle `TERMINE` → refusé (jackpot calculé)\n"
              + "- Statut `EN_RETARD` → refusé (cycle clôturé)\n\n"
              + "Si le montant est modifié, les règles de la tontine sont re-validées"
              + " (montant fixe / montant minimum).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Cotisation mise à jour",
        content = @Content(schema = @Schema(implementation = CotisationResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description =
            "Modification interdite (cycle clôturé, statut EN_RETARD ou montant invalide)"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine ou cotisation introuvable"),
  })
  public ResponseEntity<CustomResponse> updateCotisation(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @Parameter(
              description = "UUID de la cotisation à modifier",
              example = "880e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID cotisationId,
      @RequestBody @Valid UpdateCotisationRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    CotisationResponse response =
        cotisationService.updateCotisation(keycloakId, tontineId, cotisationId, request);

    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.CONTRIBUTION_UPDATE_SUCCESS, response));
  }

  // ─── Récapitulatif par membre ─────────────────────────────────────────────

  @GetMapping("/recap/{cycleId}")
  @Operation(
      summary = "Récapitulatif des cotisations d'un cycle par membre",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Retourne un enregistrement par membre actif/suspendu de la tontine :\n"
              + "- `statutCotisation` **null** → membre n'a soumis aucune cotisation pour ce cycle\n"
              + "- `statutCotisation` **EN_ATTENTE** → cotisation soumise, en attente de validation\n"
              + "- `statutCotisation` **VALIDE** → cotisation validée et prise en compte\n"
              + "- `statutCotisation` **EN_RETARD** → cycle clôturé sans cotisation validée\n\n"
              + "Les membres sont triés par `ordreJackpot` croissant puis par date d'adhésion."
              + " Les membres SORTI sont exclus.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Récapitulatif par membre",
        content = @Content(schema = @Schema(implementation = CycleRecapCotisationResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine ou cycle introuvable"),
  })
  public ResponseEntity<CustomResponse> getCycleRecap(
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
    List<CycleRecapCotisationResponse> recap =
        cotisationService.getCycleRecap(keycloakId, tontineId, cycleId);

    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.CONTRIBUTION_RECAP_SUCCESS, recap));
  }
}
