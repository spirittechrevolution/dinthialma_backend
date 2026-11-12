package com.africa.dinthialma_backend.service;

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

import com.africa.dinthialma_backend.adhesion.codeList.StatutDemande;
import com.africa.dinthialma_backend.adhesion.repository.DemandeAdhesionRepository;
import com.africa.dinthialma_backend.admin.dto.AdminUserResponse;
import com.africa.dinthialma_backend.admin.dto.GlobalDashboardResponse;
import com.africa.dinthialma_backend.admin.dto.MyDashboardResponse;
import com.africa.dinthialma_backend.admin.dto.UpdateUserRolesRequest;
import com.africa.dinthialma_backend.admin.service.impl.AdminDashboardServiceImpl;
import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserRoleAssignment;
import com.africa.dinthialma_backend.auth.mapper.UserMapper;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.mapper.CotisationMapper;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.jackpot.repository.PrioriteJackpotRepository;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.CycleGagnant;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.mapper.TontineMapper;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
@DisplayName("AdminDashboardServiceImpl")
class AdminDashboardServiceImplTest {

  @Mock private UserRepository userRepository;
  @Spy private UserMapper userMapper = Mappers.getMapper(UserMapper.class);
  @Spy private CotisationMapper cotisationMapper = Mappers.getMapper(CotisationMapper.class);
  @Spy private TontineMapper tontineMapper = Mappers.getMapper(TontineMapper.class);
  @Mock private UserRoleAssignmentRepository roleRepository;
  @Mock private TontineRepository tontineRepository;
  @Mock private CycleTontineRepository cycleRepository;
  @Mock private TontineMembreRepository membreRepository;
  @Mock private CotisationRepository cotisationRepository;
  @Mock private KeycloakAuthService keycloakAuthService;
  @Mock private DemandeAdhesionRepository demandeRepository;
  @Mock private PrioriteJackpotRepository prioriteRepository;
  @InjectMocks private AdminDashboardServiceImpl service;

  private User superAdmin;
  private User simple;
  private User cible;
  private String kc;
  private final Pageable page = PageRequest.of(0, 20);

  @BeforeEach
  void setUp() {
    superAdmin = Fixtures.user("Super");
    simple = Fixtures.user("Simple");
    cible = Fixtures.user("Cible");
    kc = superAdmin.getKeycloakId();
    for (User u : List.of(superAdmin, simple, cible)) {
      lenient().when(userRepository.findByKeycloakId(u.getKeycloakId())).thenReturn(Optional.of(u));
      lenient()
          .when(userRepository.findByIdAndDeletedAtIsNull(u.getId()))
          .thenReturn(Optional.of(u));
    }
    lenient()
        .when(roleRepository.existsByUserIdAndRole(any(), eq(UserRole.SUPER_ADMIN)))
        .thenReturn(false);
    lenient()
        .when(roleRepository.existsByUserIdAndRole(superAdmin.getId(), UserRole.SUPER_ADMIN))
        .thenReturn(true);
  }

  @Test
  @DisplayName("toutes les fonctions de console exigent SUPER_ADMIN (403)")
  void reserveAuSuperAdmin() {
    String k = simple.getKeycloakId();
    List<org.assertj.core.api.ThrowableAssert.ThrowingCallable> appels =
        List.of(
            () -> service.getGlobalDashboard(k),
            () -> service.listUsers(k, null, null, null, page),
            () -> service.getUserDetail(k, cible.getId()),
            () -> service.disableUser(k, cible.getId()),
            () -> service.enableUser(k, cible.getId()),
            () -> service.updateUserRoles(k, cible.getId(), new UpdateUserRolesRequest()),
            () -> service.listAllTontines(k, null, null, page),
            () -> service.listAllContributions(k, null, null, page));
    appels.forEach(a -> assertThatThrownBy(a).isInstanceOf(ForbiddenException.class));
    when(userRepository.findByKeycloakId("x")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getGlobalDashboard("x")).isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("tableau de bord global : chaque compteur repris du référentiel")
  void global() throws Exception {
    when(userRepository.countByDeletedAtIsNull()).thenReturn(10L);
    when(userRepository.countByActiveTrueAndDeletedAtIsNull()).thenReturn(8L);
    when(userRepository.countByActiveFalseAndDeletedAtIsNull()).thenReturn(2L);
    when(userRepository.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(any()))
        .thenReturn(3L, 1L);
    when(tontineRepository.countByDeletedAtIsNull()).thenReturn(6L);
    when(tontineRepository.countByStatutAndDeletedAtIsNull(TontineStatut.BROUILLON)).thenReturn(1L);
    when(tontineRepository.countByStatutAndDeletedAtIsNull(TontineStatut.ACTIVE)).thenReturn(3L);
    when(tontineRepository.countByStatutAndDeletedAtIsNull(TontineStatut.SUSPENDUE)).thenReturn(1L);
    when(tontineRepository.countByStatutAndDeletedAtIsNull(TontineStatut.TERMINEE)).thenReturn(1L);
    when(cotisationRepository.countByStatutAndDeletedAtIsNull(CotisationStatut.EN_ATTENTE))
        .thenReturn(4L);
    when(cotisationRepository.countByStatutAndDeletedAtIsNull(CotisationStatut.EN_RETARD))
        .thenReturn(2L);
    when(cotisationRepository.sumMontantValideSince(any())).thenReturn(new BigDecimal("25000"));
    when(cotisationRepository.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(any()))
        .thenReturn(5L);

    GlobalDashboardResponse r = service.getGlobalDashboard(kc);

    assertThat(r.getUsers().getTotal()).isEqualTo(10);
    assertThat(r.getUsers().getDesactives()).isEqualTo(2);
    assertThat(r.getUsers().getNouveauxCeMois()).isEqualTo(3);
    assertThat(r.getTontines().getActives()).isEqualTo(3);
    assertThat(r.getTontines().getTerminees()).isEqualTo(1);
    assertThat(r.getFinances().getCotisationsEnRetard()).isEqualTo(2);
    assertThat(r.getFinances().getMontantCotisationsCeMois()).isEqualByComparingTo("25000");
    assertThat(r.getActivite24h().getNouveauxInscrits()).isEqualTo(1);
    assertThat(r.getActivite24h().getCotisationsEnregistrees()).isEqualTo(5);
  }

  @Test
  @DisplayName(
      "utilisateurs : liste filtrée, détail (statut NULL -> ACTIVE, rôles triés), inconnu -> 404")
  @SuppressWarnings("unchecked")
  void utilisateurs() throws Exception {
    cible.setAccountStatus(null);
    cible.setRoles(
        new HashSet<>(
            List.of(
                UserRoleAssignment.builder().user(cible).role(UserRole.USER).build(),
                UserRoleAssignment.builder().user(cible).role(UserRole.ADMIN).build())));
    when(userRepository.findAll(any(Specification.class), eq(page)))
        .thenReturn(new PageImpl<>(List.of(cible)));

    assertThat(service.listUsers(kc, AccountStatus.ACTIVE, UserRole.ADMIN, "cib", page)).hasSize(1);
    AdminUserResponse d = service.getUserDetail(kc, cible.getId());
    assertThat(d.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(d.getRoles()).containsExactly("DINTHIALMA_ADMIN", "DINTHIALMA_USER");

    UUID inconnu = UUID.randomUUID();
    when(userRepository.findByIdAndDeletedAtIsNull(inconnu)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getUserDetail(kc, inconnu))
        .isInstanceOf(NotFoundException.class);
  }

  @Nested
  @DisplayName("activation de compte")
  class Activation {

    @Test
    @DisplayName("désactivation : base + Keycloak (échec Keycloak non bloquant)")
    void desactivation() throws Exception {
      doThrow(new CustomException(HttpStatus.BAD_GATEWAY, "KO"))
          .when(keycloakAuthService)
          .disableUser(cible.getKeycloakId());
      assertThat(service.disableUser(kc, cible.getId()).isActive()).isFalse();
      verify(userRepository).save(cible);
    }

    @Test
    @DisplayName("désactivation refusée : son propre compte ou compte déjà désactivé")
    void desactivationRefusee() throws Exception {
      assertThatThrownBy(() -> service.disableUser(kc, superAdmin.getId()))
          .isInstanceOf(BadRequestException.class);
      cible.setActive(false);
      assertThatThrownBy(() -> service.disableUser(kc, cible.getId()))
          .isInstanceOf(BadRequestException.class);
      verify(keycloakAuthService, never()).disableUser(anyString());
    }

    @Test
    @DisplayName("réactivation : base + Keycloak ; compte déjà actif refusé")
    void reactivation() throws Exception {
      cible.setActive(false);
      doThrow(new CustomException(HttpStatus.BAD_GATEWAY, "KO"))
          .when(keycloakAuthService)
          .enableUser(cible.getKeycloakId());
      assertThat(service.enableUser(kc, cible.getId()).isActive()).isTrue();
      assertThatThrownBy(() -> service.enableUser(kc, cible.getId()))
          .isInstanceOf(BadRequestException.class);
    }
  }

  @Nested
  @DisplayName("rôles (remplacement complet)")
  class Roles {

    private UpdateUserRolesRequest req(UserRole... roles) {
      UpdateUserRolesRequest r = new UpdateUserRolesRequest();
      r.setRoles(new HashSet<>(Set.of(roles)));
      return r;
    }

    @Test
    @DisplayName(
        "ajout des manquants, retrait des absents, USER toujours conservé, synchro Keycloak tracée")
    void remplacement() throws Exception {
      UserRoleAssignment user =
          UserRoleAssignment.builder().user(cible).role(UserRole.USER).build();
      UserRoleAssignment member =
          UserRoleAssignment.builder().user(cible).role(UserRole.MEMBER).build();
      when(roleRepository.findAllByUserId(cible.getId())).thenReturn(List.of(user, member));
      when(roleRepository.existsByUserIdAndRole(cible.getId(), UserRole.USER)).thenReturn(true);
      when(roleRepository.existsByUserIdAndRole(cible.getId(), UserRole.ADMIN)).thenReturn(false);
      doThrow(new CustomException(HttpStatus.BAD_GATEWAY, "KO"))
          .when(keycloakAuthService)
          .assignRole(cible.getKeycloakId(), "DINTHIALMA_ADMIN");
      doThrow(new CustomException(HttpStatus.BAD_GATEWAY, "KO"))
          .when(keycloakAuthService)
          .removeRole(cible.getKeycloakId(), "DINTHIALMA_MEMBER");

      service.updateUserRoles(kc, cible.getId(), req(UserRole.ADMIN));

      ArgumentCaptor<UserRoleAssignment> ajout = ArgumentCaptor.forClass(UserRoleAssignment.class);
      verify(roleRepository).save(ajout.capture());
      assertThat(ajout.getValue().getRole()).isEqualTo(UserRole.ADMIN);
      assertThat(ajout.getValue().isSyncedToKeycloak()).isFalse();
      verify(roleRepository).delete(member);
      verify(roleRepository, never()).delete(user);
    }

    @Test
    @DisplayName("idempotent : rôles déjà en place -> aucune écriture")
    void idempotent() throws Exception {
      UserRoleAssignment user =
          UserRoleAssignment.builder().user(cible).role(UserRole.USER).build();
      UserRoleAssignment admin =
          UserRoleAssignment.builder().user(cible).role(UserRole.ADMIN).build();
      when(roleRepository.findAllByUserId(cible.getId())).thenReturn(List.of(user, admin));
      when(roleRepository.existsByUserIdAndRole(cible.getId(), UserRole.USER)).thenReturn(true);
      when(roleRepository.existsByUserIdAndRole(cible.getId(), UserRole.ADMIN)).thenReturn(true);

      service.updateUserRoles(kc, cible.getId(), req(UserRole.ADMIN, UserRole.USER));

      verify(roleRepository, never()).save(any());
      verify(roleRepository, never()).delete(any());
    }
  }

  @Test
  @DisplayName("toutes les tontines : nombre de membres par tontine")
  @SuppressWarnings("unchecked")
  void tontines() throws Exception {
    Tontine t = Fixtures.tontine(cible);
    when(tontineRepository.findAll(any(Specification.class), eq(page)))
        .thenReturn(new PageImpl<>(List.of(t)));
    when(membreRepository.countByTontine_IdAndDeletedAtIsNull(t.getId())).thenReturn(4L);
    assertThat(
            service
                .listAllTontines(kc, TontineStatut.ACTIVE, "x", page)
                .getContent()
                .get(0)
                .getNombreMembresActuels())
        .isEqualTo(4);
  }

  @Test
  @DisplayName("toutes les cotisations : la bonne requête selon les filtres combinés")
  void cotisations() throws Exception {
    UUID tid = UUID.randomUUID();
    when(cotisationRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
            tid, CotisationStatut.VALIDE, page))
        .thenReturn(new PageImpl<>(List.of()));
    when(cotisationRepository.findByTontine_IdAndDeletedAtIsNull(tid, page))
        .thenReturn(new PageImpl<>(List.of()));
    when(cotisationRepository.findByStatutAndDeletedAtIsNull(CotisationStatut.VALIDE, page))
        .thenReturn(new PageImpl<>(List.of()));
    when(cotisationRepository.findByDeletedAtIsNull(page)).thenReturn(new PageImpl<>(List.of()));

    service.listAllContributions(kc, CotisationStatut.VALIDE, tid, page);
    service.listAllContributions(kc, null, tid, page);
    service.listAllContributions(kc, CotisationStatut.VALIDE, null, page);
    service.listAllContributions(kc, null, null, page);

    verify(cotisationRepository)
        .findByTontine_IdAndStatutAndDeletedAtIsNull(tid, CotisationStatut.VALIDE, page);
    verify(cotisationRepository).findByTontine_IdAndDeletedAtIsNull(tid, page);
    verify(cotisationRepository).findByStatutAndDeletedAtIsNull(CotisationStatut.VALIDE, page);
    verify(cotisationRepository).findByDeletedAtIsNull(page);
  }

  @Nested
  @DisplayName("mon tableau de bord (gestionnaire)")
  class MonTableauDeBord {

    @Test
    @DisplayName(
        "agrégats globaux + par tontine : tour en cours, bénéficiaire, collecté / attendu, gagnants à désigner")
    void complet() throws Exception {
      String k = simple.getKeycloakId();
      UUID uid = simple.getId();
      Tontine rotative = Fixtures.tontine(simple);
      rotative.setOrdreBeneficiaire("MANUEL");
      Tontine evt = Fixtures.tontine(simple);
      evt.setTontineType(TontineType.EVENEMENTIELLE);
      evt.setNomEvenement("Tabaski");
      when(tontineRepository.findByCreePar_IdAndDeletedAtIsNull(uid, page))
          .thenReturn(new PageImpl<>(List.of(rotative, evt), page, 2));
      when(cotisationRepository.sumMontantValideByCreateur(uid)).thenReturn(new BigDecimal("9000"));
      when(membreRepository.countByTontine_CreePar_IdAndTontine_DeletedAtIsNullAndDeletedAtIsNull(
              uid))
          .thenReturn(7L);
      when(cotisationRepository
              .countByTontine_CreePar_IdAndTontine_DeletedAtIsNullAndStatutAndDeletedAtIsNull(
                  uid, CotisationStatut.EN_ATTENTE))
          .thenReturn(2L);
      when(cotisationRepository
              .countByTontine_CreePar_IdAndTontine_DeletedAtIsNullAndStatutAndDeletedAtIsNull(
                  uid, CotisationStatut.EN_RETARD))
          .thenReturn(1L);
      when(cotisationRepository.sumMontantValideToursEnCoursByCreateur(uid))
          .thenReturn(new BigDecimal("2000"));
      when(membreRepository.findParametresToursEnCoursByCreateur(uid))
          .thenReturn(
              List.of(
                  new Object[] {false, new BigDecimal("1000"), null, 4L},
                  new Object[] {true, null, new BigDecimal("500"), 2L},
                  new Object[] {true, null, null, 3L}));

      CycleTontine tour = Fixtures.cycle(rotative, 3, CycleStatut.EN_COURS);
      User benef = Fixtures.user("Bénéficiaire");
      CycleGagnant supprime =
          CycleGagnant.builder()
              .cycle(tour)
              .membre(Fixtures.membre(rotative, Fixtures.user("X"), 1))
              .build();
      supprime.setDeletedAt(LocalDateTime.now());
      tour.setGagnants(
          new ArrayList<>(
              List.of(
                  supprime,
                  CycleGagnant.builder()
                      .cycle(tour)
                      .membre(Fixtures.membre(rotative, benef, 2))
                      .build())));
      when(cycleRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              rotative.getId(), CycleStatut.EN_COURS))
          .thenReturn(Optional.of(tour));
      when(cycleRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              evt.getId(), CycleStatut.EN_COURS))
          .thenReturn(Optional.empty());
      when(cotisationRepository.countAndSumByStatutForCycle(tour.getId()))
          .thenReturn(
              List.of(
                  new Object[] {CotisationStatut.VALIDE, 2L, new BigDecimal("2000")},
                  new Object[] {CotisationStatut.EN_ATTENTE, 1L, new BigDecimal("1000")},
                  new Object[] {CotisationStatut.REFUSE, 1L, new BigDecimal("1000")}));
      when(membreRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(
              rotative.getId(), MembreStatut.ACTIF))
          .thenReturn(5L);
      when(cycleRepository.existsTermineSansGagnant(rotative.getId())).thenReturn(true);
      when(cotisationRepository.countMembresAyantCotiseValide(evt.getId())).thenReturn(3L);

      when(demandeRepository
              .countByTontine_CreePar_IdAndTontine_DeletedAtIsNullAndStatutAndDeletedAtIsNull(
                  uid, StatutDemande.EN_ATTENTE))
          .thenReturn(4L);
      when(demandeRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(
              rotative.getId(), StatutDemande.EN_ATTENTE))
          .thenReturn(3L);

      MyDashboardResponse r = service.getMyDashboard(k, page);

      assertThat(r.getTotalDemandesEnAttente()).isEqualTo(4);
      assertThat(r.getTontines().getContent().get(0).getDemandesEnAttente()).isEqualTo(3);
      assertThat(r.getTontines().getContent().get(1).getDemandesEnAttente()).isZero();

      assertThat(r.getNombreTontinesGerees()).isEqualTo(2);
      assertThat(r.getMontantTotalValide()).isEqualByComparingTo("9000");
      assertThat(r.getTotalMembres()).isEqualTo(7);
      assertThat(r.getTotalCotisationsEnAttente()).isEqualTo(2);
      assertThat(r.getTotalCotisationsEnRetard()).isEqualTo(1);
      assertThat(r.getTotalCollecteToursEnCours()).isEqualByComparingTo("2000");
      // 1000 × 4 + 500 × 2 (montant libre au minimum) ; montant libre sans minimum ignoré
      assertThat(r.getTotalAttenduToursEnCours()).isEqualByComparingTo("5000");

      MyDashboardResponse.TontineStats s1 = r.getTontines().getContent().getFirst();
      assertThat(s1.isGagnantsADesigner()).isTrue();
      assertThat(s1.getDateEcheance()).isNull();
      assertThat(s1.getMontantLibre()).isNull();
      assertThat(s1.getNombreMembresCotise()).isNull();
      MyDashboardResponse.CycleEnCoursInfo info = s1.getCycleEnCours();
      assertThat(info.getNumeroCycle()).isEqualTo(3);
      assertThat(info.getBeneficiaireNom()).isEqualTo("Bénéficiaire Test");
      assertThat(info.getBeneficiaireUserId()).isEqualTo(benef.getId());
      assertThat(info.getMembresAyantPaye()).isEqualTo(2);
      assertThat(info.getMembresEnAttente()).isEqualTo(1);
      assertThat(info.getMontantCollecte()).isEqualByComparingTo("2000");
      assertThat(info.getMontantEnAttente()).isEqualByComparingTo("1000");
      assertThat(info.getMontantAttendu()).isEqualByComparingTo("5000");
      assertThat(info.getMembresActifs()).isEqualTo(5);

      MyDashboardResponse.TontineStats s2 = r.getTontines().getContent().get(1);
      assertThat(s2.getCycleEnCours()).isNull();
      assertThat(s2.isGagnantsADesigner()).isFalse();
      assertThat(s2.getNomEvenement()).isEqualTo("Tabaski");
      assertThat(s2.getMontantLibre()).isFalse();
      assertThat(s2.getNombreMembresCotise()).isEqualTo(3);
    }

    @Test
    @DisplayName(
        "tour en cours sans gagnant ni paiement : compteurs à zéro ; montant libre sans minimum -> attendu nul")
    void tourVide() throws Exception {
      String k = simple.getKeycloakId();
      Tontine t = Fixtures.tontine(simple);
      t.setTontineType(TontineType.EVENEMENTIELLE);
      t.setMontantLibre(true);
      t.setMontantMinimum(null);
      when(tontineRepository.findByCreePar_IdAndDeletedAtIsNull(simple.getId(), page))
          .thenReturn(new PageImpl<>(List.of(t)));
      when(membreRepository.findParametresToursEnCoursByCreateur(simple.getId()))
          .thenReturn(List.of());
      CycleTontine tour = Fixtures.cycle(t, 1, CycleStatut.EN_COURS);
      when(cycleRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              t.getId(), CycleStatut.EN_COURS))
          .thenReturn(Optional.of(tour));
      when(cotisationRepository.countAndSumByStatutForCycle(tour.getId())).thenReturn(List.of());

      MyDashboardResponse.CycleEnCoursInfo info =
          service.getMyDashboard(k, page).getTontines().getContent().get(0).getCycleEnCours();

      assertThat(info.getBeneficiaireNom()).isNull();
      assertThat(info.getMembresAyantPaye()).isZero();
      assertThat(info.getMontantCollecte()).isEqualByComparingTo("0");
      assertThat(info.getMontantAttendu()).isNull();
    }
  }
}
