package com.africa.dinthialma_backend.auth.service.impl;

import com.africa.dinthialma_backend.auth.config.KeycloakProperties;
import com.africa.dinthialma_backend.auth.dto.LoginResponse;
import com.africa.dinthialma_backend.auth.dto.RegisterCompleteRequest;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.UnAuthorizedException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Implémentation du service Keycloak – authentification et gestion des utilisateurs.
 *
 * <p>Deux canaux d'accès à Keycloak :
 *
 * <ul>
 *   <li>{@link RestClient} → endpoint token/logout/refresh (OIDC standard)
 *   <li>{@link Keycloak} Admin Client → CRUD utilisateurs + rôles (API Admin)
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAuthServiceImpl implements KeycloakAuthService {

  private final KeycloakProperties keycloakProperties;
  private final Keycloak keycloakAdminClient;
  private final RestClient restClient = RestClient.create();

  // ─── Authentification ─────────────────────────────────────────────

  @Override
  public LoginResponse login(String username, String password) throws CustomException {
    log.debug("Tentative de connexion Keycloak pour l'utilisateur : {}", username);

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "password");
    formData.add("client_id", keycloakProperties.getClientId());
    formData.add("client_secret", keycloakProperties.getClientSecret());
    formData.add("username", username);
    formData.add("password", password);
    formData.add("scope", "openid profile email");
    log.debug("Token URL: {}", buildTokenUrl());
    try {
      LoginResponse response =
          restClient
              .post()
              .uri(buildTokenUrl())
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(formData)
              .retrieve()
              .body(LoginResponse.class);

      log.info("Connexion Keycloak réussie pour l'utilisateur : {}", username);
      return response;

    } catch (HttpClientErrorException e) {
      log.warn(
          "Échec connexion Keycloak pour {} – status: {}, body: {}",
          username,
          e.getStatusCode(),
          e.getResponseBodyAsString());
      if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
        throw new UnAuthorizedException("Identifiants incorrects");
      }
      throw new CustomException(HttpStatus.BAD_GATEWAY, "Erreur lors de la connexion");
    } catch (Exception e) {
      log.error("Erreur inattendue lors de la connexion Keycloak : {}", e.getMessage());
      throw new CustomException(HttpStatus.BAD_GATEWAY, "Service d'authentification indisponible");
    }
  }

  @Override
  public void logout(String refreshToken) throws CustomException {
    log.debug("Déconnexion Keycloak via refresh token");

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("client_id", keycloakProperties.getClientId());
    formData.add("client_secret", keycloakProperties.getClientSecret());
    formData.add("refresh_token", refreshToken);

    try {
      restClient
          .post()
          .uri(buildLogoutUrl())
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(formData)
          .retrieve()
          .toBodilessEntity();

      log.info("Déconnexion Keycloak réussie");

    } catch (Exception e) {
      // Non bloquant : le token Keycloak expire de toute façon
      log.warn("Impossible de révoquer le token Keycloak (non bloquant) : {}", e.getMessage());
    }
  }

  @Override
  public LoginResponse refreshToken(String refreshToken) throws CustomException {
    log.debug("Rafraîchissement du token Keycloak via session PIN");

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "refresh_token");
    formData.add("client_id", keycloakProperties.getClientId());
    formData.add("client_secret", keycloakProperties.getClientSecret());
    formData.add("refresh_token", refreshToken);

    try {
      LoginResponse response =
          restClient
              .post()
              .uri(buildTokenUrl())
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(formData)
              .retrieve()
              .body(LoginResponse.class);

      log.info("Token Keycloak rafraîchi avec succès via session PIN");
      return response;

    } catch (HttpClientErrorException e) {
      log.warn("Impossible de rafraîchir le token Keycloak – status: {}", e.getStatusCode());
      throw new UnAuthorizedException(
          "Session expirée. Veuillez vous reconnecter avec votre mot de passe");
    } catch (Exception e) {
      log.error("Erreur lors du rafraîchissement du token Keycloak : {}", e.getMessage());
      throw new CustomException(HttpStatus.BAD_GATEWAY, "Service d'authentification indisponible");
    }
  }

  // ─── Gestion des utilisateurs (Admin API) ─────────────────────────

  @Override
  public String createUser(RegisterCompleteRequest request) throws CustomException {
    log.debug("Création utilisateur Keycloak : {}", request.getPhone());

    UserRepresentation userRep = new UserRepresentation();
    userRep.setUsername(request.getPhone());
    userRep.setFirstName(request.getFirstName());
    userRep.setLastName(request.getLastName());
    userRep.setEnabled(true);
    userRep.setEmailVerified(true);

    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      userRep.setEmail(request.getEmail());
    }

    // Attribut custom "phone" – déclaré dans Realm settings → User profile
    userRep.setAttributes(Map.of("phone", List.of(request.getPhone())));

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(request.getPassword());
    credential.setTemporary(false);
    userRep.setCredentials(List.of(credential));

    try (Response response =
        keycloakAdminClient.realm(keycloakProperties.getRealm()).users().create(userRep)) {

      if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
        String errorBody = response.readEntity(String.class);
        log.error(
            "Keycloak a refusé la création de l'utilisateur {} – status: {} – body: {}",
            request.getPhone(),
            response.getStatus(),
            errorBody);
        throw new CustomException(
            HttpStatus.BAD_GATEWAY, "Impossible de créer le compte dans l'IAM");
      }

      String keycloakId = CreatedResponseUtil.getCreatedId(response);
      log.info("Utilisateur Keycloak créé avec succès : keycloakId={}", keycloakId);
      return keycloakId;

    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error("Erreur lors de la création de l'utilisateur Keycloak : {}", e.getMessage());
      throw new CustomException(HttpStatus.BAD_GATEWAY, "Erreur lors de la création du compte");
    }
  }

  @Override
  public void resetPassword(String keycloakId, String newPassword) throws CustomException {
    log.debug("Reset mot de passe Keycloak pour keycloakId={}", keycloakId);

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(newPassword);
    credential.setTemporary(false);

    try {
      keycloakAdminClient
          .realm(keycloakProperties.getRealm())
          .users()
          .get(keycloakId)
          .resetPassword(credential);

      log.info("Mot de passe Keycloak réinitialisé pour keycloakId={}", keycloakId);

    } catch (Exception e) {
      log.error("Échec reset mot de passe Keycloak pour {} : {}", keycloakId, e.getMessage());
      throw new CustomException(
          HttpStatus.BAD_GATEWAY, "Impossible de réinitialiser le mot de passe");
    }
  }

  @Override
  public void assignRole(String keycloakId, String roleName) throws CustomException {
    log.debug("Attribution du rôle '{}' à keycloakId={}", roleName, keycloakId);

    try {
      RoleRepresentation roleRep =
          keycloakAdminClient
              .realm(keycloakProperties.getRealm())
              .roles()
              .get(roleName)
              .toRepresentation();

      UsersResource usersResource =
          keycloakAdminClient.realm(keycloakProperties.getRealm()).users();

      usersResource.get(keycloakId).roles().realmLevel().add(List.of(roleRep));

      log.info("Rôle '{}' attribué à keycloakId={}", roleName, keycloakId);

    } catch (Exception e) {
      log.error("Échec attribution rôle '{}' à {} : {}", roleName, keycloakId, e.getMessage());
      throw new CustomException(
          HttpStatus.BAD_GATEWAY, "Impossible d'attribuer le rôle dans l'IAM");
    }
  }

  @Override
  public void deleteUser(String keycloakId) throws CustomException {
    log.debug("Suppression utilisateur Keycloak : keycloakId={}", keycloakId);

    try {
      keycloakAdminClient.realm(keycloakProperties.getRealm()).users().get(keycloakId).remove();

      log.info("Utilisateur Keycloak supprimé : keycloakId={}", keycloakId);

    } catch (Exception e) {
      log.error("Échec suppression utilisateur Keycloak {} : {}", keycloakId, e.getMessage());
      throw new CustomException(
          HttpStatus.BAD_GATEWAY, "Impossible de supprimer l'utilisateur dans l'IAM");
    }
  }

  @Override
  public void disableUser(String keycloakId) throws CustomException {
    log.debug("Désactivation utilisateur Keycloak : keycloakId={}", keycloakId);
    try {
      UserRepresentation userRep =
          keycloakAdminClient
              .realm(keycloakProperties.getRealm())
              .users()
              .get(keycloakId)
              .toRepresentation();
      userRep.setEnabled(false);
      keycloakAdminClient
          .realm(keycloakProperties.getRealm())
          .users()
          .get(keycloakId)
          .update(userRep);
      log.info("Utilisateur Keycloak désactivé : keycloakId={}", keycloakId);
    } catch (Exception e) {
      log.error("Échec désactivation Keycloak {} : {}", keycloakId, e.getMessage());
      throw new CustomException(
          HttpStatus.BAD_GATEWAY, "Impossible de désactiver le compte dans l'IAM");
    }
  }

  @Override
  public void enableUser(String keycloakId) throws CustomException {
    log.debug("Réactivation utilisateur Keycloak : keycloakId={}", keycloakId);
    try {
      UserRepresentation userRep =
          keycloakAdminClient
              .realm(keycloakProperties.getRealm())
              .users()
              .get(keycloakId)
              .toRepresentation();
      userRep.setEnabled(true);
      keycloakAdminClient
          .realm(keycloakProperties.getRealm())
          .users()
          .get(keycloakId)
          .update(userRep);
      log.info("Utilisateur Keycloak réactivé : keycloakId={}", keycloakId);
    } catch (Exception e) {
      log.error("Échec réactivation Keycloak {} : {}", keycloakId, e.getMessage());
      throw new CustomException(
          HttpStatus.BAD_GATEWAY, "Impossible de réactiver le compte dans l'IAM");
    }
  }

  @Override
  public void removeRole(String keycloakId, String roleName) throws CustomException {
    log.debug("Retrait du rôle '{}' de keycloakId={}", roleName, keycloakId);
    try {
      RoleRepresentation roleRep =
          keycloakAdminClient
              .realm(keycloakProperties.getRealm())
              .roles()
              .get(roleName)
              .toRepresentation();
      keycloakAdminClient
          .realm(keycloakProperties.getRealm())
          .users()
          .get(keycloakId)
          .roles()
          .realmLevel()
          .remove(List.of(roleRep));
      log.info("Rôle '{}' retiré de keycloakId={}", roleName, keycloakId);
    } catch (Exception e) {
      log.error("Échec retrait rôle '{}' de {} : {}", roleName, keycloakId, e.getMessage());
      throw new CustomException(HttpStatus.BAD_GATEWAY, "Impossible de retirer le rôle dans l'IAM");
    }
  }

  // ─── Helpers ──────────────────────────────────────────────────────

  private String buildTokenUrl() {
    return keycloakProperties.getAuthServerUrl()
        + "/realms/"
        + keycloakProperties.getRealm()
        + "/protocol/openid-connect/token";
  }

  private String buildLogoutUrl() {
    return keycloakProperties.getAuthServerUrl()
        + "/realms/"
        + keycloakProperties.getRealm()
        + "/protocol/openid-connect/logout";
  }
}
