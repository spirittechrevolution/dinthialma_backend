package com.africa.dinthialma_backend.common.publicconfig.controller;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.publicconfig.dto.PublicConfigResponse;
import com.africa.dinthialma_backend.common.publicconfig.service.interfaces.PublicConfigService;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Configuration publique lue par le front avant connexion (B25). */
@RestController
@RequiredArgsConstructor
@Tag(name = "Public", description = "Configuration publique (sans authentification)")
public class PublicConfigController {

  private final PublicConfigService publicConfigService;

  @GetMapping("/v1/public/config")
  @Operation(
      summary = "Configuration publique",
      description =
          "🌐 Public (sans JWT).\n\n"
              + "Contact du support (`supportWhatsapp`, `supportEmail`) et `versionMinimaleApp`"
              + " (le front peut forcer une mise à jour si sa version est inférieure). Un champ"
              + " non configuré côté serveur est absent de la réponse.\n\n"
              + "Variables d'environnement : `SUPPORT_WHATSAPP`, `SUPPORT_EMAIL`,"
              + " `APP_VERSION_MINIMALE`.")
  @ApiResponse(
      responseCode = "200",
      description = "Configuration publique",
      content = @Content(schema = @Schema(implementation = PublicConfigResponse.class)))
  public ResponseEntity<CustomResponse> getConfig() {
    return ResponseEntity.ok(
        new CustomResponse(
            "success",
            200,
            ResponseMessageConstants.PUBLIC_CONFIG_SUCCESS,
            publicConfigService.getConfig()));
  }
}
