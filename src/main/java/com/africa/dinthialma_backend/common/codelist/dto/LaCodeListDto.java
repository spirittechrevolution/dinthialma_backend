package com.africa.dinthialma_backend.common.codelist.dto;

import com.africa.dinthialma_backend.common.codelist.entity.LaCodeList;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO utilisé à la fois en entrée (création / mise à jour) et en sortie (réponse API). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LaCodeListDto {

  private UUID id;

  @NotBlank(message = "Le type est obligatoire")
  @Size(max = 100, message = "Le type ne peut pas dépasser 100 caractères")
  private String type;

  @NotBlank(message = "La valeur est obligatoire")
  @Size(max = 100, message = "La valeur ne peut pas dépasser 100 caractères")
  private String value;

  @NotBlank(message = "La description est obligatoire")
  @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
  private String description;

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
