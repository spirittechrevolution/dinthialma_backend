package com.africa.dinthialma_backend.auth.controller;

import com.africa.dinthialma_backend.auth.dto.UserSearchResponse;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.PhoneUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de recherche d'utilisateurs.
 *
 * <p>🔒 Tous les endpoints nécessitent un JWT valide.
 */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Recherche d'utilisateurs Dinthialma")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final UserRepository userRepository;

  @GetMapping("/search")
  @Operation(
      summary = "Rechercher un utilisateur par numéro de téléphone",
      description =
          "🔒 Accès : tout utilisateur authentifié.\n\n"
              + "Recherche sur **l'ensemble des comptes non supprimés** : `ACTIVE` et"
              + " `PRE_ENROLLED` sont tous les deux retournés. Les comptes soft-deleted"
              + " (`deletedAt` non null) sont exclus.\n\n"
              + "Utilisé par un gestionnaire de tontine avant d'ajouter un membre (flux 2 étapes) :\n\n"
              + "- **200 / accountStatus = ACTIVE** → utilisateur inscrit, ajout direct possible"
              + " sans resaisir son nom.\n"
              + "- **200 / accountStatus = PRE_ENROLLED** → déjà ajouté à une autre tontine par"
              + " un autre gestionnaire, profil connu, ajout direct possible.\n"
              + "- **404** → numéro totalement inconnu, l'admin doit saisir firstName et lastName"
              + " pour créer un compte pré-inscrit.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Utilisateur trouvé"),
    @ApiResponse(responseCode = "404", description = "Aucun compte avec ce numéro"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou expiré"),
  })
  public ResponseEntity<CustomResponse> searchByPhone(
      @Parameter(
              description = "Numéro de téléphone (format international, avec ou sans le +)",
              example = "221770000001")
          @RequestParam
          String phone)
      throws NotFoundException {

    String normalized = PhoneUtils.normalize(phone);
    User user =
        userRepository
            .findByPhoneAndDeletedAtIsNull(normalized)
            .orElseThrow(
                () -> new NotFoundException(ResponseMessageConstants.USER_SEARCH_NOT_FOUND));

    return ResponseEntity.ok(
        new CustomResponse(
            "success",
            200,
            ResponseMessageConstants.USER_SEARCH_FOUND,
            UserSearchResponse.from(user)));
  }
}
