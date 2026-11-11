package com.africa.dinthialma_backend.auth.service.interfaces;

import com.africa.dinthialma_backend.auth.dto.UserSearchResponse;
import com.africa.dinthialma_backend.common.exception.CustomException;

/** Recherche d'un utilisateur par téléphone (lookup avant ajout d'un membre). */
public interface UserSearchService {

  /**
   * Cherche un compte ACTIVE ou PRE_ENROLLED non supprimé.
   *
   * @param phone numéro au format international, avec ou sans « + »
   * @throws CustomException 404 si aucun compte ne correspond
   */
  UserSearchResponse searchByPhone(String phone) throws CustomException;
}
