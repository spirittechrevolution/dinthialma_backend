package com.africa.dinthialma_backend.adhesion.controller;

import com.africa.dinthialma_backend.adhesion.codeList.StatutDemande;
import com.africa.dinthialma_backend.adhesion.dto.DemandeAdhesionResponse;
import com.africa.dinthialma_backend.adhesion.dto.DemandeRequests;
import com.africa.dinthialma_backend.adhesion.dto.InvitationApercuResponse;
import com.africa.dinthialma_backend.adhesion.dto.InvitationResponse;
import com.africa.dinthialma_backend.adhesion.service.interfaces.AdhesionService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.security.JwtOptionnel;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Invitation par code (B16) et demandes d'adhésion (B17). */
@RestController
@RequiredArgsConstructor
@Tag(name = "Invitations", description = "Code d'invitation, aperçu public et demandes d'adhésion")
public class AdhesionController {

  private static final String SUCCESS = "success";
  private static final int OK = 200;

  private final AdhesionService adhesionService;
  private final RequestHeaderParser headerParser;
  private final JwtOptionnel jwtOptionnel;

  // ─── B16 : invitation ────────────────────────────────────────────────────

  @PostMapping("/v1/tontines/{tontineId}/invitation")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Obtenir (ou créer) le code d'invitation",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN. Tontine BROUILLON ou ACTIVE.\n\n"
              + "Retourne le code actif (créé au premier appel), le lien `url` et le `qrToken`"
              + " (`t_<code>`). `?regenerer=true` invalide l'ancien code.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Invitation",
        content = @Content(schema = @Schema(implementation = InvitationResponse.class))),
    @ApiResponse(responseCode = "400", description = "Tontine fermée (TONTINE_NOT_OPEN)"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
  })
  public ResponseEntity<CustomResponse> obtenirInvitation(
      @PathVariable UUID tontineId,
      @Parameter(description = "true = nouveau code (l'ancien ne fonctionne plus)")
          @RequestParam(defaultValue = "false")
          boolean regenerer,
      HttpServletRequest httpRequest)
      throws CustomException {
    InvitationResponse r =
        adhesionService.obtenirInvitation(
            headerParser.extractKeycloakId(httpRequest), tontineId, regenerer);
    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.INVITATION_SUCCESS, r));
  }

  @DeleteMapping("/v1/tontines/{tontineId}/invitation")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Révoquer le code d'invitation",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN. Le lien et le QR renvoient ensuite"
              + " 404 ; les demandes déjà envoyées restent traitables.")
  public ResponseEntity<CustomResponse> revoquerInvitation(
      @PathVariable UUID tontineId, HttpServletRequest httpRequest) throws CustomException {
    adhesionService.revoquerInvitation(headerParser.extractKeycloakId(httpRequest), tontineId);
    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.INVITATION_REVOKED, null));
  }

  @GetMapping("/v1/public/invitations/{code}")
  @Operation(
      summary = "Aperçu public d'une invitation",
      description =
          "🌐 Public (JWT facultatif).\n\n"
              + "Infos minimales pour la page d'invitation : nom, type, montant, fréquence,"
              + " événement, places restantes, prénom du gestionnaire et `ouverte`. Aucune donnée"
              + " des membres. Code insensible à la casse, tiret facultatif. Code inconnu ou"
              + " révoqué → 404.\n\n"
              + "**Avec un JWT valide** (en-tête `Authorization: Bearer …`) : ajoute"
              + " `dejaMembre`, `estGestionnaire`, `demandeEnAttente` et `tontineId` (ce dernier"
              + " seulement si membre ou gestionnaire). Sans JWT, ou JWT expiré / invalide :"
              + " ces champs sont absents et la réponse reste celle d'un visiteur (jamais de 401).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Aperçu",
        content = @Content(schema = @Schema(implementation = InvitationApercuResponse.class))),
    @ApiResponse(responseCode = "404", description = "Invitation introuvable ou révoquée"),
  })
  public ResponseEntity<CustomResponse> apercu(
      @PathVariable String code, HttpServletRequest httpRequest) throws CustomException {
    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS,
            OK,
            ResponseMessageConstants.INVITATION_SUCCESS,
            adhesionService.apercu(code, jwtOptionnel.keycloakId(httpRequest).orElse(null))));
  }

  // ─── B17 : demandes ──────────────────────────────────────────────────────

  @PostMapping("/v1/invitations/{code}/demandes")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Demander à rejoindre une tontine",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\n"
              + "Crée une demande EN_ATTENTE et notifie le gestionnaire (`DEMANDE_ADHESION`).\n\n"
              + "Erreurs : `MEMBER_ALREADY_IN_TONTINE`, `DEMANDE_DEJA_EN_ATTENTE`,"
              + " `TONTINE_FULL`, `TONTINE_NOT_OPEN`, 404 si code inconnu.")
  @ApiResponse(
      responseCode = "201",
      description = "Demande créée",
      content = @Content(schema = @Schema(implementation = DemandeAdhesionResponse.class)))
  public ResponseEntity<CustomResponse> creerDemande(
      @PathVariable String code,
      @RequestBody(required = false) @Valid DemandeRequests.Creer request,
      HttpServletRequest httpRequest)
      throws CustomException {
    DemandeAdhesionResponse r =
        adhesionService.creerDemande(headerParser.extractKeycloakId(httpRequest), code, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CustomResponse(SUCCESS, 201, ResponseMessageConstants.DEMANDE_CREATED, r));
  }

  @GetMapping("/v1/me/gestion/demandes")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Demandes reçues sur mes tontines",
      description =
          "🔒 Rôles : tout utilisateur authentifié — tontines dont l'appelant est le créateur.\n\n"
              + "Filtres `?statut=EN_ATTENTE|ACCEPTEE|REFUSEE` et `?tontineId=`. Le téléphone du"
              + " demandeur est fourni pour pouvoir le contacter.")
  public ResponseEntity<CustomResponse> demandesRecues(
      @RequestParam(required = false) StatutDemande statut,
      @RequestParam(required = false) UUID tontineId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS,
            OK,
            ResponseMessageConstants.DEMANDE_LIST_SUCCESS,
            adhesionService.demandesRecues(
                headerParser.extractKeycloakId(httpRequest), statut, tontineId, pageable)));
  }

  @GetMapping("/v1/me/demandes")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Mes demandes d'adhésion",
      description = "🔒 Rôles : tout utilisateur authentifié. Suivi des demandes envoyées.")
  public ResponseEntity<CustomResponse> mesDemandes(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS,
            OK,
            ResponseMessageConstants.DEMANDE_LIST_SUCCESS,
            adhesionService.mesDemandes(headerParser.extractKeycloakId(httpRequest), pageable)));
  }

  @PutMapping("/v1/tontines/{tontineId}/demandes/{demandeId}/accepter")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Accepter une demande d'adhésion",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Ajoute le demandeur comme membre, avec les mêmes règles que l'ajout manuel"
              + " (`ordreJackpot` optionnel, `JACKPOT_POSITION_TAKEN`, notification"
              + " `INVITATION_TONTINE`). Erreurs : `DEMANDE_NOT_PENDING`, `TONTINE_FULL`.")
  public ResponseEntity<CustomResponse> accepter(
      @PathVariable UUID tontineId,
      @PathVariable UUID demandeId,
      @RequestBody(required = false) @Valid DemandeRequests.Accepter request,
      HttpServletRequest httpRequest)
      throws CustomException {
    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS,
            OK,
            ResponseMessageConstants.DEMANDE_ACCEPTED,
            adhesionService.accepter(
                headerParser.extractKeycloakId(httpRequest), tontineId, demandeId, request)));
  }

  @PutMapping("/v1/tontines/{tontineId}/demandes/{demandeId}/refuser")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Refuser une demande d'adhésion",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN. `motif` optionnel, notifié au"
              + " demandeur (`ADHESION_REFUSEE`). Erreur : `DEMANDE_NOT_PENDING`.")
  public ResponseEntity<CustomResponse> refuser(
      @PathVariable UUID tontineId,
      @PathVariable UUID demandeId,
      @RequestBody(required = false) @Valid DemandeRequests.Refuser request,
      HttpServletRequest httpRequest)
      throws CustomException {
    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS,
            OK,
            ResponseMessageConstants.DEMANDE_REFUSED,
            adhesionService.refuser(
                headerParser.extractKeycloakId(httpRequest), tontineId, demandeId, request)));
  }
}
