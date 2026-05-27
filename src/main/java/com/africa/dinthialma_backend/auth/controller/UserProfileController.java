package com.africa.dinthialma_backend.auth.controller;

import com.africa.dinthialma_backend.auth.dto.PhoneChangeRequest;
import com.africa.dinthialma_backend.auth.dto.PhoneChangeVerifyRequest;
import com.africa.dinthialma_backend.auth.dto.UpdateProfileRequest;
import com.africa.dinthialma_backend.auth.dto.UserProfileResponse;
import com.africa.dinthialma_backend.auth.service.interfaces.UserProfileService;
import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Gestion du profil utilisateur connecté.
 *
 * <p>🔒 Tous les endpoints nécessitent un JWT valide (bearerAuth).
 *
 * <p>Accès : USER, MEMBER, ADMIN, SUPER_ADMIN (chacun gère son propre profil).
 */
@RestController
@RequestMapping("/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profil", description = "Gestion du profil de l'utilisateur connecté")
public class UserProfileController {

  private final UserProfileService userProfileService;
  private final RequestHeaderParser requestHeaderParser;

  // ─── Lecture ─────────────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Voir mon profil",
      description = "🔒 Accès : USER, MEMBER, ADMIN, SUPER_ADMIN",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Profil retourné")
  public ResponseEntity<CustomResponse> getProfile(HttpServletRequest httpRequest)
      throws CustomException {
    String keycloakId = requestHeaderParser.extractKeycloakId(httpRequest);
    UserProfileResponse profile = userProfileService.getProfile(keycloakId);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Profil récupéré avec succès",
            profile));
  }

  // ─── Modification profil ─────────────────────────────────────────────

  @PutMapping
  @Operation(
      summary = "Modifier mon profil (nom, prénom, email)",
      description = "🔒 Accès : USER, MEMBER, ADMIN, SUPER_ADMIN — Met à jour DB + Keycloak.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Profil mis à jour"),
    @ApiResponse(responseCode = "409", description = "Email déjà utilisé"),
  })
  public ResponseEntity<CustomResponse> updateProfile(
      @RequestBody @Valid UpdateProfileRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    String keycloakId = requestHeaderParser.extractKeycloakId(httpRequest);
    UserProfileResponse updated = userProfileService.updateProfile(keycloakId, request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Profil mis à jour avec succès",
            updated));
  }

  // ─── Changement de numéro ────────────────────────────────────────────

  @PostMapping("/phone/request-change")
  @Operation(
      summary = "Demander un changement de numéro – étape 1/2",
      description =
          "🔒 Envoie un OTP SMS au NOUVEAU numéro. "
              + "Les tontines et memberships sont conservés (liés à l'UUID, pas au numéro).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "OTP envoyé au nouveau numéro"),
    @ApiResponse(responseCode = "400", description = "Nouveau numéro identique à l'actuel"),
    @ApiResponse(responseCode = "409", description = "Numéro déjà utilisé par un autre compte"),
  })
  public ResponseEntity<CustomResponse> requestPhoneChange(
      @RequestBody @Valid PhoneChangeRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    String keycloakId = requestHeaderParser.extractKeycloakId(httpRequest);
    userProfileService.requestPhoneChange(keycloakId, request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AUTH_OTP_SENT,
            null));
  }

  @PostMapping("/phone/verify")
  @Operation(
      summary = "Confirmer le changement de numéro – étape 2/2",
      description =
          "🔒 Vérifie l'OTP et applique le changement : met à jour Keycloak + DB + révoque toutes les sessions. "
              + "Re-connexion obligatoire après cette opération.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Numéro changé – toutes les sessions révoquées"),
    @ApiResponse(responseCode = "400", description = "OTP invalide ou expiré"),
  })
  public ResponseEntity<CustomResponse> verifyPhoneChange(
      @RequestBody @Valid PhoneChangeVerifyRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    String keycloakId = requestHeaderParser.extractKeycloakId(httpRequest);
    userProfileService.verifyPhoneChange(keycloakId, request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Numéro de téléphone mis à jour. Veuillez vous reconnecter.",
            null));
  }
}
