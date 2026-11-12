package com.africa.dinthialma_backend.auth.mapper;

import com.africa.dinthialma_backend.admin.dto.AdminUserResponse;
import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.dto.UserProfileResponse;
import com.africa.dinthialma_backend.auth.dto.UserSearchResponse;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserRoleAssignment;
import com.africa.dinthialma_backend.common.constants.Constants;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/** Conversions de l'utilisateur vers ses différentes vues (profil, recherche, console). */
@Mapper
public interface UserMapper {

  @Mapping(target = "pinConfigured", expression = "java(user.getPinHash() != null)")
  @Mapping(target = "pinExpired", source = "pinCreatedAt", qualifiedByName = "pinExpire")
  @Mapping(target = "roles", source = "roles", qualifiedByName = "nomsDeRoles")
  UserProfileResponse toProfile(User user);

  @Mapping(target = "accountStatus", source = "accountStatus", qualifiedByName = "statutCompte")
  UserSearchResponse toSearch(User user);

  @Mapping(target = "accountStatus", source = "accountStatus", qualifiedByName = "statutCompte")
  @Mapping(target = "pinConfigured", expression = "java(user.getPinHash() != null)")
  @Mapping(target = "roles", source = "roles", qualifiedByName = "nomsDeRolesTries")
  AdminUserResponse toAdmin(User user);

  /** Comptes antérieurs sans statut : considérés ACTIVE. */
  @Named("statutCompte")
  default AccountStatus statutCompte(AccountStatus statut) {
    return statut != null ? statut : AccountStatus.ACTIVE;
  }

  /** PIN expiré après {@link Constants.Pin#EXPIRY_DAYS} jours. */
  @Named("pinExpire")
  default boolean pinExpire(LocalDateTime pinCreatedAt) {
    return pinCreatedAt != null
        && pinCreatedAt.plusDays(Constants.Pin.EXPIRY_DAYS).isBefore(LocalDateTime.now());
  }

  @Named("nomsDeRoles")
  default Set<String> nomsDeRoles(Collection<UserRoleAssignment> roles) {
    return roles == null
        ? Set.of()
        : roles.stream().map(r -> r.getRole().getKeycloakRole()).collect(Collectors.toSet());
  }

  @Named("nomsDeRolesTries")
  default List<String> nomsDeRolesTries(Collection<UserRoleAssignment> roles) {
    return roles == null
        ? List.of()
        : roles.stream().map(r -> r.getRole().getKeycloakRole()).sorted().toList();
  }
}
