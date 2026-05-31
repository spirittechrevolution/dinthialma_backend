package com.africa.dinthialma_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête de mise à jour du profil."
            + " La modification est propagée en DB et dans Keycloak.")
@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

  @Schema(description = "Prénom (2 à 50 caractères)", example = "Mamadou")
  @NotBlank(message = "Le prénom est obligatoire")
  @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
  private String firstName;

  @Schema(description = "Nom de famille (2 à 50 caractères)", example = "Diallo")
  @NotBlank(message = "Le nom est obligatoire")
  @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
  private String lastName;

  @Schema(description = "Adresse email (optionnelle)", example = "mamadou@email.com")
  @Email(message = "Format d'email invalide")
  private String email;
}
