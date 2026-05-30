package com.africa.dinthialma_backend.member.dto;

import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Corps de la requête de modification du statut d'un cotisant")
@Getter
@Setter
public class UpdateMembreStatutRequest {

  @Schema(
      description =
          "Nouveau statut du cotisant :\n"
              + "- ACTIF : activer ou réactiver un membre suspendu\n"
              + "- SUSPENDU : suspendre temporairement (ne peut plus cotiser)\n"
              + "- SORTI : retrait définitif (irréversible sans intervention SUPER_ADMIN)",
      example = "SUSPENDU",
      allowableValues = {"ACTIF", "SUSPENDU", "SORTI"})
  @NotNull
  private MembreStatut statut;

  @Schema(
      description = "Motif optionnel de la modification (pour l'historique)",
      example = "Membre parti en voyage")
  private String motif;
}
