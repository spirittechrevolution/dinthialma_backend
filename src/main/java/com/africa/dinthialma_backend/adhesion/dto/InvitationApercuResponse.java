package com.africa.dinthialma_backend.adhesion.dto;

import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.codeList.UniteRythme;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(
    description =
        "Aperçu public d'une tontine via son code d'invitation. Aucune donnée personnelle des"
            + " membres ; seul le prénom du gestionnaire est affiché.")
@Getter
@Builder
public class InvitationApercuResponse {

  @Schema(description = "Code d'invitation", example = "7Q2K-M9XD")
  private String code;

  @Schema(description = "Nom de la tontine", example = "Famille")
  private String nom;

  @Schema(
      description = "Type",
      allowableValues = {"ROTATIVE", "EVENEMENTIELLE"})
  private TontineType tontineType;

  @Schema(description = "Montant de cotisation (FCFA) – null si montant libre", nullable = true)
  private BigDecimal montant;

  @Schema(description = "Fréquence", example = "MENSUEL")
  private String frequence;

  @Schema(description = "« Tous les N … » (B26)", example = "1")
  private Integer rythmeIntervalle;

  @Schema(description = "Unité du rythme (B26)", example = "MOIS")
  private UniteRythme rythmeUnite;

  @Schema(description = "Jour d'échéance (LUNDI…DIMANCHE, 1…31, DERNIER)", example = "30")
  private String jourEcheance;

  @Schema(description = "Heure limite de paiement HH:mm (Dakar)", example = "23:59")
  private String heureEcheance;

  @Schema(description = "true = montant libre (EVENEMENTIELLE)")
  private boolean montantLibre;

  @Schema(description = "Plancher en montant libre", nullable = true)
  private BigDecimal montantMinimum;

  @Schema(description = "Nom de l'événement (EVENEMENTIELLE)", nullable = true)
  private String nomEvenement;

  @Schema(description = "Date d'échéance (EVENEMENTIELLE)", nullable = true)
  private LocalDate dateEcheance;

  @Schema(description = "Date de démarrage")
  private LocalDate dateDebut;

  @Schema(
      description = "Places restantes (ROTATIVE à effectif fixé) – null si sans limite",
      nullable = true)
  private Integer placesRestantes;

  @Schema(description = "Prénom du gestionnaire", example = "Moussa")
  private String gestionnairePrenom;

  @Schema(
      description =
          "true si l'on peut encore demander à rejoindre (tontine BROUILLON ou ACTIVE, non"
              + " complète)")
  private boolean ouverte;

  // ─── Renseignés uniquement si un JWT valide est envoyé (absents sinon) ───

  @Schema(
      description =
          "Appelant déjà membre de la tontine (actif ou suspendu). Absent sans JWT valide.",
      nullable = true)
  private Boolean dejaMembre;

  @Schema(
      description = "Appelant gestionnaire (créateur) de la tontine. Absent sans JWT valide.",
      nullable = true)
  private Boolean estGestionnaire;

  @Schema(
      description =
          "Appelant ayant déjà une demande d'adhésion EN_ATTENTE sur cette tontine. Absent sans"
              + " JWT valide.",
      nullable = true)
  private Boolean demandeEnAttente;

  @Schema(
      description =
          "Identifiant de la tontine, renseigné seulement si l'appelant en est membre ou"
              + " gestionnaire (pour ouvrir sa fiche). Absent sinon.",
      nullable = true)
  private UUID tontineId;
}
