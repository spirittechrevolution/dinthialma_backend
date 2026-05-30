package com.africa.dinthialma_backend.member.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.member.dto.AddMembreRequest;
import com.africa.dinthialma_backend.member.dto.MembreResponse;
import com.africa.dinthialma_backend.member.dto.UpdateMembreStatutRequest;
import com.africa.dinthialma_backend.member.service.interfaces.MembreService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion des cotisants d'une tontine.
 *
 * <p>Base path : {@code /v1/tontines/{tontineId}/membres}
 *
 * <p>Accès :
 *
 * <ul>
 *   <li>Lecture → membres, créateur, SUPER_ADMIN
 *   <li>Ajout / retrait / modification statut → créateur, SUPER_ADMIN
 * </ul>
 */
@RestController
@RequestMapping("/v1/tontines/{tontineId}/membres")
@RequiredArgsConstructor
@Tag(name = "Membres", description = "Gestion des cotisants d'une tontine")
@SecurityRequirement(name = "bearerAuth")
public class MembreController {

  private static final String SUCCESS = "success";
  private static final int OK = 200;
  private static final int CREATED = 201;

  private final MembreService membreService;
  private final RequestHeaderParser headerParser;

  // ─── Liste ───────────────────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Lister les cotisants d'une tontine",
      description =
          "🔒 Rôles : membres de la tontine, créateur, SUPER_ADMIN.\n\n"
              + "Retourne les cotisants triés par ordre jackpot (position dans la rotation).\n\n"
              + "Pagination : `?page=0&size=20&sort=ordreJackpot,asc`")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Page de cotisants",
        content = @Content(schema = @Schema(implementation = MembreResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine introuvable"),
  })
  public ResponseEntity<CustomResponse> listMembres(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @PageableDefault(size = 20, sort = "ordreJackpot", direction = Sort.Direction.ASC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    Page<MembreResponse> membres = membreService.listMembres(keycloakId, tontineId, pageable);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.MEMBER_LIST_SUCCESS, membres));
  }

  // ─── Ajout ───────────────────────────────────────────────────────────────

  @PostMapping
  @Operation(
      summary = "Ajouter un cotisant",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Ajoute un utilisateur comme cotisant. L'utilisateur doit avoir un compte"
              + " Dinthialma existant. Il reçoit automatiquement le rôle **DINTHIALMA_MEMBER**.\n\n"
              + "Si `ordreJackpot` est null, le membre est placé en fin de liste.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Cotisant ajouté",
        content = @Content(schema = @Schema(implementation = MembreResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Utilisateur déjà membre de la tontine ou tontine complète"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine ou utilisateur introuvable"),
  })
  public ResponseEntity<CustomResponse> addMembre(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @RequestBody @Valid AddMembreRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    MembreResponse response = membreService.addMembre(keycloakId, tontineId, request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                SUCCESS, CREATED, ResponseMessageConstants.MEMBER_ADD_SUCCESS, response));
  }

  // ─── Retrait ─────────────────────────────────────────────────────────────

  @DeleteMapping("/{membreId}")
  @Operation(
      summary = "Retirer un cotisant (soft delete)",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Suppression logique du cotisant (statut → SORTI, deletedAt renseigné)."
              + " Le membre ne voit plus la tontine.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cotisant retiré"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine ou membre introuvable"),
  })
  public ResponseEntity<CustomResponse> removeMembre(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @Parameter(
              description = "UUID du membre (tontine_membres.id)",
              example = "770e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID membreId,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    membreService.removeMembre(keycloakId, tontineId, membreId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.MEMBER_REMOVE_SUCCESS, null));
  }

  // ─── Modification du statut ──────────────────────────────────────────────

  @PatchMapping("/{membreId}/statut")
  @Operation(
      summary = "Modifier le statut d'un cotisant",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Transitions de statut autorisées :\n\n"
              + "- **ACTIF** : activer ou réactiver un membre suspendu\n"
              + "- **SUSPENDU** : suspension temporaire (ne peut plus cotiser)\n"
              + "- **SORTI** : retrait définitif")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Statut mis à jour",
        content = @Content(schema = @Schema(implementation = MembreResponse.class))),
    @ApiResponse(responseCode = "400", description = "Transition de statut invalide"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine ou membre introuvable"),
  })
  public ResponseEntity<CustomResponse> updateStatut(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID tontineId,
      @Parameter(
              description = "UUID du membre (tontine_membres.id)",
              example = "770e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID membreId,
      @RequestBody @Valid UpdateMembreStatutRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    MembreResponse response = membreService.updateStatut(keycloakId, tontineId, membreId, request);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.MEMBER_STATUT_UPDATED, response));
  }
}
