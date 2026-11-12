package com.africa.dinthialma_backend.adhesion.mapper;

import com.africa.dinthialma_backend.adhesion.dto.DemandeAdhesionResponse;
import com.africa.dinthialma_backend.adhesion.entity.DemandeAdhesion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Conversions des demandes d'adhésion. Le téléphone du demandeur n'est exposé qu'au gestionnaire
 * (pour le contacter), jamais au demandeur lui-même dans les listes.
 */
@Mapper
public interface DemandeAdhesionMapper {

  /** Vue gestionnaire : avec le téléphone du demandeur. */
  @Mapping(target = "tontineId", source = "tontine.id")
  @Mapping(target = "tontineNom", source = "tontine.nom")
  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "firstName", source = "user.firstName")
  @Mapping(target = "lastName", source = "user.lastName")
  @Mapping(target = "phone", source = "user.phone")
  @Mapping(target = "membreId", source = "membre.id")
  DemandeAdhesionResponse toVueGestionnaire(DemandeAdhesion demande);

  /** Vue demandeur : sans téléphone. */
  @Mapping(target = "tontineId", source = "tontine.id")
  @Mapping(target = "tontineNom", source = "tontine.nom")
  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "firstName", source = "user.firstName")
  @Mapping(target = "lastName", source = "user.lastName")
  @Mapping(target = "phone", ignore = true)
  @Mapping(target = "membreId", source = "membre.id")
  DemandeAdhesionResponse toVueDemandeur(DemandeAdhesion demande);
}
