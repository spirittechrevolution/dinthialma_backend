package com.africa.dinthialma_backend.adhesion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Corps des requêtes liées aux demandes d'adhésion. */
public final class DemandeRequests {

  private DemandeRequests() {}

  @Schema(description = "Demande à rejoindre une tontine")
  @Getter
  @Setter
  public static class Creer {

    @Schema(
        description = "Message au gestionnaire (optionnel)",
        example = "Je suis la cousine d'Awa")
    @Size(max = 500, message = "Message trop long (500 caractères max)")
    private String message;
  }

  @Schema(description = "Acceptation d'une demande")
  @Getter
  @Setter
  public static class Accepter {

    @Schema(
        description = "Position dans l'ordre des jackpots (optionnel : à la suite sinon)",
        example = "4")
    @Min(value = 1, message = "La position doit être supérieure ou égale à 1")
    private Integer ordreJackpot;
  }

  @Schema(description = "Refus d'une demande")
  @Getter
  @Setter
  public static class Refuser {

    @Schema(description = "Motif communiqué au demandeur (optionnel)", example = "Tontine complète")
    @Size(max = 500, message = "Motif trop long (500 caractères max)")
    private String motif;
  }
}
