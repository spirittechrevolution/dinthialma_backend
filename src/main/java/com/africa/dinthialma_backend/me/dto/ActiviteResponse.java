package com.africa.dinthialma_backend.me.dto;

import com.africa.dinthialma_backend.me.repository.ActiviteRepository.ActiviteRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "Ligne normalisée du fil d'activité (cotisation ou jackpot)")
@Getter
@Builder
public class ActiviteResponse {

  @Schema(description = "UUID de la cotisation ou du gain jackpot")
  private UUID id;

  @Schema(
      description = "Type d'événement",
      allowableValues = {
        "COTISATION_DECLAREE",
        "COTISATION_VALIDEE",
        "COTISATION_ENCAISSEE",
        "COTISATION_EN_RETARD",
        "COTISATION_REFUSEE",
        "JACKPOT_RECU"
      })
  private String type;

  @Schema(
      description = "Date de l'événement (refus, validation ou déclaration ; remise du jackpot)")
  private LocalDateTime date;

  @Schema(description = "Montant en FCFA", example = "5000")
  private BigDecimal montant;

  @Schema(
      description = "EPARGNE = cotisation, GAIN = jackpot reçu",
      allowableValues = {"EPARGNE", "GAIN"})
  private String sens;

  @Schema(
      description = "Statut de la cotisation (null pour un jackpot)",
      allowableValues = {"EN_ATTENTE", "VALIDE", "EN_RETARD", "REFUSE"},
      nullable = true)
  private String statut;

  @Schema(description = "UUID de la tontine")
  private UUID tontineId;

  @Schema(description = "Nom de la tontine", example = "Famille")
  private String tontineNom;

  @Schema(description = "UUID du tour")
  private UUID cycleId;

  @Schema(description = "Numéro du tour", example = "5")
  private Integer numeroCycle;

  @Schema(description = "UUID de la cotisation (null pour un jackpot)", nullable = true)
  private UUID cotisationId;

  @Schema(description = "UUID du membership concerné")
  private UUID membreId;

  @Schema(description = "Prénom et nom du membre concerné (utile en mode GERE)")
  private String membreNom;

  @Schema(description = "Libellé prêt à afficher", example = "Cotisation Famille · tour 5")
  private String libelle;

  public static ActiviteResponse from(ActiviteRow r) {
    String nom =
        ((r.getMembrePrenom() != null ? r.getMembrePrenom() : "")
                + " "
                + (r.getMembreNom() != null ? r.getMembreNom() : ""))
            .trim();
    String prefixe = "JACKPOT_RECU".equals(r.getType()) ? "Jackpot " : "Cotisation ";
    return ActiviteResponse.builder()
        .id(r.getId())
        .type(r.getType())
        .date(r.getDateEvenement())
        .montant(r.getMontant())
        .sens(r.getSens())
        .statut(r.getStatut())
        .tontineId(r.getTontineId())
        .tontineNom(r.getTontineNom())
        .cycleId(r.getCycleId())
        .numeroCycle(r.getNumeroCycle())
        .cotisationId(r.getCotisationId())
        .membreId(r.getMembreId())
        .membreNom(nom.isEmpty() ? null : nom)
        .libelle(prefixe + r.getTontineNom() + " · tour " + r.getNumeroCycle())
        .build();
  }
}
