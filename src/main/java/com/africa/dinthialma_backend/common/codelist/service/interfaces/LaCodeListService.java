package com.africa.dinthialma_backend.common.codelist.service.interfaces;

import com.africa.dinthialma_backend.common.codelist.dto.LaCodeListDto;
import com.africa.dinthialma_backend.common.exception.CustomException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Référentiel de valeurs (listes déroulantes). Toutes les règles d'accès sont portées ici : le
 * contrôleur ne fait qu'exposer le résultat.
 */
public interface LaCodeListService {

  /** Type réservé aux administrateurs, non exposé par l'endpoint public. */
  String TYPE_RESTREINT = "ROLE_INTERNE";

  /**
   * Valeurs actives d'un type, pour l'endpoint public (sans authentification).
   *
   * @throws CustomException 403 si le type est restreint ({@link #TYPE_RESTREINT})
   */
  List<LaCodeListDto> findPublicByType(String type) throws CustomException;

  /** Valeurs actives d'un type, y compris les types restreints (ADMIN ou SUPER_ADMIN). */
  List<LaCodeListDto> findByTypeAdmin(String keycloakId, String type) throws CustomException;

  /** Page de valeurs actives, tous types confondus. */
  Page<LaCodeListDto> findAll(Pageable pageable);

  /** Valeur active par id (404 sinon). */
  LaCodeListDto findById(UUID id) throws CustomException;

  /**
   * Crée une valeur (ADMIN ou SUPER_ADMIN). Le couple type/value doit être unique (409) ; un couple
   * précédemment supprimé est restauré.
   */
  LaCodeListDto create(String keycloakId, LaCodeListDto dto) throws CustomException;

  /** Met à jour une valeur (ADMIN ou SUPER_ADMIN) ; 404 si introuvable, 409 si doublon. */
  LaCodeListDto update(String keycloakId, UUID id, LaCodeListDto dto) throws CustomException;

  /**
   * Suppression logique (SUPER_ADMIN). Les valeurs système ({@code is_system_assign}) sont refusées
   * ({@code CODE_LIST_SYSTEM_VALUE}).
   */
  void delete(String keycloakId, UUID id) throws CustomException;
}
