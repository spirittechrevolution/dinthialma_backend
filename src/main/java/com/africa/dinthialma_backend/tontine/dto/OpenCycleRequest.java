package com.africa.dinthialma_backend.tontine.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Corps de requête pour l'ouverture manuelle d'un cycle (mode {@code MANUEL} uniquement).
 *
 * <p>Le bénéficiaire peut être désigné immédiatement ou plus tard via un PATCH dédié.
 */
@Getter
@Setter
public class OpenCycleRequest {

  /** Date de début du cycle. */
  @NotNull @FutureOrPresent private LocalDate dateDebut;

  /** Date de fin du cycle. */
  @NotNull private LocalDate dateFin;

  /**
   * Id du membre bénéficiaire du jackpot (UUID de {@code tontine_membres.id}). Peut être null si le
   * bénéficiaire n'est pas encore désigné.
   */
  private UUID beneficiaireId;
}
