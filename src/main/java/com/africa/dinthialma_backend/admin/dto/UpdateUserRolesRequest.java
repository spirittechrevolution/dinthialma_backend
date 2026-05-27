package com.africa.dinthialma_backend.admin.dto;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Corps de requête pour remplacer l'ensemble des rôles d'un utilisateur.
 *
 * <p>Sémantique : replace-all (idempotent). Les rôles présents dans {@code roles} sont ajoutés, les
 * rôles absents sont retirés. Le rôle {@link UserRole#USER} ne peut pas être retiré.
 */
@Getter
@Setter
public class UpdateUserRolesRequest {

  /** Nouvel ensemble de rôles souhaité. Doit contenir au minimum {@link UserRole#USER}. */
  @NotEmpty private Set<UserRole> roles;
}
