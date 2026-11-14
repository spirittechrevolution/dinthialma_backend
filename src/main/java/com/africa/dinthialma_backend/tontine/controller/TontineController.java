package com.africa.dinthialma_backend.tontine.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.tontine.codeList.RoleFiltreTontine;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.dto.ChangerOrdreBeneficiaireRequest;
import com.africa.dinthialma_backend.tontine.dto.CreateTontineRequest;
import com.africa.dinthialma_backend.tontine.dto.ModifierEcheanceRequest;
import com.africa.dinthialma_backend.tontine.dto.SimulationCalendrierRequest;
import com.africa.dinthialma_backend.tontine.dto.SimulationCalendrierResponse;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import com.africa.dinthialma_backend.tontine.dto.UpdateTontineRequest;
import com.africa.dinthialma_backend.tontine.service.interfaces.CalendrierService;
import com.africa.dinthialma_backend.tontine.service.interfaces.EcheanceService;
import com.africa.dinthialma_backend.tontine.service.interfaces.TontineService;
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
import org.springdoc.core.annotations.ParameterObject;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
  private final CalendrierService calendrierService;
  private final EcheanceService echeanceService;
  private final RequestHeaderParser headerParser;

  // ─── Création ────────────────────────────────────────────────────────────

  @PostMapping
  @Operation(
      summary = "Créer une tontine",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\n"
              + "L'appelant devient automatiquement l'administrateur de la tontine et reçoit le"
              + " rôle DINTHIALMA_ADMIN. La tontine est créée en statut **BROUILLON**.\n\n"
              + "**Types disponibles :**\n\n"
              + "- `ROTATIVE` *(défaut)* – jackpot tournant. Champs obligatoires supplémentaires :"
              + " `ordreBeneficiaire`, `nombreMembres` (≥2), `modeCycle`, `montant`.\n"
              + "- `EVENEMENTIELLE` – épargne vers un événement (Tabaski, Korité…). Champs"
              + " obligatoires supplémentaires : `dateEcheance` (> `dateDebut`), `montant` si"
              + " `montantLibre=false`. Champ optionnel : `nomEvenement`, `montantMinimum`.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Tontine créée en statut BROUILLON",
        content = @Content(schema = @Schema(implementation = TontineResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides (validation)"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
  })
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

  // ─── Simulation du calendrier (B26) ──────────────────────────────────────

  @GetMapping("/simulation")
  @Operation(
      summary = "Simuler le calendrier des tours",
      description =
          "🔒 Rôles : tout utilisateur authentifié. Aucun effet en base.\n\n"
              + "Calcule les échéances avec les mêmes règles que l'activation (B26), pour l'écran"
              + " de création : `rythmeIntervalle` + `rythmeUnite` (ou `frequence`),"
              + " `jourEcheance`, `heureEcheance`, `dateDebut` ; ROTATIVE : `nombreMembres`"
              + " (+ `nombreGagnants`) ; EVENEMENTIELLE : `dateEcheance`. 400 échéances listées au"
              + " plus.\n\n"
              + "Erreurs : `RYTHME_INVALIDE`, `JOUR_ECHEANCE_INVALIDE`, `HEURE_ECHEANCE_INVALIDE`.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Calendrier simulé",
        content = @Content(schema = @Schema(implementation = SimulationCalendrierResponse.class))),
    @ApiResponse(responseCode = "400", description = "Rythme ou paramètres invalides"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
  })
  public ResponseEntity<CustomResponse> simulerCalendrier(
      @ParameterObject @Valid SimulationCalendrierRequest request) throws CustomException {
    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS,
            OK,
            ResponseMessageConstants.TONTINE_SIMULATION_SUCCESS,
            calendrierService.simuler(request)));
  }

  // ─── Liste ───────────────────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Lister les tontines",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\n"
              + "- **SUPER_ADMIN** : voit toutes les tontines de la plateforme.\n"
              + "- **Autres** : voit uniquement les tontines dont il est créateur + celles où il"
              + " est cotisant.\n\n"
              + "Chaque tontine indique la relation de l'appelant : `monRole`"
              + " (GESTIONNAIRE / MEMBRE / GESTIONNAIRE_ET_MEMBRE / LECTURE), `monMembreId` et"
              + " `monStatutMembre`.\n\n"
              + "Filtres optionnels :\n"
              + "- `role=GERE|COTISE` (s'applique aussi au SUPER_ADMIN, qui ne voit alors que ses"
              + " propres tontines)\n"
              + "- `statut` (BROUILLON, ACTIVE, SUSPENDUE, TERMINEE)\n"
              + "- `q` : nom de la tontine ou de l'événement, insensible à la casse\n\n"
              + "Pagination : `?page=0&size=20&sort=createdAt,desc`")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Page de tontines",
        content = @Content(schema = @Schema(implementation = TontineResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
  })
  public ResponseEntity<CustomResponse> listTontines(
      @Parameter(description = "GERE = tontines créées par moi ; COTISE = tontines où je cotise")
          @RequestParam(required = false)
          RoleFiltreTontine role,
      @Parameter(description = "Filtre par statut (BROUILLON, ACTIVE, SUSPENDUE, TERMINEE)")
          @RequestParam(required = false)
          TontineStatut statut,
      @Parameter(description = "Recherche sur le nom de la tontine ou de l'événement")
          @RequestParam(required = false)
          String q,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    Page<TontineResponse> tontines =
        tontineService.listTontines(keycloakId, role, statut, q, pageable);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.TONTINE_LIST_SUCCESS, tontines));
  }

  // ─── Récupération ────────────────────────────────────────────────────────

  @GetMapping("/{id}")
  @Operation(
      summary = "Récupérer une tontine",
      description =
          "🔒 Rôles : créateur de la tontine, membres de la tontine, SUPER_ADMIN.\n\n"
              + "Retourne le détail complet de la tontine avec les informations du créateur et la"
              + " relation de l'appelant (`monRole`, `monMembreId`, `monStatutMembre`).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Détail de la tontine",
        content = @Content(schema = @Schema(implementation = TontineResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(
        responseCode = "403",
        description = "Accès interdit – pas créateur ni membre ni SUPER_ADMIN"),
    @ApiResponse(responseCode = "404", description = "Tontine introuvable"),
  })
  public ResponseEntity<CustomResponse> getTontine(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID id,
      HttpServletRequest httpRequest)
      throws CustomException {

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
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Modification autorisée **uniquement si la tontine est en statut BROUILLON**."
              + " Les champs null dans la requête ne sont pas modifiés.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Tontine mise à jour",
        content = @Content(schema = @Schema(implementation = TontineResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Données invalides ou tontine pas en BROUILLON"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(
        responseCode = "403",
        description = "Accès interdit – pas le créateur ni SUPER_ADMIN"),
    @ApiResponse(responseCode = "404", description = "Tontine introuvable"),
  })
  public ResponseEntity<CustomResponse> updateTontine(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID id,
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
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Suppression logique (deletedAt renseigné) – la tontine disparaît des listes mais"
              + " les données restent en base. Autorisé **uniquement en statut BROUILLON**.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Tontine supprimée"),
    @ApiResponse(responseCode = "400", description = "Tontine pas en statut BROUILLON"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine introuvable"),
  })
  public ResponseEntity<CustomResponse> deleteTontine(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID id,
      HttpServletRequest httpRequest)
      throws CustomException {

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
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Passe la tontine de **BROUILLON** (ou SUSPENDUE) à **ACTIVE**.\n\n"
              + "**ROTATIVE – Mode AUTOMATIQUE** : génère immédiatement tous les cycles"
              + " (N membres → N cycles), le premier passe EN_COURS.\n\n"
              + "**ROTATIVE – Mode MANUEL** : active la tontine sans générer de cycles."
              + " Ouvrir les cycles via `POST .../cycles`.\n\n"
              + "**EVENEMENTIELLE** : génère automatiquement tous les sous-cycles de"
              + " `dateDebut` à `dateEcheance` selon la `frequence` (JOURNALIERE, HEBDOMADAIRE,"
              + " BIMENSUEL, MENSUEL, TRIMESTRIEL). Le dernier sous-cycle se termine exactement"
              + " sur `dateEcheance`.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Tontine activée",
        content = @Content(schema = @Schema(implementation = TontineResponse.class))),
    @ApiResponse(responseCode = "400", description = "Tontine déjà ACTIVE ou TERMINEE"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine introuvable"),
  })
  public ResponseEntity<CustomResponse> activateTontine(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID id,
      HttpServletRequest httpRequest)
      throws CustomException {

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
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN.\n\n"
              + "Passe la tontine de **ACTIVE** à **SUSPENDUE**. Les cycles et cotisations existants"
              + " sont conservés. La tontine peut être réactivée via `PUT /{id}/activer`.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Tontine suspendue",
        content = @Content(schema = @Schema(implementation = TontineResponse.class))),
    @ApiResponse(responseCode = "400", description = "Tontine pas en statut ACTIVE"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "403", description = "Accès interdit"),
    @ApiResponse(responseCode = "404", description = "Tontine introuvable"),
  })
  public ResponseEntity<CustomResponse> suspendTontine(
      @Parameter(
              description = "UUID de la tontine",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID id,
      HttpServletRequest httpRequest)
      throws CustomException {

    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    TontineResponse response = tontineService.suspendTontine(keycloakId, id);

    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.TONTINE_SUSPENDED, response));
  }

  @PatchMapping("/{id}/ordre-beneficiaire")
  @Operation(
      summary = "Changer l'ordre des bénéficiaires (y compris en cours de tontine)",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN. Tontine ROTATIVE BROUILLON, ACTIVE ou"
              + " SUSPENDUE. Effet à la prochaine clôture de tour ; les gagnants déjà désignés ne"
              + " changent pas. Vers ROTATION ou ALEATOIRE : refusé (409 `DESIGNATION_EN_ATTENTE`)"
              + " tant qu'un tour clôturé attend ses gagnants. Membres actifs notifiés"
              + " (`ORDRE_BENEFICIAIRE_MODIFIE`).")
  public ResponseEntity<CustomResponse> changerOrdreBeneficiaire(
      @PathVariable UUID id,
      @RequestBody @Valid ChangerOrdreBeneficiaireRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    TontineResponse response = tontineService.changerOrdreBeneficiaire(keycloakId, id, request);
    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.ORDRE_BENEFICIAIRE_UPDATED, response));
  }

  @PatchMapping("/{id}/echeance")
  @Operation(
      summary = "Modifier le jour / l'heure d'échéance et les rappels (y compris en cours)",
      description =
          "🔒 Rôles : créateur de la tontine, SUPER_ADMIN. Tontine non terminée (sinon 400"
              + " `ECHEANCE_NON_MODIFIABLE`). Champ absent = inchangé ; `jourEcheance: \"\"` retire"
              + " le jour imposé. Le tour en cours et les tours terminés ne bougent pas ; les tours"
              + " pas encore commencés sont replanifiés (événementielle : jusqu'à la date de"
              + " l'événement). Changement d'échéance notifié aux membres actifs"
              + " (`REGLES_MODIFIEES`). Le rythme (intervalle, unité) ne se modifie qu'en"
              + " BROUILLON via PUT.")
  public ResponseEntity<CustomResponse> modifierEcheance(
      @PathVariable UUID id,
      @RequestBody @Valid ModifierEcheanceRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    TontineResponse response = echeanceService.modifierEcheance(keycloakId, id, request);
    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.ECHEANCE_UPDATED, response));
  }
}
