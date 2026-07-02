package com.africa.dinthialma_backend.contribution.dto;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * Total cumulé des cotisations validées d'un membre, tous cycles confondus, pour une tontine
 * donnée.
 */
@Schema(
    description =
        "Total cotisé par membre (tous cycles confondus, cotisations VALIDÉES uniquement)")
@Getter
@Builder
public class MembreTotalCotisationResponse {

  @Schema(description = "UUID du membership (tontine_membres.id)")
  private UUID membreId;

  @Schema(description = "UUID de l'utilisateur")
  private UUID userId;

  @Schema(description = "Prénom", example = "Fatou")
  private String firstName;

  @Schema(description = "Nom de famille", example = "Sow")
  private String lastName;

  @Schema(description = "Numéro de téléphone normalisé", example = "221770000001")
  private String phone;

  @Schema(description = "Statut du compte utilisateur (ACTIVE ou PRE_ENROLLED)")
  private AccountStatus accountStatus;

  @Schema(description = "Statut du membre dans la tontine (ACTIF, SUSPENDU)")
  private MembreStatut statutMembre;

  @Schema(description = "Ordre de passage au jackpot (null si non défini)")
  private Integer ordreJackpot;

  @Schema(description = "Total cotisé (VALIDÉ) en FCFA, tous cycles confondus", example = "150000")
  private BigDecimal totalCotise;

  @Schema(description = "Nombre de cotisations validées prises en compte dans le total")
  private long nombreCotisationsValidees;

  public static MembreTotalCotisationResponse from(
      TontineMembre membre, BigDecimal totalCotise, long nombreCotisationsValidees) {
    var u = membre.getUser();

    return MembreTotalCotisationResponse.builder()
        .membreId(membre.getId())
        .userId(u.getId())
        .firstName(u.getFirstName())
        .lastName(u.getLastName())
        .phone(u.getPhone())
        .accountStatus(u.getAccountStatus())
        .statutMembre(membre.getStatut())
        .ordreJackpot(membre.getOrdreJackpot())
        .totalCotise(totalCotise != null ? totalCotise : BigDecimal.ZERO)
        .nombreCotisationsValidees(nombreCotisationsValidees)
        .build();
  }
}
