package com.africa.dinthialma_backend.tontine.mapper;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.tontine.calendrier.Rythme;
import com.africa.dinthialma_backend.tontine.codeList.MonRoleTontine;
import com.africa.dinthialma_backend.tontine.dto.RappelsEcheance;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import java.time.LocalTime;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/** Conversions des tontines. */
@Mapper
public interface TontineMapper {

  /** Réponse sans relation de l'appelant (listes plateforme SUPER_ADMIN). */
  @Mapping(target = "monRole", ignore = true)
  @Mapping(target = "monMembreId", ignore = true)
  @Mapping(target = "monStatutMembre", ignore = true)
  @Mapping(target = "rappels", source = "tontine", qualifiedByName = "rappels")
  TontineResponse toResponse(Tontine tontine, int nombreMembresActuels);

  /** Rappels avant échéance de la tontine (B26). */
  @Named("rappels")
  default RappelsEcheance rappels(Tontine tontine) {
    return RappelsEcheance.builder()
        .veille(tontine.isRappelVeille())
        .heuresAvant(tontine.getRappelHeuresAvant())
        .build();
  }

  /** Heure d'échéance au format HH:mm. */
  default String heure(LocalTime heure) {
    return Rythme.formaterHeure(heure);
  }

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
