package com.africa.dinthialma_backend.tontine.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Corps de requête pour la mise à jour d'une tontine.
 *
 * <p>Tous les champs sont optionnels – seuls les champs non nuls sont mis à jour. Modification
 * autorisée uniquement si la tontine est en statut {@code BROUILLON}.
 */
@Getter
@Setter
public class UpdateTontineRequest {

  @Size(min = 2, max = 150)
  private String nom;

  private String description;

  @Positive private BigDecimal montant;

  private String frequence;

  private String ordreBeneficiaire;

  private LocalDate dateDebut;

  @Min(2)
  @Max(100)
  private Integer nombreMembres;
}
