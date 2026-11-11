package com.africa.dinthialma_backend.me.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Validation groupée de cotisations EN_ATTENTE (bouton « Tout valider »)")
@Getter
@Setter
public class ValidationGroupeeRequest {

  /** Taille maximale d'un lot : une page de la file « À valider » tient largement dedans. */
  public static final int MAX_COTISATIONS = 100;

  @ArraySchema(
      arraySchema =
          @Schema(
              description =
                  "UUID des cotisations à valider (1 à 100, doublons ignorés). Elles peuvent"
                      + " appartenir à plusieurs tontines gérées par l'appelant."),
      schema = @Schema(format = "uuid"))
  @NotEmpty(message = "La liste des cotisations à valider est obligatoire")
  @Size(max = MAX_COTISATIONS, message = "Au plus 100 cotisations par validation groupée")
  private List<@NotNull UUID> cotisationIds;
}
