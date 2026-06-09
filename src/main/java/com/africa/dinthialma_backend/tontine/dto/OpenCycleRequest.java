package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête d'ouverture manuelle d'un cycle."
            + " Disponible uniquement si la tontine est en mode MANUEL."
            + " Un seul cycle EN_COURS est autorisé à la fois."
            + " Les gagnants sont désignés après clôture via PATCH /{cycleId}/gagnants.")
@Getter
@Setter
public class OpenCycleRequest {

  @Schema(description = "Date de début du cycle (aujourd'hui ou future)", example = "2024-07-01")
  @NotNull
  @FutureOrPresent
  private LocalDate dateDebut;

  @Schema(
      description = "Date de fin prévue du cycle (doit être après dateDebut)",
      example = "2024-07-31")
  @NotNull
  private LocalDate dateFin;
}
