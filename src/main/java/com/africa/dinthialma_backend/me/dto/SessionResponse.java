package com.africa.dinthialma_backend.me.dto;

import com.africa.dinthialma_backend.auth.codeList.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(
    description =
        "Appareil connecté. Le backend conserve une session par type de client (WEB, MOBILE) :"
            + " une nouvelle connexion du même type remplace la précédente.")
@Getter
@Builder
public class SessionResponse {

  @Schema(description = "UUID de la session (pour la déconnexion à distance)")
  private UUID id;

  @Schema(
      description = "Type de client",
      allowableValues = {"WEB", "MOBILE"})
  private ClientType clientType;

  @Schema(
      description = "Informations appareil envoyées à la connexion (user-agent, modèle…)",
      nullable = true)
  private String deviceInfo;

  @Schema(description = "Dernière activité (connexion ou rafraîchissement du jeton)")
  private LocalDateTime derniereActivite;

  @Schema(description = "Expiration de la session")
  private LocalDateTime expireLe;

  @Schema(description = "true si la session est expirée (plus utilisable)")
  private boolean expiree;

  @Schema(description = "true pour la session de l'appareil qui fait la requête")
  private boolean courante;
}
