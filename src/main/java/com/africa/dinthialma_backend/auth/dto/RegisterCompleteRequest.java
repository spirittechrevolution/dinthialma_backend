package com.africa.dinthialma_backend.auth.dto;

import com.africa.dinthialma_backend.auth.codeList.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
    description =
        "Corps de la 3ème étape d'inscription : crée le compte après vérification OTP."
            + " Le numéro doit correspondre au téléphone ayant reçu l'OTP.")
@Getter
@Setter
@NoArgsConstructor
public class RegisterCompleteRequest {

  @Schema(
      description = "Numéro de téléphone au format international (doit avoir reçu l'OTP)",
      example = "+221783703310")
  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Format international requis")
  private String phone;

  @Schema(description = "Prénom (2 à 100 caractères)", example = "Mamadou")
  @NotBlank(message = "Le prénom est obligatoire")
  @Size(min = 2, max = 100, message = "Le prénom doit contenir entre 2 et 100 caractères")
  private String firstName;

  @Schema(description = "Nom de famille (2 à 100 caractères)", example = "Diallo")
  @NotBlank(message = "Le nom est obligatoire")
  @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
  private String lastName;

  @Schema(description = "Mot de passe (minimum 8 caractères)", example = "MonMDP@2024")
  @NotBlank(message = "Le mot de passe est obligatoire")
  @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
  private String password;

  @Schema(description = "Adresse email (optionnelle)", example = "mamadou.diallo@email.com")
  @Email(message = "Format email invalide")
  private String email;

  @Schema(
      description =
          "Type de client (WEB ou MOBILE). S'il est fourni, l'utilisateur est connecté"
              + " automatiquement : la réponse contient access_token, refresh_token et"
              + " pin_configured. Absent = inscription seule (comportement historique).",
      example = "MOBILE")
  private ClientType clientType;

  @Schema(
      description = "Informations appareil pour la session (optionnel, comme au login)",
      example = "Samsung A14 – Android 14")
  private String deviceInfo;
}
