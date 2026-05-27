package com.africa.dinthialma_backend.common.codelist.service.interfaces;

import com.africa.dinthialma_backend.common.codelist.entity.LaCodeList;
import com.africa.dinthialma_backend.common.exception.CustomException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LaCodeListService {

  /**
   * Retourne toutes les valeurs pour un type donné.
   *
   * <p>Utilisé par le frontend pour peupler les selects (dropdown). Appel public, pas
   * d'authentification requise.
   *
   * <p>Exemples de types disponibles : {@code FREQUENCE_TONTINE}, {@code METHODE_PAIEMENT}, {@code
   * STATUT_COTISATION}…
   *
   * @param type identifiant du type (ex : {@code "FREQUENCE_TONTINE"})
   * @return liste des entrées correspondantes, triées dans l'ordre d'insertion
   */
  List<LaCodeList> findAllByType(String type);

  /**
   * Retourne une page de code lists tous types confondus.
   *
   * <p>Réservé à l'interface d'administration.
   *
   * @param pageable paramètres de pagination et de tri
   * @return page de code lists
   */
  Page<LaCodeList> findAll(Pageable pageable);

  /**
   * Retourne un code list par son identifiant.
   *
   * @param id UUID du code list
   * @return le code list trouvé
   * @throws CustomException 404 si aucun code list ne correspond à l'id
   */
  LaCodeList findById(UUID id) throws CustomException;

  /**
   * Crée un nouveau code list.
   *
   * <p>Le couple {@code type + value} doit être unique — une violation lève une {@link
   * CustomException} 409.
   *
   * @param codeList entité à persister (sans id)
   * @return le code list créé avec son id assigné
   * @throws CustomException 409 si le couple type/value existe déjà
   */
  LaCodeList createCodeList(LaCodeList codeList) throws CustomException;

  /**
   * Met à jour un code list existant.
   *
   * @param codeList entité avec les nouvelles valeurs (l'id doit correspondre au paramètre {@code
   *     id})
   * @param id UUID du code list à mettre à jour
   * @return le code list mis à jour
   * @throws CustomException 404 si introuvable · 409 si doublon type/value
   */
  LaCodeList updateCodeList(LaCodeList codeList, UUID id) throws CustomException;
}
