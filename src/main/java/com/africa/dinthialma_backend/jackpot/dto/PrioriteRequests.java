package com.africa.dinthialma_backend.jackpot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Corps des requêtes liées aux priorités de jackpot. */
public final class PrioriteRequests {

  private PrioriteRequests() {}

  @Schema(description = "Demande du membre : recevoir le prochain jackpot en priorité")
  @Getter
  @Setter
  public static class Demander {

    @Schema(
        description = "Raison du besoin, lue par le gestionnaire (optionnel)",
        example = "Frais d'hospitalisation de ma mère")
    @Size(max = 500, message = "Motif trop long (500 caractères max)")
    private String motif;
  }

  @Schema(description = "Attribution directe par le gestionnaire (accord pris avec le groupe)")
  @Getter
  @Setter
  public static class Attribuer {

    @Schema(description = "UUID du membership bénéficiaire")
    @NotNull(message = "membreId est obligatoire")
    private UUID membreId;

    @Schema(description = "Raison (optionnel)", example = "Décidé en réunion du 12/11")
    @Size(max = 500, message = "Motif trop long (500 caractères max)")
    private String motif;
  }

  @Schema(description = "Refus ou annulation d'une priorité")
  @Getter
  @Setter
  public static class Refuser {

    @Schema(description = "Motif communiqué au membre (optionnel)", example = "Tour déjà promis")
    @Size(max = 500, message = "Motif trop long (500 caractères max)")
    private String motif;
  }
}
