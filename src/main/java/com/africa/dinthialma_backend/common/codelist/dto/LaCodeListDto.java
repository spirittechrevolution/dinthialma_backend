package com.africa.dinthialma_backend.common.codelist.dto;

import com.africa.dinthialma_backend.common.codelist.entity.LaCodeList;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO utilisé à la fois en entrée (création / mise à jour) et en sortie (réponse API). */
@Schema(
    description =
        "Entrée d'un référentiel de valeurs (code list). Utilisé pour peupler les dropdowns"
            + " frontend sans recompiler l'application. Chaque entrée appartient à un type"
            + " (FREQUENCE_TONTINE, METHODE_PAIEMENT…) et possède une valeur technique (code)"
            + " ainsi qu'un libellé lisible.")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LaCodeListDto {

  @Schema(
      description = "UUID de l'entrée (null à la création)",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID id;

  @Schema(
      description = "Famille du référentiel",
      example = "FREQUENCE_TONTINE",
      allowableValues = {
        "FREQUENCE_TONTINE",
        "METHODE_PAIEMENT",
        "STATUT_COTISATION",
        "ORDRE_BENEFICIAIRE",
        "ROLE_INTERNE"
      })
  @NotBlank(message = "Le type est obligatoire")
  @Size(max = 100, message = "Le type ne peut pas dépasser 100 caractères")
  private String type;

  @Schema(description = "Valeur technique utilisée dans le code", example = "MENSUEL")
  @NotBlank(message = "La valeur est obligatoire")
  @Size(max = 100, message = "La valeur ne peut pas dépasser 100 caractères")
  private String value;

  @Schema(description = "Libellé lisible affiché en frontend", example = "Chaque mois")
  @NotBlank(message = "La description est obligatoire")
  @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
  private String description;

  @Schema(
      description = "true = valeur initialisée par le système (non supprimable via l'API)",
      example = "true")
  private boolean isSystemAssign;

  // ── Conversion entité → DTO ───────────────────────────────────────

  public static LaCodeListDto from(LaCodeList entity) {
    return LaCodeListDto.builder()
        .id(entity.getId())
        .type(entity.getType())
        .value(entity.getValue())
        .description(entity.getDescription())
        .isSystemAssign(entity.isSystemAssign())
        .build();
  }

  // ── Conversion DTO → entité ───────────────────────────────────────

  public LaCodeList toEntity() {
    return LaCodeList.builder()
        .type(this.type)
        .value(this.value)
        .description(this.description)
        .isSystemAssign(this.isSystemAssign)
        .build();
  }

  /** Conversion DTO → entité avec id (pour les mises à jour). */
  public LaCodeList toEntity(UUID id) {
    LaCodeList entity = toEntity();
    // L'id est set via le builder parent BaseEntity
    return LaCodeList.builder()
        .type(this.type)
        .value(this.value)
        .description(this.description)
        .isSystemAssign(this.isSystemAssign)
        .build();
  }
}
