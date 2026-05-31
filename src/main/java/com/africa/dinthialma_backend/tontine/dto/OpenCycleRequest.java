package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête d'ouverture manuelle d'un cycle."
            + " Disponible uniquement si la tontine est en mode MANUEL."
            + " Un seul cycle EN_COURS est autorisé à la fois.")
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

  @Schema(
      description =
          "UUID du membre bénéficiaire du jackpot (tontine_membres.id)."
              + " Optionnel – peut être désigné plus tard.",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID beneficiaireId;
}
