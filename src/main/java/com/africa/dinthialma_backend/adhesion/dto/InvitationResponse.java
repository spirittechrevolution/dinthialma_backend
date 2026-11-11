package com.africa.dinthialma_backend.adhesion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Invitation active d'une tontine (lien et QR à partager)")
@Getter
@Builder
public class InvitationResponse {

  @Schema(description = "Code d'invitation", example = "7Q2K-M9XD")
  private String code;

  @Schema(
      description = "Lien à partager (page d'invitation)",
      example = "https://www.dinthialma.com/q/t/7Q2K-M9XD")
  private String url;

  @Schema(
      description = "Valeur à encoder dans le QR (résolue par GET /v1/qr/{token})",
      example = "t_7Q2K-M9XD")
  private String qrToken;
}
