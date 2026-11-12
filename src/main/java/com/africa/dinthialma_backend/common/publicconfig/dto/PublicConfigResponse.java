package com.africa.dinthialma_backend.common.publicconfig.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Configuration publique de la plateforme")
@Getter
@Builder
public class PublicConfigResponse {

  @Schema(description = "Numéro WhatsApp du support (format international)", nullable = true)
  private String supportWhatsapp;

  @Schema(description = "Email du support", nullable = true)
  private String supportEmail;

  @Schema(
      description =
          "Version minimale du front (SemVer MAJEUR.MINEUR.CORRECTIF). Version du front"
              + " strictement inférieure → écran bloquant « Mise à jour requise ». Absent → aucun"
              + " contrôle.",
      example = "1.0.0",
      nullable = true)
  private String versionMinimaleApp;

  @Schema(
      description =
          "Version recommandée du front (SemVer, ≥ versionMinimaleApp). Version du front"
              + " strictement inférieure → simple bandeau « Nouvelle version disponible », non"
              + " bloquant. Absent → aucun bandeau.",
      example = "1.2.0",
      nullable = true)
  private String versionRecommandeeApp;
}
