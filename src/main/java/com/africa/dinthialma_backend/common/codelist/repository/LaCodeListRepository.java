package com.africa.dinthialma_backend.common.codelist.repository;

import com.africa.dinthialma_backend.common.codelist.entity.LaCodeList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LaCodeListRepository extends CrudRepository<LaCodeList, UUID> {

  /** Retourne toutes les valeurs d'un type donné (ex : FREQUENCE_TONTINE). */
  List<LaCodeList> findAllByType(String type);

  /** Recherche une valeur unique par type + value (pour les contraintes d'unicité). */
  Optional<LaCodeList> findByTypeAndValue(String type, String value);

  /** Liste paginée de tous les référentiels (admin). */
  Page<LaCodeList> findAll(Pageable pageable);

  /** Vérifie si un couple type/value existe déjà. */
  boolean existsByTypeAndValue(String type, String value);
}
