package com.africa.dinthialma_backend.me.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.me.dto.ChangePasswordRequest;
import com.africa.dinthialma_backend.me.dto.PreferencesDto;
import com.africa.dinthialma_backend.me.dto.SessionResponse;
import com.africa.dinthialma_backend.me.service.interfaces.CompteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Compte de l'utilisateur connecté : mot de passe (B21) et appareils connectés (B20). */
@RestController
@RequestMapping("/v1/me")
@RequiredArgsConstructor
@Tag(name = "Mon compte", description = "Mot de passe et appareils connectés")
@SecurityRequirement(name = "bearerAuth")
public class CompteController {

  private static final String SUCCESS = "success";
  private static final int OK = 200;

  private final CompteService compteService;
  private final RequestHeaderParser headerParser;

  @PostMapping("/password")
  @Operation(
      summary = "Changer mon mot de passe",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\n"
              + "Vérifie `ancienMotDePasse` puis applique `nouveauMotDePasse` (≥ 8 caractères)."
              + " Avec `deconnecterAutresAppareils=true`, toutes les autres sessions sont"
              + " révoquées (celle de la requête est conservée).\n\n"
              + "Erreurs : `CURRENT_PASSWORD_INVALID` (ancien incorrect), `PASSWORD_UNCHANGED`"
              + " (identique à l'ancien), `VALIDATION_ERROR`.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Mot de passe changé"),
    @ApiResponse(responseCode = "400", description = "Ancien mot de passe incorrect ou invalide"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
  })
  public ResponseEntity<CustomResponse> changerMotDePasse(
      @RequestBody @Valid ChangePasswordRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    compteService.changerMotDePasse(
        headerParser.extractKeycloakId(httpRequest),
        headerParser.extractSessionId(httpRequest),
        request);
    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.ME_PASSWORD_CHANGED, null));
  }

  @GetMapping("/preferences")
  @Operation(
      summary = "Mes préférences",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\n"
              + "Préférences synchronisées entre appareils : `tontinesFavorites`, `dernierMode`"
              + " (COTISE/GERE), `masquerMontant`, `dernierMoyenPaiement`. Valeurs par défaut si"
              + " elles n'ont jamais été enregistrées.")
  @ApiResponse(
      responseCode = "200",
      description = "Mes préférences",
      content = @Content(schema = @Schema(implementation = PreferencesDto.class)))
  public ResponseEntity<CustomResponse> getPreferences(HttpServletRequest httpRequest)
      throws CustomException {
    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS,
            OK,
            ResponseMessageConstants.ME_PREFERENCES_SUCCESS,
            compteService.getPreferences(headerParser.extractKeycloakId(httpRequest))));
  }

  @PutMapping("/preferences")
  @Operation(
      summary = "Enregistrer mes préférences",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\n"
              + "Remplace l'ensemble des préférences (envoyer l'objet complet). Favoris"
              + " dédoublonnés, 50 max ; champs inconnus ignorés.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Préférences enregistrées",
        content = @Content(schema = @Schema(implementation = PreferencesDto.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides (VALIDATION_ERROR)"),
  })
  public ResponseEntity<CustomResponse> savePreferences(
      @RequestBody @Valid PreferencesDto preferences, HttpServletRequest httpRequest)
      throws CustomException {
    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS,
            OK,
            ResponseMessageConstants.ME_PREFERENCES_SUCCESS,
            compteService.savePreferences(
                headerParser.extractKeycloakId(httpRequest), preferences)));
  }

  @GetMapping("/sessions")
  @Operation(
      summary = "Mes appareils connectés",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\n"
              + "Une session par type de client (WEB, MOBILE) : type, `deviceInfo` envoyé à la"
              + " connexion, dernière activité, expiration, et `courante=true` pour l'appareil"
              + " qui fait la requête.")
  @ApiResponse(
      responseCode = "200",
      description = "Mes sessions",
      content =
          @Content(array = @ArraySchema(schema = @Schema(implementation = SessionResponse.class))))
  public ResponseEntity<CustomResponse> listerSessions(HttpServletRequest httpRequest)
      throws CustomException {
    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS,
            OK,
            ResponseMessageConstants.ME_SESSIONS_SUCCESS,
            compteService.listerSessions(
                headerParser.extractKeycloakId(httpRequest),
                headerParser.extractSessionId(httpRequest))));
  }

  @DeleteMapping("/sessions/{sessionId}")
  @Operation(
      summary = "Déconnecter un appareil à distance",
      description =
          "🔒 Rôles : tout utilisateur authentifié (ses propres sessions uniquement).\n\n"
              + "Révoque la session Keycloak (le refresh token ne fonctionne plus) et supprime la"
              + " session. Une session d'un autre utilisateur renvoie 404.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Session révoquée"),
    @ApiResponse(responseCode = "404", description = "Session introuvable"),
  })
  public ResponseEntity<CustomResponse> revoquerSession(
      @Parameter(description = "UUID de la session") @PathVariable UUID sessionId,
      HttpServletRequest httpRequest)
      throws CustomException {
    compteService.revoquerSession(headerParser.extractKeycloakId(httpRequest), sessionId);
    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, ResponseMessageConstants.ME_SESSION_REVOKED, null));
  }
}
