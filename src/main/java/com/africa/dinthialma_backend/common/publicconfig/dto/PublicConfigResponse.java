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

  @Schema(description = "Version minimale de l'app supportée", example = "2.0.0", nullable = true)
  private String versionMinimaleApp;
}
