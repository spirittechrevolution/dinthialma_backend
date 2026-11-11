package com.africa.dinthialma_backend.me.dto;

import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(
    description =
        "Résultat d'une validation groupée. Chaque cotisation est traitée indépendamment : un"
            + " échec n'annule pas les validations réussies.")
@Getter
@Builder
public class ValidationGroupeeResponse {

  @Schema(description = "Nombre de cotisations distinctes traitées", example = "6")
  private int total;

  @Schema(description = "Nombre de cotisations validées", example = "5")
  private int validees;

  @Schema(description = "Nombre d'échecs", example = "1")
  private int echecs;

  @Schema(description = "Résultat par cotisation, dans l'ordre de la requête")
  private List<Resultat> resultats;

  @Schema(description = "Résultat pour une cotisation")
  @Getter
  @Builder
  public static class Resultat {

    @Schema(description = "UUID de la cotisation")
    private UUID cotisationId;

    @Schema(description = "true si la cotisation a été validée")
    private boolean succes;

    @Schema(description = "Statut après traitement (VALIDE si succès)", nullable = true)
    private CotisationStatut statut;

    @Schema(
        description =
            "Code d'erreur stable en cas d'échec (ex : COTISATION_NOT_PENDING, FORBIDDEN,"
                + " NOT_FOUND)",
        nullable = true)
    private ErrorCode code;

    @Schema(description = "Message d'erreur lisible en cas d'échec", nullable = true)
    private String message;
  }
}
