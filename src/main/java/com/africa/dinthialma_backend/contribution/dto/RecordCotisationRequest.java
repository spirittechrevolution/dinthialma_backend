package com.africa.dinthialma_backend.contribution.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête d'enregistrement d'une cotisation par le membre."
            + " Une seule cotisation par membre par cycle est autorisée."
            + " La cotisation est créée en statut EN_ATTENTE – l'admin devra la valider.")
@Getter
@Setter
public class RecordCotisationRequest {

  @Schema(
      description = "UUID du cycle auquel appartient cette cotisation (cycles_tontine.id)",
      example = "550e8400-e29b-41d4-a716-446655440002")
  @NotNull
  private UUID cycleId;

  @Schema(
      description =
          "Montant cotisé en FCFA. Doit idéalement correspondre au montant configuré sur la"
              + " tontine.",
      example = "5000")
  @NotNull
  @Positive
  private BigDecimal montant;

  @Schema(
      description =
          "Méthode de paiement – code de la liste METHODE_PAIEMENT."
              + " Exemples : WAVE, ORANGE_MONEY, FREE_MONEY, CASH."
              + " Voir GET /v1/code-list/type/METHODE_PAIEMENT.",
      example = "WAVE")
  private String methodePaiement;

  @Schema(
      description =
          "Référence de transaction externe (ID Wave, Orange Money, etc.) permettant à l'admin de"
              + " vérifier le paiement",
      example = "WAVE-TXN-20240701-XYZ123")
  private String referenceTransaction;

  @Schema(description = "Note libre du membre", example = "Paiement pour juillet 2024")
  private String note;
}
