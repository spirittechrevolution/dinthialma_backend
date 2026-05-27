package com.africa.dinthialma_backend.member.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Corps de requête pour ajouter un cotisant à une tontine. */
@Getter
@Setter
public class AddMembreRequest {

  /**
   * UUID de l'utilisateur à ajouter ({@code users.id} en base).
   *
   * <p>L'utilisateur doit exister dans la table {@code users}.
   */
  @NotNull private UUID userId;

  /**
   * Position dans la rotation des jackpots (1 = premier bénéficiaire).
   *
   * <p>Optionnel – si null, attribué automatiquement en fin de liste.
   */
  private Integer ordreJackpot;
}
