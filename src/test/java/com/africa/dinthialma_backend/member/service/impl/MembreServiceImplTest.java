package com.africa.dinthialma_backend.member.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserRoleAssignment;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.dto.AddMembreRequest;
import com.africa.dinthialma_backend.member.dto.MembreResponse;
import com.africa.dinthialma_backend.member.dto.UpdateMembreStatutRequest;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.mapper.MembreMapper;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembreServiceImpl")
class MembreServiceImplTest {

  @Mock private TontineMembreRepository membreRepository;
  @Spy private MembreMapper membreMapper = Mappers.getMapper(MembreMapper.class);
  @Mock private TontineRepository tontineRepository;
  @Mock private CycleTontineRepository cycleRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserRoleAssignmentRepository roleRepository;
  @Mock private KeycloakAuthService keycloakAuthService;
  @Mock private AuditService auditService;
  @Mock private WhatsappService whatsappService;
  @Mock private NotificationService notificationService;
  @InjectMocks private MembreServiceImpl service;

  private User gestionnaire;
  private User etranger;
  private Tontine tontine;

  @BeforeEach
  void setUp() {
    gestionnaire = Fixtures.user("Gestionnaire");
    etranger = Fixtures.user("Étranger");
    tontine = Fixtures.tontine(gestionnaire);
    lenient()
        .when(userRepository.findByKeycloakId(gestionnaire.getKeycloakId()))
        .thenReturn(Optional.of(gestionnaire));
    lenient()
        .when(userRepository.findByKeycloakId(etranger.getKeycloakId()))
        .thenReturn(Optional.of(etranger));
    lenient()
        .when(roleRepository.existsByUserIdAndRole(any(), eq(UserRole.SUPER_ADMIN)))
        .thenReturn(false);
    lenient()
        .when(tontineRepository.findByIdAndDeletedAtIsNull(tontine.getId()))
        .thenReturn(Optional.of(tontine));
    lenient()
        .when(membreRepository.save(any()))
        .thenAnswer(
            i -> {
              TontineMembre m = i.getArgument(0);
              if (m.getId() == null) m.setId(UUID.randomUUID());
              return m;
            });
    lenient()
        .when(userRepository.save(any()))
        .thenAnswer(
            i -> {
              User u = i.getArgument(0);
              if (u.getId() == null) u.setId(UUID.randomUUID());
              return u;
            });
  }

  private static AddMembreRequest ajout(String phone, String prenom, String nom, Integer ordre) {
    AddMembreRequest r = new AddMembreRequest();
    r.setPhone(phone);
    r.setFirstName(prenom);
    r.setLastName(nom);
    r.setOrdreJackpot(ordre);
    return r;
  }

  @Nested
  @DisplayName("ajout de membre")
  class Ajout {

    @Test
    @DisplayName(
        "compte ACTIVE existant : membre ACTIF en fin de rotation, rôle MEMBER attribué, notifié")
    void compteActif() throws Exception {
      User awa = Fixtures.user("Awa");
      when(userRepository.findByPhoneAndDeletedAtIsNull(awa.getPhone()))
          .thenReturn(Optional.of(awa));
      when(membreRepository.findMaxOrdreJackpot(tontine.getId())).thenReturn(3);
      when(roleRepository.existsByUserIdAndRole(awa.getId(), UserRole.MEMBER)).thenReturn(false);

      MembreResponse r =
          service.addMembre(
              gestionnaire.getKeycloakId(),
              tontine.getId(),
              ajout("+" + awa.getPhone(), null, null, null));

      assertThat(r.getOrdreJackpot()).isEqualTo(4);
      assertThat(r.getStatut()).isEqualTo(MembreStatut.ACTIF);
      assertThat(r.getTontineId()).isEqualTo(tontine.getId());
      verify(keycloakAuthService).assignRole(awa.getKeycloakId(), "DINTHIALMA_MEMBER");
      ArgumentCaptor<UserRoleAssignment> role = ArgumentCaptor.forClass(UserRoleAssignment.class);
      verify(roleRepository).save(role.capture());
      assertThat(role.getValue().isSyncedToKeycloak()).isTrue();
      verify(notificationService)
          .notify(
              eq(awa.getId()),
              eq(NotificationType.INVITATION_TONTINE),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
      verify(auditService).logCreate(eq(gestionnaire), eq("tontine_membres"), any());
      verify(whatsappService, never()).sendTontineInvite(anyString(), anyString());
    }

    @Test
    @DisplayName(
        "rôle déjà présent : rien n'est réattribué ; Keycloak en panne -> rôle non synchronisé")
    void roleMember() throws Exception {
      User a = Fixtures.user("A");
      User b = Fixtures.user("B");
      when(userRepository.findByPhoneAndDeletedAtIsNull(a.getPhone())).thenReturn(Optional.of(a));
      when(userRepository.findByPhoneAndDeletedAtIsNull(b.getPhone())).thenReturn(Optional.of(b));
      when(roleRepository.existsByUserIdAndRole(a.getId(), UserRole.MEMBER)).thenReturn(true);
      when(roleRepository.existsByUserIdAndRole(b.getId(), UserRole.MEMBER)).thenReturn(false);
      doThrow(new CustomException(HttpStatus.BAD_GATEWAY, "KO"))
          .when(keycloakAuthService)
          .assignRole(b.getKeycloakId(), "DINTHIALMA_MEMBER");

      service.addMembre(
          gestionnaire.getKeycloakId(), tontine.getId(), ajout(a.getPhone(), null, null, null));
      service.addMembre(
          gestionnaire.getKeycloakId(), tontine.getId(), ajout(b.getPhone(), null, null, null));

      ArgumentCaptor<UserRoleAssignment> role = ArgumentCaptor.forClass(UserRoleAssignment.class);
      verify(roleRepository).save(role.capture());
      assertThat(role.getValue().getUser()).isSameAs(b);
      assertThat(role.getValue().isSyncedToKeycloak()).isFalse();
    }

    @Test
    @DisplayName(
        "numéro inconnu : compte PRE_ENROLLED créé (sans Keycloak), invitation WhatsApp, pas de rôle")
    void numeroInconnu() throws Exception {
      when(userRepository.findByPhoneAndDeletedAtIsNull("221771234567"))
          .thenReturn(Optional.empty());
      doThrow(new RuntimeException("WhatsApp KO"))
          .when(whatsappService)
          .sendTontineInvite(anyString(), anyString());

      MembreResponse r =
          service.addMembre(
              gestionnaire.getKeycloakId(),
              tontine.getId(),
              ajout("+221771234567", " Fatou ", " Sow ", 2));

      ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(captor.capture());
      User cree = captor.getValue();
      assertThat(cree.getAccountStatus()).isEqualTo(AccountStatus.PRE_ENROLLED);
      assertThat(cree.getKeycloakId()).isNull();
      assertThat(cree.getFirstName()).isEqualTo("Fatou");
      assertThat(cree.getPhone()).isEqualTo("221771234567");
      assertThat(r.getOrdreJackpot()).isEqualTo(2);
      verify(whatsappService).sendTontineInvite("221771234567", tontine.getNom());
      verify(roleRepository, never()).save(any());
      verify(keycloakAuthService, never()).assignRole(any(), any());
    }

    @Test
    @DisplayName("numéro inconnu sans prénom ou nom -> 400 MEMBER_NAME_REQUIRED")
    void nomObligatoire() {
      when(userRepository.findByPhoneAndDeletedAtIsNull("221771234567"))
          .thenReturn(Optional.empty());
      for (AddMembreRequest r :
          List.of(
              ajout("221771234567", null, "Sow", null),
              ajout("221771234567", "Fatou", " ", null))) {
        assertThatThrownBy(
                () -> service.addMembre(gestionnaire.getKeycloakId(), tontine.getId(), r))
            .isInstanceOfSatisfying(
                BadRequestException.class,
                e -> assertThat(e.getCode()).isEqualTo(ErrorCode.MEMBER_NAME_REQUIRED));
      }
      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("PRE_ENROLLED existant au nom vide : nom complété ; nom déjà connu : conservé")
    void completionPreInscrit() throws Exception {
      User pre = Fixtures.user("");
      pre.setLastName("");
      pre.setAccountStatus(AccountStatus.PRE_ENROLLED);
      when(userRepository.findByPhoneAndDeletedAtIsNull(pre.getPhone()))
          .thenReturn(Optional.of(pre));

      service.addMembre(
          gestionnaire.getKeycloakId(),
          tontine.getId(),
          ajout(pre.getPhone(), " Ndeye ", " Fall ", null));
      assertThat(pre.getFirstName()).isEqualTo("Ndeye");
      assertThat(pre.getLastName()).isEqualTo("Fall");

      User connu = Fixtures.user("Connu");
      connu.setAccountStatus(AccountStatus.PRE_ENROLLED);
      when(userRepository.findByPhoneAndDeletedAtIsNull(connu.getPhone()))
          .thenReturn(Optional.of(connu));
      service.addMembre(
          gestionnaire.getKeycloakId(),
          tontine.getId(),
          ajout(connu.getPhone(), "Autre", "Nom", null));
      assertThat(connu.getFirstName()).isEqualTo("Connu");
    }

    @Test
    @DisplayName("PRE_ENROLLED au nom vide ajouté sans nom : pas d'erreur 500, nom laissé vide")
    void preInscritSansNomFourni() throws Exception {
      User pre = Fixtures.user("");
      pre.setLastName("");
      pre.setAccountStatus(AccountStatus.PRE_ENROLLED);
      when(userRepository.findByPhoneAndDeletedAtIsNull(pre.getPhone()))
          .thenReturn(Optional.of(pre));

      service.addMembre(
          gestionnaire.getKeycloakId(), tontine.getId(), ajout(pre.getPhone(), null, null, null));

      assertThat(pre.getFirstName()).isEmpty();
    }

    @Test
    @DisplayName(
        "déjà membre (MEMBER_ALREADY_IN_TONTINE) ou position occupée (JACKPOT_POSITION_TAKEN) -> 409")
    void conflits() {
      User awa = Fixtures.user("Awa");
      when(userRepository.findByPhoneAndDeletedAtIsNull(awa.getPhone()))
          .thenReturn(Optional.of(awa));
      when(membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
              tontine.getId(), awa.getId()))
          .thenReturn(true);
      assertThatThrownBy(
              () ->
                  service.addMembre(
                      gestionnaire.getKeycloakId(),
                      tontine.getId(),
                      ajout(awa.getPhone(), null, null, null)))
          .isInstanceOfSatisfying(
              ConflictException.class,
              e -> assertThat(e.getCode()).isEqualTo(ErrorCode.MEMBER_ALREADY_IN_TONTINE));

      when(membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
              tontine.getId(), awa.getId()))
          .thenReturn(false);
      when(membreRepository.existsByTontine_IdAndOrdreJackpotAndDeletedAtIsNull(tontine.getId(), 1))
          .thenReturn(true);
      assertThatThrownBy(
              () ->
                  service.addMembre(
                      gestionnaire.getKeycloakId(),
                      tontine.getId(),
                      ajout(awa.getPhone(), null, null, 1)))
          .isInstanceOfSatisfying(
              ConflictException.class,
              e -> assertThat(e.getCode()).isEqualTo(ErrorCode.JACKPOT_POSITION_TAKEN));
      verify(membreRepository, never()).save(any());
    }

    @Test
    @DisplayName("ajout par un tiers -> 403 ; tontine inconnue -> 404")
    void acces() {
      assertThatThrownBy(
              () ->
                  service.addMembre(
                      etranger.getKeycloakId(),
                      tontine.getId(),
                      ajout("221770000009", "A", "B", null)))
          .isInstanceOf(ForbiddenException.class);
      UUID inconnue = UUID.randomUUID();
      when(tontineRepository.findByIdAndDeletedAtIsNull(inconnue)).thenReturn(Optional.empty());
      assertThatThrownBy(
              () ->
                  service.addMembre(
                      gestionnaire.getKeycloakId(),
                      inconnue,
                      ajout("221770000009", "A", "B", null)))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  @DisplayName("retrait : suppression logique, statut SORTI, membre notifié ; inconnu -> 404")
  void retrait() throws Exception {
    TontineMembre m = Fixtures.membre(tontine, Fixtures.user("Awa"), 1);
    when(membreRepository.findByIdAndTontine_IdAndDeletedAtIsNull(m.getId(), tontine.getId()))
        .thenReturn(Optional.of(m));

    service.removeMembre(gestionnaire.getKeycloakId(), tontine.getId(), m.getId());

    assertThat(m.getDeletedAt()).isNotNull();
    assertThat(m.getStatut()).isEqualTo(MembreStatut.SORTI);
    verify(notificationService)
        .notify(
            eq(m.getUser().getId()),
            eq(NotificationType.STATUT_MEMBRE),
            anyString(),
            anyString(),
            eq(tontine.getId()),
            any());

    UUID inconnu = UUID.randomUUID();
    when(membreRepository.findByIdAndTontine_IdAndDeletedAtIsNull(inconnu, tontine.getId()))
        .thenReturn(Optional.empty());
    assertThatThrownBy(
            () -> service.removeMembre(gestionnaire.getKeycloakId(), tontine.getId(), inconnu))
        .isInstanceOf(NotFoundException.class);
  }

  @Nested
  @DisplayName("liste des membres")
  class Liste {

    private final Pageable page = PageRequest.of(0, 20);

    @Test
    @DisplayName("membre de la tontine : page convertie avec filtres")
    @SuppressWarnings("unchecked")
    void membre() throws Exception {
      User awa = Fixtures.user("Awa");
      when(userRepository.findByKeycloakId(awa.getKeycloakId())).thenReturn(Optional.of(awa));
      when(membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
              tontine.getId(), awa.getId()))
          .thenReturn(true);
      when(membreRepository.findAll(any(Specification.class), eq(page)))
          .thenReturn(new PageImpl<>(List.of(Fixtures.membre(tontine, awa, 1))));

      assertThat(
              service
                  .listMembres(
                      awa.getKeycloakId(),
                      tontine.getId(),
                      MembreStatut.ACTIF,
                      "awa",
                      null,
                      null,
                      page)
                  .getContent())
          .extracting(MembreResponse::getOrdreJackpot)
          .containsExactly(1);
    }

    @Test
    @DisplayName("filtre de paiement sur un cycle d'une autre tontine -> 404 ; tiers -> 403")
    void refus() {
      UUID cycleAilleurs = UUID.randomUUID();
      when(cycleRepository.findByIdAndTontine_IdAndDeletedAtIsNull(cycleAilleurs, tontine.getId()))
          .thenReturn(Optional.empty());
      assertThatThrownBy(
              () ->
                  service.listMembres(
                      gestionnaire.getKeycloakId(),
                      tontine.getId(),
                      null,
                      null,
                      cycleAilleurs,
                      true,
                      page))
          .isInstanceOf(NotFoundException.class);

      assertThatThrownBy(
              () ->
                  service.listMembres(
                      etranger.getKeycloakId(), tontine.getId(), null, null, null, null, page))
          .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("SUPER_ADMIN : accès sans être membre, cycle de la tontine accepté")
    @SuppressWarnings("unchecked")
    void superAdmin() throws Exception {
      when(roleRepository.existsByUserIdAndRole(etranger.getId(), UserRole.SUPER_ADMIN))
          .thenReturn(true);
      var cycle = Fixtures.cycle(tontine, 1, CycleStatut.EN_COURS);
      when(cycleRepository.findByIdAndTontine_IdAndDeletedAtIsNull(cycle.getId(), tontine.getId()))
          .thenReturn(Optional.of(cycle));
      when(membreRepository.findAll(any(Specification.class), eq(page)))
          .thenReturn(new PageImpl<>(List.of()));

      assertThat(
              service.listMembres(
                  etranger.getKeycloakId(),
                  tontine.getId(),
                  null,
                  null,
                  cycle.getId(),
                  false,
                  page))
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("changement de statut")
  class Statut {

    private UpdateMembreStatutRequest req(MembreStatut s) {
      UpdateMembreStatutRequest r = new UpdateMembreStatutRequest();
      r.setStatut(s);
      return r;
    }

    @Test
    @DisplayName("suspension puis réactivation : audit + notification à chaque fois")
    void suspensionReactivation() throws Exception {
      TontineMembre m = Fixtures.membre(tontine, Fixtures.user("Awa"), 1);
      when(membreRepository.findByIdAndTontine_IdAndDeletedAtIsNull(m.getId(), tontine.getId()))
          .thenReturn(Optional.of(m));

      assertThat(
              service
                  .updateStatut(
                      gestionnaire.getKeycloakId(),
                      tontine.getId(),
                      m.getId(),
                      req(MembreStatut.SUSPENDU))
                  .getStatut())
          .isEqualTo(MembreStatut.SUSPENDU);
      verify(auditService)
          .log(gestionnaire, "tontine_membres", m.getId(), "statut", "ACTIF", "SUSPENDU");
      service.updateStatut(
          gestionnaire.getKeycloakId(), tontine.getId(), m.getId(), req(MembreStatut.ACTIF));
      verify(notificationService, org.mockito.Mockito.times(2))
          .notify(
              eq(m.getUser().getId()),
              eq(NotificationType.STATUT_MEMBRE),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
    }

    @Test
    @DisplayName("passage à SORTI : pas de notification ; un SORTI ne peut que redevenir ACTIF")
    void sorti() throws Exception {
      TontineMembre m = Fixtures.membre(tontine, Fixtures.user("Awa"), 1);
      when(membreRepository.findByIdAndTontine_IdAndDeletedAtIsNull(m.getId(), tontine.getId()))
          .thenReturn(Optional.of(m));

      service.updateStatut(
          gestionnaire.getKeycloakId(), tontine.getId(), m.getId(), req(MembreStatut.SORTI));
      verify(notificationService, never()).notify(any(), any(), any(), any(), any(), any());

      assertThatThrownBy(
              () ->
                  service.updateStatut(
                      gestionnaire.getKeycloakId(),
                      tontine.getId(),
                      m.getId(),
                      req(MembreStatut.SUSPENDU)))
          .isInstanceOf(BadRequestException.class);
      assertThat(
              service
                  .updateStatut(
                      gestionnaire.getKeycloakId(),
                      tontine.getId(),
                      m.getId(),
                      req(MembreStatut.ACTIF))
                  .getStatut())
          .isEqualTo(MembreStatut.ACTIF);
    }

    @Test
    @DisplayName("membre inconnu -> 404 ; tiers -> 403")
    void refus() {
      UUID inconnu = UUID.randomUUID();
      when(membreRepository.findByIdAndTontine_IdAndDeletedAtIsNull(inconnu, tontine.getId()))
          .thenReturn(Optional.empty());
      assertThatThrownBy(
              () ->
                  service.updateStatut(
                      gestionnaire.getKeycloakId(),
                      tontine.getId(),
                      inconnu,
                      req(MembreStatut.ACTIF)))
          .isInstanceOf(NotFoundException.class);
      assertThatThrownBy(
              () ->
                  service.updateStatut(
                      etranger.getKeycloakId(), tontine.getId(), inconnu, req(MembreStatut.ACTIF)))
          .isInstanceOf(ForbiddenException.class);
    }
  }
}
