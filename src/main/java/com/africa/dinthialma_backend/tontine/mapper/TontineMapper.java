package com.africa.dinthialma_backend.tontine.mapper;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.tontine.codeList.MonRoleTontine;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversions des tontines. */
@Mapper
public interface TontineMapper {

  /** Réponse sans relation de l'appelant (listes plateforme SUPER_ADMIN). */
  @Mapping(target = "monRole", ignore = true)
  @Mapping(target = "monMembreId", ignore = true)
  @Mapping(target = "monStatutMembre", ignore = true)
  TontineResponse toResponse(Tontine tontine, int nombreMembresActuels);

  TontineResponse.CreateurInfo toCreateurInfo(User user);

  /**
   * Réponse enrichie de la relation de l'appelant avec la tontine (B01).
   *
   * @param monMembership membership de l'appelant dans cette tontine (null s'il ne cotise pas)
   */
  default TontineResponse toResponse(
      Tontine tontine, int nombreMembresActuels, UUID callerId, TontineMembre monMembership) {
    boolean createur =
        tontine.getCreePar() != null && tontine.getCreePar().getId().equals(callerId);
    return toResponse(tontine, nombreMembresActuels).toBuilder()
        .monRole(MonRoleTontine.of(createur, monMembership != null))
        .monMembreId(monMembership != null ? monMembership.getId() : null)
        .monStatutMembre(monMembership != null ? monMembership.getStatut() : null)
        .build();
  }
}
