package com.africa.dinthialma_backend.common.codelist.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.codelist.dto.LaCodeListDto;
import com.africa.dinthialma_backend.common.codelist.entity.LaCodeList;
import com.africa.dinthialma_backend.common.codelist.mapper.LaCodeListMapper;
import com.africa.dinthialma_backend.common.codelist.repository.LaCodeListRepository;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.security.AccessControl;
import com.africa.dinthialma_backend.support.Fixtures;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("LaCodeListServiceImpl")
class LaCodeListServiceImplTest {

  private static final String KC = "kc-admin";

  @Mock private LaCodeListRepository repository;
  @Spy private LaCodeListMapper mapper = Mappers.getMapper(LaCodeListMapper.class);
  @Mock private AccessControl accessControl;

  @InjectMocks private LaCodeListServiceImpl service;

  private static LaCodeList valeur(String value, boolean systeme) {
    return LaCodeList.builder()
        .id(UUID.randomUUID())
        .type("METHODE_PAIEMENT")
        .value(value)
        .description(value)
        .isSystemAssign(systeme)
        .build();
  }

  private static LaCodeListDto dto(String value) {
    return LaCodeListDto.builder()
        .type("METHODE_PAIEMENT")
        .value(value)
        .description("Desc " + value)
        .build();
  }

  @Test
  @DisplayName("lecture publique : valeurs actives ; type restreint -> 403")
  void lecturePublique() throws Exception {
    when(repository.findAllByTypeAndDeletedAtIsNull("METHODE_PAIEMENT"))
        .thenReturn(List.of(valeur("WAVE", true)));
    assertThat(service.findPublicByType("METHODE_PAIEMENT"))
        .extracting(LaCodeListDto::getValue)
        .containsExactly("WAVE");
    assertThatThrownBy(() -> service.findPublicByType("ROLE_INTERNE"))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  @DisplayName("lecture admin : exige ADMIN ou SUPER_ADMIN, donne accès aux types restreints")
  void lectureAdmin() throws Exception {
    when(repository.findAllByTypeAndDeletedAtIsNull("ROLE_INTERNE"))
        .thenReturn(List.of(valeur("X", true)));
    assertThat(service.findByTypeAdmin(KC, "ROLE_INTERNE")).hasSize(1);
    verify(accessControl).exigerUnRole(KC, UserRole.SUPER_ADMIN, UserRole.ADMIN);
  }

  @Test
  @DisplayName("liste paginée et lecture par id ; id inconnu ou supprimé -> 404")
  void listeEtDetail() throws Exception {
    LaCodeList v = valeur("CASH", false);
    when(repository.findByDeletedAtIsNull(any())).thenReturn(new PageImpl<>(List.of(v)));
    assertThat(service.findAll(PageRequest.of(0, 25)).getContent()).hasSize(1);

    when(repository.findByIdAndDeletedAtIsNull(v.getId())).thenReturn(Optional.of(v));
    assertThat(service.findById(v.getId()).getValue()).isEqualTo("CASH");
    UUID inconnu = UUID.randomUUID();
    when(repository.findByIdAndDeletedAtIsNull(inconnu)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.findById(inconnu)).isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("création : nouvelle valeur enregistrée ; doublon actif -> 409")
  void creation() throws Exception {
    when(repository.findByTypeAndValue("METHODE_PAIEMENT", "OM")).thenReturn(Optional.empty());
    when(repository.existsByTypeAndValue("METHODE_PAIEMENT", "OM")).thenReturn(false);
    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    assertThat(service.create(KC, dto("OM")).getDescription()).isEqualTo("Desc OM");
    verify(accessControl).exigerUnRole(KC, UserRole.ADMIN, UserRole.SUPER_ADMIN);

    when(repository.findByTypeAndValue("METHODE_PAIEMENT", "WAVE"))
        .thenReturn(Optional.of(valeur("WAVE", true)));
    when(repository.existsByTypeAndValue("METHODE_PAIEMENT", "WAVE")).thenReturn(true);
    assertThatThrownBy(() -> service.create(KC, dto("WAVE"))).isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("création d'un couple supprimé : restauration de la même ligne")
  void restauration() throws Exception {
    LaCodeList supprimee = valeur("OM", false);
    supprimee.setDeletedAt(java.time.LocalDateTime.now());
    when(repository.findByTypeAndValue("METHODE_PAIEMENT", "OM"))
        .thenReturn(Optional.of(supprimee));
    when(repository.save(supprimee)).thenReturn(supprimee);

    LaCodeListDto r = service.create(KC, dto("OM"));

    assertThat(r.getId()).isEqualTo(supprimee.getId());
    assertThat(supprimee.getDeletedAt()).isNull();
    assertThat(supprimee.getDescription()).isEqualTo("Desc OM");
  }

  @Test
  @DisplayName("création : violation d'unicité concurrente -> 409")
  void creationConcurrente() {
    when(repository.findByTypeAndValue(any(), any())).thenReturn(Optional.empty());
    when(repository.existsByTypeAndValue(any(), any())).thenReturn(false);
    when(repository.save(any())).thenThrow(new DataIntegrityViolationException("uq"));
    assertThatThrownBy(() -> service.create(KC, dto("OM"))).isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("modification : couple modifié vers un couple existant -> 409, sinon mis à jour")
  void modification() throws Exception {
    LaCodeList v = valeur("OM", false);
    when(repository.findByIdAndDeletedAtIsNull(v.getId())).thenReturn(Optional.of(v));
    when(repository.existsByTypeAndValue("METHODE_PAIEMENT", "WAVE")).thenReturn(true);
    assertThatThrownBy(() -> service.update(KC, v.getId(), dto("WAVE")))
        .isInstanceOf(ConflictException.class);

    when(repository.save(v)).thenReturn(v);
    LaCodeListDto r = service.update(KC, v.getId(), dto("OM"));
    assertThat(r.getDescription()).isEqualTo("Desc OM");
    verify(repository, never()).existsByTypeAndValue("METHODE_PAIEMENT", "OM");
  }

  @Test
  @DisplayName(
      "suppression : SUPER_ADMIN, logique ; valeur système refusée (CODE_LIST_SYSTEM_VALUE)")
  void suppression() throws Exception {
    LaCodeList manuelle = valeur("OM", false);
    when(repository.findByIdAndDeletedAtIsNull(manuelle.getId())).thenReturn(Optional.of(manuelle));
    service.delete(KC, manuelle.getId());
    assertThat(manuelle.getDeletedAt()).isNotNull();
    verify(accessControl).exigerUnRole(KC, UserRole.SUPER_ADMIN);

    LaCodeList systeme = valeur("WAVE", true);
    when(repository.findByIdAndDeletedAtIsNull(systeme.getId())).thenReturn(Optional.of(systeme));
    assertThatThrownBy(() -> service.delete(KC, systeme.getId()))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.CODE_LIST_SYSTEM_VALUE));
  }

  @Test
  @DisplayName("droits insuffisants : l'exception d'AccessControl est propagée, rien n'est écrit")
  void droitsInsuffisants() throws Exception {
    User u = Fixtures.user("Simple");
    when(accessControl.exigerUnRole(KC, UserRole.SUPER_ADMIN))
        .thenThrow(new ForbiddenException("refusé"));
    assertThatThrownBy(() -> service.delete(KC, u.getId())).isInstanceOf(ForbiddenException.class);
    verify(repository, never()).save(any());
  }
}
