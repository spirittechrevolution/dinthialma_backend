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
        "Corps de la requête admin pour enregistrer une cotisation directement VALIDÉE."
            + " Utilisé pour les paiements cash ou les membres PRE_ENROLLED qui ne peuvent pas"
            + " déclarer eux-mêmes. La cotisation est créée et validée en une seule opération.")
@Getter
@Setter
public class AdminRecordCotisationRequest {

  @Schema(
      description = "UUID du membre dans la tontine (tontine_membres.id)",
      example = "550e8400-e29b-41d4-a716-446655440099")
  @NotNull
  private UUID membreId;

  @Schema(
      description = "UUID du cycle concerné (cycles_tontine.id)",
      example = "550e8400-e29b-41d4-a716-446655440002")
  @NotNull
  private UUID cycleId;

  @Schema(description = "Montant cotisé en FCFA", example = "5000")
  @NotNull
  @Positive
  private BigDecimal montant;

  @Schema(
      description =
          "Méthode de paiement – code METHODE_PAIEMENT."
              + " Exemples : CASH, WAVE, ORANGE_MONEY, FREE_MONEY."
              + " Voir GET /v1/code-list/type/METHODE_PAIEMENT.",
      example = "CASH")
  private String methodePaiement;

  @Schema(
      description = "Référence de transaction externe (Wave, Orange Money…) si applicable",
      example = "WAVE-TXN-20240701-XYZ123")
  private String referenceTransaction;

  @Schema(description = "Note libre de l'admin", example = "Reçu cash le 01/06/2026")
  private String note;
}
