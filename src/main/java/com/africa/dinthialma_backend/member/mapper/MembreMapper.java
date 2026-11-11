package com.africa.dinthialma_backend.member.mapper;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.member.dto.MembreResponse;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/** Conversions des memberships (cotisants d'une tontine). */
@Mapper
public interface MembreMapper {

  @Mapping(target = "tontineId", source = "tontine.id")
  @Mapping(target = "aRecuJackpot", expression = "java(membre.isARecuJackpot())")
  MembreResponse toResponse(TontineMembre membre);

  @Mapping(target = "accountStatus", source = "accountStatus", qualifiedByName = "statutCompte")
  MembreResponse.UserInfo toUserInfo(User user);

  /** Comptes antérieurs sans statut : considérés ACTIVE. */
  @Named("statutCompte")
  default AccountStatus statutCompte(AccountStatus statut) {
    return statut != null ? statut : AccountStatus.ACTIVE;
  }
}
