package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
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

  @Schema(description = "Nom du groupe de tontine", example = "Tontine Famille Diallo")
  private String nom;

  @Schema(description = "Description du groupe")
  private String description;

  @Schema(description = "Montant de cotisation par membre par cycle en FCFA", example = "5000")
  private BigDecimal montant;

  @Schema(description = "Code de fréquence (FREQUENCE_TONTINE)", example = "MENSUEL")
  private String frequence;

  @Schema(description = "Code de l'ordre des bénéficiaires (ORDRE_BENEFICIAIRE)", example = "FIXE")
  private String ordreBeneficiaire;

  @Schema(
      description = "Mode de gestion des cycles",
      example = "AUTOMATIQUE",
      allowableValues = {"AUTOMATIQUE", "MANUEL"})
  private ModeCycle modeCycle;

  @Schema(description = "Date de démarrage du premier cycle", example = "2024-07-01")
  private LocalDate dateDebut;

  @Schema(description = "Nombre total de membres attendus", example = "10")
  private int nombreMembres;

  @Schema(description = "Nombre de gagnants du jackpot par cycle", example = "2")
  private int nombreGagnants;

  @Schema(description = "Nombre de cotisants actuellement actifs dans la tontine", example = "8")
  private int nombreMembresActuels;

  @Schema(
      description = "Statut courant de la tontine",
      allowableValues = {"BROUILLON", "ACTIVE", "SUSPENDUE", "TERMINEE"})
  private TontineStatut statut;

  @Schema(description = "Informations résumées du créateur")
  private CreateurInfo creePar;

  @Schema(description = "Date de création de la tontine")
  private LocalDateTime createdAt;

  @Schema(description = "Date de dernière modification")
  private LocalDateTime updatedAt;

  public static TontineResponse from(Tontine tontine, int nombreMembresActuels) {
    return TontineResponse.builder()
        .id(tontine.getId())
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
