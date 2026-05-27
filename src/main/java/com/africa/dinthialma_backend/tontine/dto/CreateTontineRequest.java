package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Corps de requête pour la création d'une tontine. */
@Getter
@Setter
public class CreateTontineRequest {

  /** Nom du groupe de tontine. */
  @NotBlank
  @Size(max = 150)
  private String nom;

  /** Description optionnelle. */
  private String description;

  /** Montant de cotisation par membre par cycle (FCFA). */
  @NotNull @Positive private BigDecimal montant;

  /**
   * Fréquence de cotisation – réf. code list type {@code FREQUENCE_TONTINE}.
   *
   * <p>Ex : {@code HEBDOMADAIRE}, {@code MENSUEL}, {@code TRIMESTRIEL}.
   */
  @NotBlank private String frequence;

  /**
   * Ordre de désignation des bénéficiaires – réf. code list type {@code ORDRE_BENEFICIAIRE}.
   *
   * <p>Ex : {@code FIXE}, {@code ALEATOIRE}.
   */
  @NotBlank private String ordreBeneficiaire;

  /**
   * Mode de gestion des cycles.
   *
   * <ul>
   *   <li>{@code AUTOMATIQUE} – tous les cycles générés à l'activation.
   *   <li>{@code MANUEL} – l'admin ouvre chaque cycle au moment voulu.
   * </ul>
   */
  @NotNull private ModeCycle modeCycle;

  /** Date de démarrage du premier cycle. */
  @NotNull @FutureOrPresent private LocalDate dateDebut;

  /** Nombre total de membres attendus (= nombre de cycles à générer). Minimum 2, maximum 100. */
  @Min(2)
  @Max(100)
  private int nombreMembres;
}
