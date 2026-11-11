package com.africa.dinthialma_backend.me.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Mon QR membre : jeton opaque stable (ne contient pas mon numéro)")
@Getter
@Builder
public class MonQrResponse {

  @Schema(description = "Jeton à encoder dans le QR", example = "m_Xc8fK2pQ0aLm3VbN9tYz1w")
  private String qrToken;

  @Schema(
      description = "URL complète à encoder (ouvre l'app ou la page web)",
      example = "https://www.dinthialma.com/q/m_Xc8fK2pQ0aLm3VbN9tYz1w")
  private String url;
}
