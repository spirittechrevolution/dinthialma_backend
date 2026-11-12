package com.africa.dinthialma_backend.jackpot.mapper;

import com.africa.dinthialma_backend.jackpot.dto.PrioriteJackpotResponse;
import com.africa.dinthialma_backend.jackpot.entity.PrioriteJackpot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Conversions des priorités de jackpot. Le motif (raison du besoin, souvent personnelle) n'est
 * visible que du gestionnaire et du membre concerné.
 */
@Mapper
public interface PrioriteJackpotMapper {

  /** Vue complète : gestionnaire, SUPER_ADMIN ou membre concerné. */
  @Mapping(target = "tontineId", source = "tontine.id")
  @Mapping(target = "membreId", source = "membre.id")
  @Mapping(target = "userId", source = "membre.user.id")
  @Mapping(target = "firstName", source = "membre.user.firstName")
  @Mapping(target = "lastName", source = "membre.user.lastName")
  @Mapping(target = "cycleId", source = "cycle.id")
  PrioriteJackpotResponse toResponse(PrioriteJackpot priorite);

  /** Vue des autres membres : sans le motif du besoin. */
  default PrioriteJackpotResponse toVueMembre(PrioriteJackpot priorite) {
    return toResponse(priorite).toBuilder().motif(null).build();
  }
}
