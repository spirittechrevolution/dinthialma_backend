package com.africa.dinthialma_backend.auth.service.interfaces;

import com.africa.dinthialma_backend.auth.codeList.ClientType;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserSession;
import com.africa.dinthialma_backend.common.exception.CustomException;
import java.util.Optional;
import java.util.UUID;

/**
 * Service de gestion des sessions PIN (WEB + MOBILE).
 *
 * <p>Une session est créée lors de la première connexion complète (mot de passe). Elle est utilisée
 * pour la connexion rapide PIN : le refresh token Keycloak est stocké chiffré et échangé contre un
 * nouveau JWT à chaque connexion PIN.
 *
 * <p>Une session par (utilisateur × clientType) est maintenue. Si une session existe déjà pour ce
 * couple, elle est mise à jour (refresh token + TTL).
 */
public interface UserSessionService {

  /**
   * Crée ou met à jour la session d'un utilisateur pour un type de client.
   *
   * @param user propriétaire de la session
   * @param clientType WEB ou MOBILE
   * @param refreshToken refresh token Keycloak en clair (sera chiffré avant persistance)
   * @param deviceInfo user-agent (WEB) ou device_id (MOBILE), optionnel
   * @return la session persistée
   */
  UserSession createOrUpdateSession(
      User user, ClientType clientType, String refreshToken, String deviceInfo)
      throws CustomException;

  /**
   * Recherche la session active d'un utilisateur pour un type de client.
   *
   * @return session si elle existe et n'est pas expirée, sinon {@link Optional#empty()}
   */
  Optional<UserSession> findActiveSession(UUID userId, ClientType clientType);

  /**
   * Recherche n'importe quelle session active de l'utilisateur, tous types confondus. Utilisé en
   * fallback PIN quand le clientType demandé n'a pas de session mais un autre en a une.
   *
   * @return la session active la plus récente, sinon {@link Optional#empty()}
   */
  Optional<UserSession> findAnyActiveSession(UUID userId);

  /**
   * Révoque une session spécifique (déconnexion d'un appareil).
   *
   * @param sessionId identifiant de la session à révoquer
   */
  void revokeSession(UUID sessionId) throws CustomException;

  /**
   * Révoque toutes les sessions d'un utilisateur. Utilisé lors du reset PIN, reset mot de passe ou
   * désactivation du compte.
   *
   * @param userId identifiant de l'utilisateur
   */
  void revokeAllSessions(UUID userId);

  /**
   * Décrypte et retourne le refresh token en clair pour un appel Keycloak.
   *
   * @param session session dont on veut extraire le token
   * @return refresh token en clair
   */
  String decryptRefreshToken(UserSession session) throws CustomException;
}
