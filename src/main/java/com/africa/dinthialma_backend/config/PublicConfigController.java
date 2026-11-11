package com.africa.dinthialma_backend.config;

import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Configuration publique lue par le front avant connexion (B25). */
@RestController
@Tag(name = "Public", description = "Configuration publique (sans authentification)")
public class PublicConfigController {

  @Value("${dinthialma.support.whatsapp:}")
  private String supportWhatsapp;

  @Value("${dinthialma.support.email:}")
  private String supportEmail;

  @Value("${dinthialma.version-minimale-app:}")
  private String versionMinimaleApp;

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
      content = @Content(schema = @Schema(implementation = PublicConfig.class)))
  public ResponseEntity<CustomResponse> getConfig() {
    PublicConfig config =
        PublicConfig.builder()
            .supportWhatsapp(vide(supportWhatsapp))
            .supportEmail(vide(supportEmail))
            .versionMinimaleApp(vide(versionMinimaleApp))
            .build();
    return ResponseEntity.ok(
        new CustomResponse("success", 200, ResponseMessageConstants.PUBLIC_CONFIG_SUCCESS, config));
  }

  private static String vide(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  @Schema(description = "Configuration publique de la plateforme")
  @Getter
  @Builder
  public static class PublicConfig {

    @Schema(description = "Numéro WhatsApp du support (format international)", nullable = true)
    private String supportWhatsapp;

    @Schema(description = "Email du support", nullable = true)
    private String supportEmail;

    @Schema(description = "Version minimale de l'app supportée", example = "2.0.0", nullable = true)
    private String versionMinimaleApp;
  }
}
