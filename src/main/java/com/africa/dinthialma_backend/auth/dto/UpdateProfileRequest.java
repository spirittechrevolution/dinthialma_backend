package com.africa.dinthialma_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Requête de mise à jour du profil utilisateur (nom, prénom, email). */
@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

  @NotBlank(message = "Le prénom est obligatoire")
  @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
  private String firstName;

  @NotBlank(message = "Le nom est obligatoire")
  @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
  private String lastName;

  @Email(message = "Format d'email invalide")
  private String email;
}
