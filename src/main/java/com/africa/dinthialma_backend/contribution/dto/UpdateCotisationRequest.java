package com.africa.dinthialma_backend.contribution.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête de modification d'une cotisation par l'admin."
            + " Seuls les champs non-null sont mis à jour (sémantique PATCH)."
            + " Conditions : statut EN_ATTENTE, ou statut VALIDE si le cycle est encore EN_COURS.")
@Getter
@Setter
public class UpdateCotisationRequest {

  @Schema(description = "Nouveau montant en FCFA (null = inchangé)", example = "6000")
  @Positive
  private BigDecimal montant;

  @Schema(
      description = "Méthode de paiement – code METHODE_PAIEMENT (null = inchangée)",
      example = "ORANGE_MONEY")
  private String methodePaiement;

  @Schema(
      description = "Référence de transaction externe (null = inchangée)",
      example = "OM-TXN-20240701-ABC456")
  private String referenceTransaction;

  @Schema(description = "Note libre (null = inchangée)", example = "Correction du montant initial")
  private String note;
}
