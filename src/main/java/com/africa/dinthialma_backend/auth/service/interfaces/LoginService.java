package com.africa.dinthialma_backend.auth.service.interfaces;

import com.africa.dinthialma_backend.auth.dto.LoginRequest;
import com.africa.dinthialma_backend.auth.dto.LoginResponse;
import com.africa.dinthialma_backend.common.exception.CustomException;

/** Service de connexion – résout l'identifiant (phone ou email) avant d'appeler Keycloak. */
public interface LoginService {

  /**
   * Authentifie un utilisateur à partir de son {@code username} (téléphone ou email) et son mot de
   * passe.
   *
   * <p>Flux :
   *
   * <ol>
   *   <li>Normalisation : suppression du {@code +} initial ({@code PhoneUtils.normalize}).
   *   <li>Recherche en base par téléphone normalisé, puis par email.
   *   <li>Vérification que le compte est actif et non supprimé.
   *   <li>Appel Keycloak ROPC avec {@code user.getPhone()} (= username exact Keycloak) + password.
   *   <li>Fallback : si absent de la base, tentative directe sur Keycloak (base désynchronisée).
   * </ol>
   *
   * @param request {@code username} (phone ou email) + mot de passe
   * @return tokens JWT retournés par Keycloak
   * @throws CustomException 401 si identifiants incorrects, 403 si compte désactivé
   */
  LoginResponse login(LoginRequest request) throws CustomException;
}
