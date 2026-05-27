package com.africa.dinthialma_backend.auth.service.interfaces;

import com.africa.dinthialma_backend.auth.dto.LoginResponse;
import com.africa.dinthialma_backend.auth.dto.PinLoginRequest;
import com.africa.dinthialma_backend.auth.dto.PinResetRequest;
import com.africa.dinthialma_backend.auth.dto.PinSetupRequest;
import com.africa.dinthialma_backend.common.exception.CustomException;

/**
 * Service de gestion du code PIN (connexion rapide WEB + MOBILE).
 *
 * <p>Pré-requis : l'utilisateur doit s'être connecté au moins une fois via mot de passe pour qu'une
 * {@link com.africa.dinthialma_backend.auth.entity.UserSession} existe.
 *
 * <p>Sécurité :
 *
 * <ul>
 *   <li>PIN haché avec Argon2id (jamais stocké en clair)
 *   <li>Verrouillage automatique après {@code MAX_ATTEMPTS} échecs consécutifs
 *   <li>PINs triviaux refusés (liste noire)
 * </ul>
 */
public interface PinService {

  /**
   * Configure le code PIN pour la première fois (ou après réinitialisation).
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur authentifié (extrait du JWT)
   * @param request PIN + confirmation
   */
  void setupPin(String keycloakId, PinSetupRequest request) throws CustomException;

  /**
   * Authentifie l'utilisateur via son code PIN et retourne de nouveaux tokens JWT.
   *
   * <p>Flux interne :
   *
   * <ol>
   *   <li>Trouver l'utilisateur par téléphone
   *   <li>Vérifier que le PIN est configuré
   *   <li>Vérifier le verrouillage
   *   <li>Valider le code PIN (Argon2id)
   *   <li>Trouver la session active pour ce client
   *   <li>Rafraîchir le token Keycloak
   *   <li>Mettre à jour la session (lastUsedAt + expiresAt)
   * </ol>
   *
   * @param request téléphone + PIN + clientType + deviceInfo (optionnel)
   * @return nouveaux tokens JWT Keycloak
   */
  LoginResponse loginWithPin(PinLoginRequest request) throws CustomException;

  /**
   * Réinitialise le code PIN via OTP SMS. Révoque toutes les sessions actives après reset.
   *
   * @param request téléphone + OTP (vérifié) + nouveau PIN + confirmation
   */
  void resetPin(PinResetRequest request) throws CustomException;

  /**
   * Indique si l'utilisateur a un code PIN configuré.
   *
   * @param keycloakId identifiant Keycloak
   * @return {@code true} si {@code pinHash} est non nul
   */
  boolean isPinConfigured(String keycloakId) throws CustomException;

  /**
   * Envoie l'OTP nécessaire à la réinitialisation du PIN. Anti-énumération : réponse silencieuse si
   * le numéro est inconnu.
   *
   * @param phone numéro de téléphone
   */
  void sendResetPinOtp(String phone) throws CustomException;

  /**
   * Vérifie l'OTP de réinitialisation du PIN (sans le consommer).
   *
   * @param phone numéro de téléphone
   * @param code code OTP reçu par SMS
   */
  void verifyResetPinOtp(String phone, String code) throws CustomException;
}
