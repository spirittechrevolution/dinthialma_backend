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
          "Retourne les cotisants triés par ordre jackpot. Accès : membres + créateur + SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> listMembres(
      @PathVariable UUID tontineId,
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
          "Ajoute un utilisateur comme cotisant dans la tontine. L'utilisateur se voit attribuer "
              + "le rôle DINTHIALMA_MEMBER. Réservé au créateur et au SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> addMembre(
      @PathVariable UUID tontineId,
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
          "Suppression logique du cotisant (statut → SORTI). Réservé au créateur et au SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> removeMembre(
      @PathVariable UUID tontineId, @PathVariable UUID membreId, HttpServletRequest httpRequest)
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
          "Change le statut d'un cotisant : ACTIF / SUSPENDU / SORTI. "
              + "Réservé au créateur et au SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> updateStatut(
      @PathVariable UUID tontineId,
      @PathVariable UUID membreId,
      @RequestBody @Valid UpdateMembreStatutRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    MembreResponse response = membreService.updateStatut(keycloakId, tontineId, membreId, request);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.MEMBER_STATUT_UPDATED, response));
  }
}
