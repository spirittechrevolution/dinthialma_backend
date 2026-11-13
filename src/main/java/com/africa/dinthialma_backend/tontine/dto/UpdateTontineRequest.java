package com.africa.dinthialma_backend.tontine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête de mise à jour d'une tontine."
            + " Tous les champs sont optionnels – seuls les champs non nuls sont mis à jour."
            + " Modification autorisée uniquement si la tontine est en statut BROUILLON.")
@Getter
@Setter
public class UpdateTontineRequest {

  @Schema(
      description = "Nouveau nom du groupe (2 à 150 caractères)",
      example = "Tontine Famille Diallo 2024")
  @Size(min = 2, max = 150)
  private String nom;

  @Schema(
      description = "Nouvelle description (null = inchangée)",
      example = "Tontine mensuelle mise à jour")
  private String description;

  @Schema(
      description = "Nouveau montant de cotisation par cycle en FCFA (doit être positif)",
      example = "10000")
  @Positive
  private BigDecimal montant;

  @Schema(
      description =
          "Nouvelle fréquence historique (null = inchangée), traduite en rythme ; ignorée si"
              + " rythmeIntervalle ou rythmeUnite est fourni",
      example = "HEBDOMADAIRE")
  private String frequence;

  // ─── Rythme et échéance (B26) ───────────────────────────────────────────

  @Schema(
      description =
          "Nouvel intervalle du rythme (null = inchangé). Rythme modifiable seulement en"
              + " BROUILLON (sinon 400 RYTHME_NON_MODIFIABLE).",
      example = "3")
  private Integer rythmeIntervalle;

  @Schema(
      description = "Nouvelle unité du rythme (null = inchangée)",
      allowableValues = {"JOUR", "SEMAINE", "MOIS"},
      example = "JOUR")
  private String rythmeUnite;

  @Schema(
      description = "Nouveau jour d'échéance (null = inchangé, \"\" = aucun jour imposé)",
      example = "DIMANCHE")
  private String jourEcheance;

  @Schema(description = "Nouvelle heure d'échéance HH:mm (null = inchangée)", example = "20:00")
  private String heureEcheance;

  @Schema(description = "Rappels avant échéance (champ absent = inchangé)")
  @Valid
  private RappelsEcheance rappels;

  @Schema(
      description =
          "Nouvel ordre des bénéficiaires : ROTATION, ALEATOIRE, MANUEL ou HYBRIDE (null ="
              + " inchangé ; autre valeur → 400 INVALID_PARAMETER)",
      allowableValues = {"ROTATION", "ALEATOIRE", "MANUEL", "HYBRIDE"},
      example = "HYBRIDE")
  private String ordreBeneficiaire;

  @Schema(description = "Nouvelle date de démarrage (null = inchangée)", example = "2024-08-01")
  private LocalDate dateDebut;

  @Schema(
      description = "Nouveau nombre de membres attendus (min 2, max 100, null = inchangé)",
      example = "15",
      minimum = "2",
      maximum = "100")
  @Min(2)
  @Max(100)
  private Integer nombreMembres;

  @Schema(
      description = "Nouveau nombre de gagnants par cycle (min 1, null = inchangé)",
      example = "2",
      minimum = "1")
  @Min(1)
  private Integer nombreGagnants;

  // ─── Champs EVENEMENTIELLE ──────────────────────────────────────────────

  @Schema(
      description = "Nouvelle date d'échéance de l'événement (EVENEMENTIELLE, doit être future)",
      example = "2026-06-06")
  @Future
  private LocalDate dateEcheance;

  @Schema(description = "Nouveau nom de l'événement (max 200 caractères)", example = "Korité 2026")
  @Size(max = 200)
  private String nomEvenement;

  @Schema(
      description = "Nouveau montant minimum de cotisation (EVENEMENTIELLE libre, null = inchangé)",
      example = "1000")
  @Positive
  private BigDecimal montantMinimum;

  @Schema(description = "Basculer entre cotisation libre et cotisation fixe (null = inchangé)")
  private Boolean montantLibre;
}
