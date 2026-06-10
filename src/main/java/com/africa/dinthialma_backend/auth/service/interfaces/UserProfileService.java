package com.africa.dinthialma_backend.auth.service.interfaces;

import com.africa.dinthialma_backend.auth.dto.PhoneChangeRequest;
import com.africa.dinthialma_backend.auth.dto.PhoneChangeVerifyRequest;
import com.africa.dinthialma_backend.auth.dto.UpdateProfileRequest;
import com.africa.dinthialma_backend.auth.dto.UserProfileResponse;
import com.africa.dinthialma_backend.common.exception.CustomException;

/** Service de gestion du profil utilisateur. */
public interface UserProfileService {

  /** Récupérer le profil de l'utilisateur connecté. */
  UserProfileResponse getProfile(String keycloakId) throws CustomException;

  /** Mettre à jour le prénom, nom et email (dans DB + Keycloak). */
  UserProfileResponse updateProfile(String keycloakId, UpdateProfileRequest request)
      throws CustomException;

  /**
   * Étape 1 – Demander le changement de numéro : envoie un OTP au nouveau numéro. Les
   * tontines/memberships sont liés à l'UUID utilisateur → aucune relation cassée.
   */
  void requestPhoneChange(String keycloakId, PhoneChangeRequest request) throws CustomException;

  /**
   * Étape 2 – Vérifier l'OTP et appliquer le changement : met à jour Keycloak (username + attribut
   * phone) + DB + révoque toutes les sessions.
   */
  void verifyPhoneChange(String keycloakId, PhoneChangeVerifyRequest request)
      throws CustomException;
}
