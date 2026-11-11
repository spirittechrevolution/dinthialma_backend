package com.africa.dinthialma_backend.me.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.me.dto.MonQrResponse;
import com.africa.dinthialma_backend.me.dto.QrResolutionResponse;
import com.africa.dinthialma_backend.me.service.interfaces.QrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** QR membre (B15) : mon QR ({@code /v1/me/qr}) et résolution d'un QR scanné ({@code /v1/qr}). */
@RestController
@RequiredArgsConstructor
@Tag(name = "QR", description = "QR membre : jeton opaque et résolution d'un QR scanné")
@SecurityRequirement(name = "bearerAuth")
public class QrController {

  private static final String SUCCESS = "success";
  private static final int OK = 200;

  private final QrService qrService;
  private final RequestHeaderParser headerParser;

  @GetMapping("/v1/me/qr")
  @Operation(
      summary = "Mon QR membre",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\n"
              + "Retourne un jeton opaque **stable** (`qrToken`) et l'`url` à encoder dans le QR."
              + " Le numéro de téléphone n'y figure pas. Le jeton est créé à la première demande"
              + " puis réutilisé.")
  @ApiResponse(
      responseCode = "200",
      description = "Mon QR",
      content = @Content(schema = @Schema(implementation = MonQrResponse.class)))
  public ResponseEntity<CustomResponse> getMonQr(HttpServletRequest httpRequest)
      throws CustomException {
    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS, OK, ResponseMessageConstants.ME_QR_SUCCESS, qrService.getMonQr(keycloakId)));
  }

  @PostMapping("/v1/me/qr/regenerer")
  @Operation(
      summary = "Régénérer mon QR (révocation)",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\n"
              + "Crée un nouveau jeton : l'ancien QR (capture d'écran, impression) ne se résout"
              + " plus (404).")
  @ApiResponse(
      responseCode = "200",
      description = "Nouveau QR",
      content = @Content(schema = @Schema(implementation = MonQrResponse.class)))
  public ResponseEntity<CustomResponse> regenererMonQr(HttpServletRequest httpRequest)
      throws CustomException {
    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS,
            OK,
            ResponseMessageConstants.ME_QR_SUCCESS,
            qrService.regenererMonQr(keycloakId)));
  }

  @GetMapping("/v1/qr/{token}")
  @Operation(
      summary = "Résoudre un QR scanné",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\n"
              + "Renvoie l'identité de la personne (`id`, prénom, nom, statut de compte — **jamais"
              + " le téléphone**) et `dansMesTontines` : pour chaque tontine **que l'appelant"
              + " gère** où elle est inscrite, son `membreId`, son statut et si elle a déjà payé"
              + " le tour en cours (`aPayeCycleEnCours`). Liste vide si elle n'est dans aucune de"
              + " mes tontines.\n\n"
              + "Jeton inconnu ou régénéré → 404 `NOT_FOUND`.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "QR résolu",
        content = @Content(schema = @Schema(implementation = QrResolutionResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
    @ApiResponse(responseCode = "404", description = "QR inconnu ou révoqué"),
  })
  public ResponseEntity<CustomResponse> resoudre(
      @Parameter(description = "Jeton lu dans le QR", example = "m_Xc8fK2pQ0aLm3VbN9tYz1w")
          @PathVariable
          String token,
      HttpServletRequest httpRequest)
      throws CustomException {
    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    return ResponseEntity.ok(
        new CustomResponse(
            SUCCESS,
            OK,
            ResponseMessageConstants.QR_RESOLVE_SUCCESS,
            qrService.resoudre(keycloakId, token)));
  }
}
