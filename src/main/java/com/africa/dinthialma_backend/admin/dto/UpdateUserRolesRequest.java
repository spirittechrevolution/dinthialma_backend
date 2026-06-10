package com.africa.dinthialma_backend.admin.dto;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Schema(
    description =
        "Corps de la requête de remplacement des rôles d'un utilisateur (replace-all idempotent)."
            + " Les rôles présents dans roles sont ajoutés, les rôles absents sont retirés."
            + " Le rôle USER ne peut jamais être retiré.")
@Getter
@Setter
public class UpdateUserRolesRequest {

  @Schema(
      description =
          "Nouvel ensemble de rôles souhaité."
              + " USER est toujours conservé même s'il n'est pas dans la liste.",
      example = "[\"USER\", \"ADMIN\", \"MEMBER\"]",
      allowableValues = {"USER", "ADMIN", "MEMBER", "SUPER_ADMIN"})
  @NotEmpty
  private Set<UserRole> roles;
}
