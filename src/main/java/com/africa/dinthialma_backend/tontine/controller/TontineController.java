package com.africa.dinthialma_backend.tontine.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.tontine.dto.CreateTontineRequest;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import com.africa.dinthialma_backend.tontine.dto.UpdateTontineRequest;
import com.africa.dinthialma_backend.tontine.service.interfaces.TontineService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion des tontines.
 *
 * <p>Accès :
 *
 * <ul>
 *   <li>Création → tout utilisateur authentifié (devient ADMIN de sa tontine)
 *   <li>Lecture → membre de la tontine, créateur, SUPER_ADMIN
 *   <li>Modification / suppression / activation → créateur ou SUPER_ADMIN
 * </ul>
 */
@RestController
@RequestMapping("/v1/tontines")
@RequiredArgsConstructor
@Tag(name = "Tontines", description = "Gestion des groupes de tontine (CRUD + cycle de vie)")
@SecurityRequirement(name = "bearerAuth")
public class TontineController {

  private static final String SUCCESS = "success";
  private static final int OK = 200;
  private static final int CREATED = 201;

  private final TontineService tontineService;
  private final RequestHeaderParser headerParser;

  // ─── Création ────────────────────────────────────────────────────────────

  @PostMapping
  @Operation(
      summary = "Créer une tontine",
      description =
          "Tout utilisateur authentifié peut créer une tontine. Il en devient l'administrateur "
              + "et se voit attribuer le rôle DINTHIALMA_ADMIN.")
  public ResponseEntity<CustomResponse> createTontine(
      @RequestBody @Valid CreateTontineRequest request, HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    TontineResponse response = tontineService.createTontine(keycloakId, request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                SUCCESS, CREATED, ResponseMessageConstants.TONTINE_CREATE_SUCCESS, response));
  }

  // ─── Liste ───────────────────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Lister les tontines",
      description =
          "SUPER_ADMIN : toutes les tontines. Autres : tontines créées + tontines où l'utilisateur "
              + "est cotisant.")
  public ResponseEntity<CustomResponse> listTontines(HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    List<TontineResponse> tontines = tontineService.listTontines(keycloakId);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.TONTINE_LIST_SUCCESS, tontines));
  }

  // ─── Récupération ────────────────────────────────────────────────────────

  @GetMapping("/{id}")
  @Operation(
      summary = "Récupérer une tontine",
      description = "Accès : membres, créateur, SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> getTontine(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    TontineResponse response = tontineService.getTontine(keycloakId, id);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.TONTINE_GET_SUCCESS, response));
  }

  // ─── Mise à jour ─────────────────────────────────────────────────────────

  @PutMapping("/{id}")
  @Operation(
      summary = "Mettre à jour une tontine",
      description =
          "Champs modifiables uniquement si la tontine est en BROUILLON. "
              + "Réservé au créateur et au SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> updateTontine(
      @PathVariable UUID id,
      @RequestBody @Valid UpdateTontineRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    TontineResponse response = tontineService.updateTontine(keycloakId, id, request);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.TONTINE_UPDATE_SUCCESS, response));
  }

  // ─── Suppression ─────────────────────────────────────────────────────────

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Supprimer une tontine (soft delete)",
      description =
          "Suppression logique. Autorisé uniquement en statut BROUILLON. "
              + "Réservé au créateur et au SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> deleteTontine(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    tontineService.deleteTontine(keycloakId, id);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.TONTINE_DELETE_SUCCESS, null));
  }

  // ─── Activation ──────────────────────────────────────────────────────────

  @PutMapping("/{id}/activer")
  @Operation(
      summary = "Activer une tontine",
      description =
          "Passe la tontine de BROUILLON (ou SUSPENDUE) à ACTIVE. En mode AUTOMATIQUE, génère "
              + "tous les cycles. Réservé au créateur et au SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> activateTontine(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    TontineResponse response = tontineService.activateTontine(keycloakId, id);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.TONTINE_ACTIVATED, response));
  }

  // ─── Suspension ──────────────────────────────────────────────────────────

  @PutMapping("/{id}/suspendre")
  @Operation(
      summary = "Suspendre une tontine",
      description =
          "Passe la tontine de ACTIVE à SUSPENDUE. Réservé au créateur et au SUPER_ADMIN.")
  public ResponseEntity<CustomResponse> suspendTontine(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    TontineResponse response = tontineService.suspendTontine(keycloakId, id);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.TONTINE_SUSPENDED, response));
  }
}
