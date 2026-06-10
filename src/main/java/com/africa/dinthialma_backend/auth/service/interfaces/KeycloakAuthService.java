package com.africa.dinthialma_backend.auth.service.interfaces;

import com.africa.dinthialma_backend.auth.dto.LoginResponse;
import com.africa.dinthialma_backend.auth.dto.RegisterCompleteRequest;
import com.africa.dinthialma_backend.common.exception.CustomException;

/** Service d'interaction avec Keycloak – authentification et gestion des utilisateurs. */
public interface KeycloakAuthService {

  /**
   * Authentifie un utilisateur auprès de Keycloak via username/password.
   *
   * @param username identifiant Keycloak (téléphone ou email)
   * @param password mot de passe
   * @return LoginResponse avec les tokens JWT
   */
  LoginResponse login(String username, String password) throws CustomException;

  /**
   * Invalide la session Keycloak via le refresh token.
   *
   * @param refreshToken token de rafraîchissement
   */
  void logout(String refreshToken) throws CustomException;

  /**
   * Crée un utilisateur dans Keycloak et lui assigne le rôle MEMBER.
   *
   * @param request données d'inscription
   * @return Keycloak ID du nouvel utilisateur (UUID)
   */
  String createUser(RegisterCompleteRequest request) throws CustomException;

  /**
   * Met à jour le mot de passe d'un utilisateur Keycloak.
   *
   * @param keycloakId UUID Keycloak de l'utilisateur
   * @param newPassword nouveau mot de passe
   */
  void resetPassword(String keycloakId, String newPassword) throws CustomException;

  /**
   * Assigne un rôle realm à un utilisateur Keycloak.
   *
   * @param keycloakId UUID Keycloak de l'utilisateur
   * @param roleName nom du rôle realm (ex: DINTHIALMA_ADMIN)
   */
  void assignRole(String keycloakId, String roleName) throws CustomException;

  /**
   * Supprime un utilisateur de Keycloak.
   *
   * @param keycloakId UUID Keycloak de l'utilisateur
   */
  void deleteUser(String keycloakId) throws CustomException;

  /**
   * Rafraîchit les tokens Keycloak à partir d'un refresh token valide. Utilisé par la connexion PIN
   * : PIN valide → session → nouveau JWT.
   *
   * @param refreshToken refresh token Keycloak (valeur en clair)
   * @return nouveaux tokens JWT
   */
  LoginResponse refreshToken(String refreshToken) throws CustomException;

  /**
   * Désactive un utilisateur dans Keycloak ({@code enabled = false}).
   *
   * @param keycloakId UUID Keycloak de l'utilisateur
   */
  void disableUser(String keycloakId) throws CustomException;

  /**
   * Réactive un utilisateur dans Keycloak ({@code enabled = true}).
   *
   * @param keycloakId UUID Keycloak de l'utilisateur
   */
  void enableUser(String keycloakId) throws CustomException;

  /**
   * Retire un rôle realm d'un utilisateur Keycloak.
   *
   * @param keycloakId UUID Keycloak de l'utilisateur
   * @param roleName nom du rôle realm (ex: DINTHIALMA_ADMIN)
   */
  void removeRole(String keycloakId, String roleName) throws CustomException;
}
