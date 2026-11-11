package com.africa.dinthialma_backend.me.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.me.dto.ChangePasswordRequest;
import com.africa.dinthialma_backend.me.dto.PreferencesDto;
import com.africa.dinthialma_backend.me.dto.SessionResponse;
import java.util.List;
import java.util.UUID;

/** Gestion du compte de l'utilisateur connecté : mot de passe (B21), appareils (B20). */
public interface CompteService {

  /**
   * Change le mot de passe après vérification de l'ancien.
   *
   * @param sessionIdCourante sid Keycloak du JWT courant (conservé si l'on déconnecte les autres)
   */
  void changerMotDePasse(String keycloakId, String sessionIdCourante, ChangePasswordRequest request)
      throws CustomException;

  /** Mes sessions (appareils connectés), la plus récemment utilisée d'abord. */
  List<SessionResponse> listerSessions(String keycloakId, String sessionIdCourante)
      throws CustomException;

  /** Mes préférences (valeurs par défaut si jamais enregistrées). */
  PreferencesDto getPreferences(String keycloakId) throws CustomException;

  /** Remplace mes préférences (favoris dédoublonnés). */
  PreferencesDto savePreferences(String keycloakId, PreferencesDto preferences)
      throws CustomException;

  /** Déconnexion à distance : révoque la session Keycloak et supprime la session locale. */
  void revoquerSession(String keycloakId, UUID sessionId) throws CustomException;
}
