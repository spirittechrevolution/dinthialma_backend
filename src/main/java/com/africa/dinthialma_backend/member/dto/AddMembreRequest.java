package com.africa.dinthialma_backend.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête d'ajout d'un cotisant à une tontine."
            + " L'utilisateur doit avoir un compte Dinthialma."
            + " Il se voit attribuer automatiquement le rôle DINTHIALMA_MEMBER.")
@Getter
@Setter
public class AddMembreRequest {

  @Schema(
      description =
          "UUID de l'utilisateur à ajouter (users.id en base). L'utilisateur doit exister.",
      example = "550e8400-e29b-41d4-a716-446655440001")
  @NotNull
  private UUID userId;

  @Schema(
      description =
          "Position dans la rotation des jackpots (1 = premier bénéficiaire)."
              + " Optionnel – si null, attribué automatiquement en fin de liste.",
      example = "5")
  private Integer ordreJackpot;
}
