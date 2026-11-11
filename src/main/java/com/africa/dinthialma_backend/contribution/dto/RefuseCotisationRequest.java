package com.africa.dinthialma_backend.contribution.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Refus d'une cotisation EN_ATTENTE par l'admin. Le motif est communiqué au membre, qui"
            + " pourra déclarer un nouveau paiement sur le même cycle.")
@Getter
@Setter
public class RefuseCotisationRequest {

  @Schema(
      description = "Motif du refus, visible par le membre",
      example = "Transfert non reçu",
      maxLength = 500)
  @NotBlank(message = "Le motif du refus est obligatoire")
  @Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères")
  private String motif;
}
