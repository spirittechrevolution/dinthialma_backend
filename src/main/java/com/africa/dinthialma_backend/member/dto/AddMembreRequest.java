package com.africa.dinthialma_backend.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête d'ajout d'un cotisant à une tontine.\n\n"
            + "**Flux recommandé (2 étapes) :**\n"
            + "1. `GET /v1/users/search?phone=xxx` pour vérifier si le numéro existe.\n"
            + "2a. Numéro **trouvé** → envoyer uniquement `phone` (+ `ordreJackpot`). "
            + "Le nom est déjà connu du système.\n"
            + "2b. Numéro **introuvable** → envoyer `phone` + `firstName` + `lastName` "
            + "(+ `ordreJackpot`). Un compte PRE_ENROLLED est créé avec ces informations.")
@Getter
@Setter
public class AddMembreRequest {

  @Schema(
      description = "Numéro de téléphone (format international, avec ou sans le +).",
      example = "221770000001")
  @NotBlank
  private String phone;

  @Schema(
      description =
          "Prénom du membre. **Obligatoire uniquement si le numéro est inconnu** (création"
              + " d'un compte PRE_ENROLLED). Ignoré si le numéro appartient à un compte ACTIVE.",
      example = "Fatou")
  private String firstName;

  @Schema(
      description =
          "Nom de famille du membre. **Obligatoire uniquement si le numéro est inconnu**"
              + " (création d'un compte PRE_ENROLLED). Ignoré si le numéro appartient à un compte ACTIVE.",
      example = "Sow")
  private String lastName;

  @Schema(
      description =
          "Position dans la rotation des jackpots (1 = premier bénéficiaire)."
              + " Optionnel – si null, attribué automatiquement en fin de liste.",
      example = "5")
  private Integer ordreJackpot;
}
