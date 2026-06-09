package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Représentation complète d'une tontine")
@Getter
@Builder
public class TontineResponse {

  @Schema(description = "UUID de la tontine")
  private UUID id;

  @Schema(
      description = "Type de tontine",
      allowableValues = {"ROTATIVE", "EVENEMENTIELLE"})
  private TontineType tontineType;

  @Schema(description = "Nom du groupe de tontine", example = "Tontine Famille Diallo")
  private String nom;

  @Schema(description = "Description du groupe")
  private String description;

  @Schema(description = "Montant fixe de cotisation par cycle en FCFA", example = "5000")
  private BigDecimal montant;

  @Schema(description = "Code de fréquence (FREQUENCE_TONTINE)", example = "MENSUEL")
  private String frequence;

  @Schema(description = "Code de l'ordre des bénéficiaires (ROTATIVE uniquement)", example = "FIXE")
  private String ordreBeneficiaire;

  @Schema(
      description = "Mode de gestion des cycles (ROTATIVE uniquement)",
      example = "AUTOMATIQUE",
      allowableValues = {"AUTOMATIQUE", "MANUEL"})
  private ModeCycle modeCycle;

  @Schema(description = "Date de démarrage du premier cycle", example = "2024-07-01")
  private LocalDate dateDebut;

  @Schema(description = "Nombre total de membres attendus (ROTATIVE)", example = "10")
  private int nombreMembres;

  @Schema(description = "Nombre de gagnants du jackpot par cycle (ROTATIVE)", example = "2")
  private int nombreGagnants;

  @Schema(description = "Nombre de cotisants actuellement actifs dans la tontine", example = "8")
  private int nombreMembresActuels;

  @Schema(
      description = "Statut courant de la tontine",
      allowableValues = {"BROUILLON", "ACTIVE", "SUSPENDUE", "TERMINEE"})
  private TontineStatut statut;

  // ─── Champs EVENEMENTIELLE ──────────────────────────────────────────────

  @Schema(description = "Date cible de l'événement (EVENEMENTIELLE)", example = "2026-03-20")
  private LocalDate dateEcheance;

  @Schema(description = "Nom de l'événement cible", example = "Tabaski 2026")
  private String nomEvenement;

  @Schema(
      description = "true = cotisation libre par le membre, false = montant fixe imposé",
      example = "false")
  private boolean montantLibre;

  @Schema(description = "Montant minimum de cotisation en mode libre (null = aucun plancher)")
  private BigDecimal montantMinimum;

  @Schema(description = "Informations résumées du créateur")
  private CreateurInfo creePar;

  @Schema(description = "Date de création de la tontine")
  private LocalDateTime createdAt;

  @Schema(description = "Date de dernière modification")
  private LocalDateTime updatedAt;

  public static TontineResponse from(Tontine tontine, int nombreMembresActuels) {
    return TontineResponse.builder()
        .id(tontine.getId())
        .tontineType(tontine.getTontineType())
        .nom(tontine.getNom())
        .description(tontine.getDescription())
        .montant(tontine.getMontant())
        .frequence(tontine.getFrequence())
        .ordreBeneficiaire(tontine.getOrdreBeneficiaire())
        .modeCycle(tontine.getModeCycle())
        .dateDebut(tontine.getDateDebut())
        .nombreMembres(tontine.getNombreMembres())
        .nombreGagnants(tontine.getNombreGagnants())
        .nombreMembresActuels(nombreMembresActuels)
        .statut(tontine.getStatut())
        .dateEcheance(tontine.getDateEcheance())
        .nomEvenement(tontine.getNomEvenement())
        .montantLibre(tontine.isMontantLibre())
        .montantMinimum(tontine.getMontantMinimum())
        .creePar(
            tontine.getCreePar() != null
                ? CreateurInfo.builder()
                    .id(tontine.getCreePar().getId())
                    .firstName(tontine.getCreePar().getFirstName())
                    .lastName(tontine.getCreePar().getLastName())
                    .phone(tontine.getCreePar().getPhone())
                    .build()
                : null)
        .createdAt(tontine.getCreatedAt())
        .updatedAt(tontine.getUpdatedAt())
        .build();
  }

  @Schema(description = "Informations résumées du créateur de la tontine")
  @Getter
  @Builder
  public static class CreateurInfo {

    @Schema(description = "UUID interne du créateur")
    private UUID id;

    @Schema(description = "Prénom", example = "Mamadou")
    private String firstName;

    @Schema(description = "Nom de famille", example = "Diallo")
    private String lastName;

    @Schema(description = "Numéro de téléphone normalisé", example = "221783703310")
    private String phone;
  }
}
