package com.africa.dinthialma_backend.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
            + "(+ `ordreJackpot`). Un compte PRE_ENROLLED est créé avec ces informations.\n\n"
            + "**Ajout depuis un QR scanné :** envoyer `qrToken` (`m_…`, tel que lu dans le QR)"
            + " **à la place** de `phone`. Le numéro n'est jamais exposé au gestionnaire avant"
            + " l'ajout. Exactement un des deux est requis (`MEMBER_IDENTIFIER_REQUIRED`).")
@Getter
@Setter
public class AddMembreRequest {

  @Schema(
      description =
          "Numéro de téléphone (format international, avec ou sans le +)."
              + " Obligatoire sauf si `qrToken` est fourni.",
      example = "221770000001")
  private String phone;

  @Schema(
      description =
          "Jeton du QR membre scanné (`m_…`), alternative à `phone`. QR inconnu ou révoqué"
              + " → 404. Le compte doit déjà exister (pas de création PRE_ENROLLED par QR).",
      example = "m_Q2hhbmdlTWVQbGVhc2U")
  private String qrToken;

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
