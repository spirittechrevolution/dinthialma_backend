package com.africa.dinthialma_backend.contribution.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Corps de requête pour enregistrer une cotisation (paiement effectué par le membre). */
@Getter
@Setter
public class RecordCotisationRequest {

  /** Cycle auquel appartient cette cotisation. */
  @NotNull private UUID cycleId;

  /**
   * Montant cotisé. Doit correspondre au montant configuré dans la tontine, sauf autorisation
   * explicite de l'admin.
   */
  @NotNull @Positive private BigDecimal montant;

  /**
   * Méthode de paiement – réf. code list type {@code METHODE_PAIEMENT}.
   *
   * <p>Ex : {@code WAVE}, {@code ORANGE_MONEY}, {@code CASH}.
   */
  private String methodePaiement;

  /** Référence de transaction externe (ex : ID Wave, Orange Money). */
  private String referenceTransaction;

  /** Note libre du membre (ex : "Paiement pour le mois de janvier"). */
  private String note;
}
