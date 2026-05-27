package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/** Représentation complète d'une tontine retournée par l'API. */
@Getter
@Builder
public class TontineResponse {

  private UUID id;
  private String nom;
  private String description;
  private BigDecimal montant;
  private String frequence;
  private String ordreBeneficiaire;
  private ModeCycle modeCycle;
  private LocalDate dateDebut;
  private int nombreMembres;
  private int nombreMembresActuels;
  private TontineStatut statut;
  private CreateurInfo creePar;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /**
   * Construit un TontineResponse depuis une entité Tontine.
   *
   * @param tontine entité
   * @param nombreMembresActuels nombre de cotisants actifs
   */
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

  /** Informations résumées du créateur de la tontine. */
  @Getter
  @Builder
  public static class CreateurInfo {
    private UUID id;
    private String firstName;
    private String lastName;
    private String phone;
  }
}
