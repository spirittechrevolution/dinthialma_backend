package com.africa.dinthialma_backend.me.dto;

import com.africa.dinthialma_backend.me.codeList.ModeActivite;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Préférences de l'utilisateur, synchronisées entre ses appareils (B22). Stockées telles quelles en
 * JSONB ({@code users.preferences}) ; les champs inconnus sont ignorés.
 */
@Schema(description = "Préférences synchronisées entre appareils")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PreferencesDto implements Serializable {

  @ArraySchema(
      arraySchema = @Schema(description = "Tontines épinglées en favori (50 max)"),
      schema = @Schema(format = "uuid"))
  @Size(max = 50, message = "Au plus 50 tontines favorites")
  private List<UUID> tontinesFavorites = new ArrayList<>();

  @Schema(
      description = "Dernier mode d'accueil utilisé",
      allowableValues = {"COTISE", "GERE"},
      nullable = true)
  private ModeActivite dernierMode;

  @Schema(description = "Masquer les montants à l'écran", example = "false")
  private boolean masquerMontant;

  @Schema(
      description = "Dernier moyen de paiement utilisé (valeur METHODE_PAIEMENT)",
      example = "WAVE",
      nullable = true)
  @Size(max = 50, message = "Moyen de paiement trop long")
  private String dernierMoyenPaiement;

  @Schema(
      description =
          "Dernière tontine choisie dans l'espace Gestionnaire de l'accueil. Une tontine dont"
              + " l'utilisateur n'est pas (ou plus) gestionnaire est ramenée à null, sans refuser"
              + " les autres préférences.",
      format = "uuid",
      nullable = true)
  private UUID derniereTontineGeree;
}
