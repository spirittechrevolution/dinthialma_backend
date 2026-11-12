package com.africa.dinthialma_backend.jackpot.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.jackpot.codeList.StatutPriorite;
import com.africa.dinthialma_backend.jackpot.dto.PrioriteJackpotResponse;
import com.africa.dinthialma_backend.jackpot.dto.PrioriteRequests;
import com.africa.dinthialma_backend.jackpot.service.interfaces.PrioriteJackpotService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Priorités de jackpot : un membre dans le besoin reçoit le prochain jackpot en premier. */
@RestController
@RequestMapping("/v1/tontines/{tontineId}/priorites")
@RequiredArgsConstructor
@Tag(
    name = "Priorités de jackpot",
    description =
        "Exception ponctuelle à l'ordre convenu : un membre reçoit le prochain jackpot, puis la"
            + " règle reprend")
@SecurityRequirement(name = "bearerAuth")
public class PrioriteJackpotController {

  private static final String SUCCESS = "success";
  private static final int OK = 200;

  private final PrioriteJackpotService prioriteService;
  private final RequestHeaderParser headerParser;

  @PostMapping
  @Operation(
      summary = "Demander la priorité sur le prochain jackpot",
      description =
          "🔒 Membre ACTIF de la tontine, sans jackpot reçu. Tontine ROTATIVE ACTIVE ou SUSPENDUE."
              + " La demande part EN_ATTENTE et le gestionnaire est notifié"
              + " (`PRIORITE_JACKPOT_DEMANDEE`).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Demande envoyée",
        content = @Content(schema = @Schema(implementation = PrioriteJackpotResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "PRIORITE_NON_DISPONIBLE, MEMBRE_NON_ELIGIBLE_JACKPOT"),
    @ApiResponse(responseCode = "403", description = "Pas membre de la tontine"),
    @ApiResponse(responseCode = "409", description = "PRIORITE_DEJA_ACTIVE"),
  })
  public ResponseEntity<CustomResponse> demander(
      @PathVariable UUID tontineId,
      @RequestBody(required = false) @Valid PrioriteRequests.Demander request,
      HttpServletRequest httpRequest)
      throws CustomException {
    PrioriteJackpotResponse r =
        prioriteService.demander(headerParser.extractKeycloakId(httpRequest), tontineId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CustomResponse(SUCCESS, 201, ResponseMessageConstants.PRIORITE_CREATED, r));
  }

  @PostMapping("/attribuer")
  @Operation(
      summary = "Attribuer directement la priorité à un membre",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN. Pour un accord pris avec le groupe hors"
              + " application. La priorité est ACCEPTEE immédiatement ; le membre et les autres"
              + " membres actifs sont notifiés (`PRIORITE_JACKPOT_ACCORDEE`).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Priorité attribuée",
        content = @Content(schema = @Schema(implementation = PrioriteJackpotResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "PRIORITE_NON_DISPONIBLE, MEMBRE_NON_ELIGIBLE_JACKPOT"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Membre introuvable"),
    @ApiResponse(responseCode = "409", description = "PRIORITE_DEJA_ACTIVE"),
  })
  public ResponseEntity<CustomResponse> attribuer(
      @PathVariable UUID tontineId,
      @RequestBody @Valid PrioriteRequests.Attribuer request,
      HttpServletRequest httpRequest)
      throws CustomException {
    PrioriteJackpotResponse r =
        prioriteService.attribuer(headerParser.extractKeycloakId(httpRequest), tontineId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CustomResponse(SUCCESS, 201, ResponseMessageConstants.PRIORITE_ATTRIBUEE, r));
  }

  @GetMapping
  @Operation(
      summary = "Lister les priorités de la tontine",
      description =
          "🔒 Membres de la tontine, créateur, SUPER_ADMIN. Le `motif` (raison du besoin) n'est"
              + " visible que du gestionnaire et du membre concerné. Filtre `?statut=`.")
  public ResponseEntity<CustomResponse> lister(
      @PathVariable UUID tontineId,
      @Parameter(description = "Filtre de statut (optionnel)") @RequestParam(required = false)
          StatutPriorite statut,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    Page<PrioriteJackpotResponse> page =
        prioriteService.lister(
            headerParser.extractKeycloakId(httpRequest), tontineId, statut, pageable);
    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.PRIORITE_LIST_SUCCESS, page));
  }

  @PutMapping("/{prioriteId}/accepter")
  @Operation(
      summary = "Accepter une demande de priorité",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN. EN_ATTENTE → ACCEPTEE ; le membre et les"
              + " autres membres actifs sont notifiés.")
  public ResponseEntity<CustomResponse> accepter(
      @PathVariable UUID tontineId, @PathVariable UUID prioriteId, HttpServletRequest httpRequest)
      throws CustomException {
    PrioriteJackpotResponse r =
        prioriteService.accepter(
            headerParser.extractKeycloakId(httpRequest), tontineId, prioriteId);
    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.PRIORITE_ACCEPTED, r));
  }

  @PutMapping("/{prioriteId}/refuser")
  @Operation(
      summary = "Refuser une demande de priorité",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN. EN_ATTENTE → REFUSEE, motif optionnel ;"
              + " le membre est notifié (`PRIORITE_JACKPOT_REFUSEE`).")
  public ResponseEntity<CustomResponse> refuser(
      @PathVariable UUID tontineId,
      @PathVariable UUID prioriteId,
      @RequestBody(required = false) @Valid PrioriteRequests.Refuser request,
      HttpServletRequest httpRequest)
      throws CustomException {
    PrioriteJackpotResponse r =
        prioriteService.refuser(
            headerParser.extractKeycloakId(httpRequest), tontineId, prioriteId, request);
    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.PRIORITE_REFUSED, r));
  }

  @DeleteMapping("/{prioriteId}")
  @Operation(
      summary = "Annuler une priorité",
      description =
          "🔒 Créateur de la tontine, SUPER_ADMIN, ou le membre concerné. Priorité EN_ATTENTE ou"
              + " ACCEPTEE → ANNULEE.")
  public ResponseEntity<CustomResponse> annuler(
      @PathVariable UUID tontineId, @PathVariable UUID prioriteId, HttpServletRequest httpRequest)
      throws CustomException {
    PrioriteJackpotResponse r =
        prioriteService.annuler(headerParser.extractKeycloakId(httpRequest), tontineId, prioriteId);
    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.PRIORITE_CANCELLED, r));
  }
}
