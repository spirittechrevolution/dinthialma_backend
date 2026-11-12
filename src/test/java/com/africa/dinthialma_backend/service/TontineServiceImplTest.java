package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.MonRoleTontine;
import com.africa.dinthialma_backend.tontine.codeList.RoleFiltreTontine;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.dto.ChangerOrdreBeneficiaireRequest;
import com.africa.dinthialma_backend.tontine.dto.CreateTontineRequest;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import com.africa.dinthialma_backend.tontine.dto.UpdateTontineRequest;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.mapper.TontineMapper;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import com.africa.dinthialma_backend.tontine.service.impl.TontineServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("TontineServiceImpl")
class TontineServiceImplTest {

  @Mock private TontineRepository tontineRepository;
  @Spy private TontineMapper tontineMapper = Mappers.getMapper(TontineMapper.class);
  @Mock private CycleTontineRepository cycleRepository;
  @Mock private TontineMembreRepository membreRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserRoleAssignmentRepository roleRepository;
  @Mock private KeycloakAuthService keycloakAuthService;
  @Mock private AuditService auditService;
  @Mock private NotificationService notificationService;
  @InjectMocks private TontineServiceImpl service;

  private User gestionnaire;
  private User etranger;
  private Tontine tontine;
  private final List<CycleTontine> cyclesCrees = new ArrayList<>();

  @BeforeEach
  void setUp() {
    gestionnaire = Fixtures.user("Gestionnaire");
    etranger = Fixtures.user("Étranger");
    tontine = Fixtures.tontine(gestionnaire);
    tontine.setStatut(TontineStatut.BROUILLON);
    lenient()
        .when(userRepository.findByKeycloakId(gestionnaire.getKeycloakId()))
        .thenReturn(Optional.of(gestionnaire));
    lenient()
        .when(userRepository.findByKeycloakId(etranger.getKeycloakId()))
        .thenReturn(Optional.of(etranger));
    lenient()
        .when(tontineRepository.findByIdAndDeletedAtIsNull(tontine.getId()))
        .thenReturn(Optional.of(tontine));
    lenient()
        .when(roleRepository.existsByUserIdAndRole(any(), eq(UserRole.SUPER_ADMIN)))
        .thenReturn(false);
    lenient()
        .when(tontineRepository.save(any()))
        .thenAnswer(
            i -> {
              Tontine t = i.getArgument(0);
              if (t.getId() == null) t.setId(UUID.randomUUID());
              return t;
            });
    lenient()
        .when(cycleRepository.save(any()))
        .thenAnswer(
            i -> {
              cyclesCrees.add(i.getArgument(0));
              return i.getArgument(0);
            });
  }

  private static CreateTontineRequest rotative() {
    CreateTontineRequest r = new CreateTontineRequest();
    r.setTontineType(TontineType.ROTATIVE);
    r.setNom("  Tontine du quartier ");
    r.setMontant(new BigDecimal("5000"));
    r.setFrequence("MENSUEL");
    r.setOrdreBeneficiaire("ROTATION");
    r.setModeCycle(ModeCycle.MANUEL);
    r.setDateDebut(LocalDate.of(2026, 1, 1));
    r.setNombreMembres(5);
    return r;
  }

  private static CreateTontineRequest requeteEvenementielle() {
    CreateTontineRequest r = rotative();
    r.setTontineType(TontineType.EVENEMENTIELLE);
    r.setNomEvenement("Tabaski 2026");
    r.setDateEcheance(LocalDate.of(2026, 3, 31));
    r.setNombreMembres(null);
    return r;
  }

  @Nested
  @DisplayName("création")
  class Creation {

    @Test
    @DisplayName(
        "ROTATIVE : BROUILLON, nom rogné, créateur promu ADMIN (Keycloak + base), relation GESTIONNAIRE")
    void rotativeCreee() throws Exception {
      when(roleRepository.existsByUserIdAndRole(gestionnaire.getId(), UserRole.ADMIN))
          .thenReturn(false);

      TontineResponse r = service.createTontine(gestionnaire.getKeycloakId(), rotative());

      assertThat(r.getNom()).isEqualTo("Tontine du quartier");
      assertThat(r.getStatut()).isEqualTo(TontineStatut.BROUILLON);
      assertThat(r.getMonRole()).isEqualTo(MonRoleTontine.GESTIONNAIRE);
      assertThat(r.getNombreMembresActuels()).isZero();
      verify(keycloakAuthService).assignRole(gestionnaire.getKeycloakId(), "DINTHIALMA_ADMIN");
      ArgumentCaptor<UserRoleAssignment> role = ArgumentCaptor.forClass(UserRoleAssignment.class);
      verify(roleRepository).save(role.capture());
      assertThat(role.getValue().getRole()).isEqualTo(UserRole.ADMIN);
      verify(auditService).logCreate(eq(gestionnaire), eq("tontines"), any());
    }

    @Test
    @DisplayName("type absent -> ROTATIVE ; ADMIN déjà présent : aucun rôle réattribué")
    void typeParDefaut() throws Exception {
      when(roleRepository.existsByUserIdAndRole(gestionnaire.getId(), UserRole.ADMIN))
          .thenReturn(true);
      CreateTontineRequest r = rotative();
      r.setTontineType(null);
      assertThat(service.createTontine(gestionnaire.getKeycloakId(), r).getTontineType())
          .isEqualTo(TontineType.ROTATIVE);
      verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Keycloak indisponible : tontine créée, rôle ADMIN marqué non synchronisé")
    void roleNonSynchronise() throws Exception {
      when(roleRepository.existsByUserIdAndRole(gestionnaire.getId(), UserRole.ADMIN))
          .thenReturn(false);
      doThrow(new CustomException(HttpStatus.BAD_GATEWAY, "KO"))
          .when(keycloakAuthService)
          .assignRole(any(), any());
      service.createTontine(gestionnaire.getKeycloakId(), rotative());
      ArgumentCaptor<UserRoleAssignment> role = ArgumentCaptor.forClass(UserRoleAssignment.class);
      verify(roleRepository).save(role.capture());
      assertThat(role.getValue().isSyncedToKeycloak()).isFalse();
    }

    @Test
    @DisplayName(
        "EVENEMENTIELLE : champs rotatifs neutralisés, mode AUTOMATIQUE, montant libre -> minimum")
    void evenementielle() throws Exception {
      when(roleRepository.existsByUserIdAndRole(gestionnaire.getId(), UserRole.ADMIN))
          .thenReturn(true);
      CreateTontineRequest r = requeteEvenementielle();
      r.setMontantLibre(true);
      r.setMontant(null);
      r.setMontantMinimum(new BigDecimal("500"));

      TontineResponse t = service.createTontine(gestionnaire.getKeycloakId(), r);

      assertThat(t.getOrdreBeneficiaire()).isNull();
      assertThat(t.getModeCycle()).isEqualTo(ModeCycle.AUTOMATIQUE);
      assertThat(t.getMontant()).isEqualByComparingTo("500");
      assertThat(t.isMontantLibre()).isTrue();
      assertThat(t.getNomEvenement()).isEqualTo("Tabaski 2026");

      r.setMontantMinimum(null);
      assertThat(service.createTontine(gestionnaire.getKeycloakId(), r).getMontant())
          .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName(
        "ordre des bénéficiaires : HYBRIDE accepté ; valeur inconnue -> 400 INVALID_PARAMETER")
    void ordreBeneficiaire() throws Exception {
      when(roleRepository.existsByUserIdAndRole(gestionnaire.getId(), UserRole.ADMIN))
          .thenReturn(true);
      CreateTontineRequest hybride = rotative();
      hybride.setOrdreBeneficiaire("HYBRIDE");
      assertThat(
              service.createTontine(gestionnaire.getKeycloakId(), hybride).getOrdreBeneficiaire())
          .isEqualTo("HYBRIDE");

      for (String inconnu : new String[] {"FIXE", "hybride", "AUTO"}) {
        CreateTontineRequest r = rotative();
        r.setOrdreBeneficiaire(inconnu);
        assertThatThrownBy(() -> service.createTontine(gestionnaire.getKeycloakId(), r))
            .isInstanceOfSatisfying(
                BadRequestException.class,
                e -> {
                  assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETER);
                  assertThat(e.getMessage()).contains("HYBRIDE");
                });
      }

      UpdateTontineRequest maj = new UpdateTontineRequest();
      maj.setOrdreBeneficiaire("FIXE");
      assertThatThrownBy(
              () -> service.updateTontine(gestionnaire.getKeycloakId(), tontine.getId(), maj))
          .isInstanceOf(BadRequestException.class);
      maj.setOrdreBeneficiaire("HYBRIDE");
      assertThat(
              service
                  .updateTontine(gestionnaire.getKeycloakId(), tontine.getId(), maj)
                  .getOrdreBeneficiaire())
          .isEqualTo("HYBRIDE");
    }

    @Test
    @DisplayName("validations ROTATIVE : ordre, montant, nombre de membres (>= 2), mode de cycle")
    void validationsRotative() {
      List<Consumer<CreateTontineRequest>> defauts =
          List.of(
              r -> r.setOrdreBeneficiaire(" "),
              r -> r.setMontant(BigDecimal.ZERO),
              r -> r.setMontant(null),
              r -> r.setNombreMembres(1),
              r -> r.setNombreMembres(null),
              r -> r.setModeCycle(null));
      for (Consumer<CreateTontineRequest> defaut : defauts) {
        CreateTontineRequest r = rotative();
        defaut.accept(r);
        assertThatThrownBy(() -> service.createTontine(gestionnaire.getKeycloakId(), r))
            .isInstanceOf(BadRequestException.class);
      }
      verify(tontineRepository, never()).save(any());
    }

    @Test
    @DisplayName(
        "validations EVENEMENTIELLE : échéance obligatoire et postérieure, montant fixe obligatoire")
    void validationsEvenementielle() {
      List<Consumer<CreateTontineRequest>> defauts =
          List.of(
              r -> r.setDateEcheance(null),
              r -> r.setDateEcheance(r.getDateDebut()),
              r -> r.setMontant(null));
      for (Consumer<CreateTontineRequest> defaut : defauts) {
        CreateTontineRequest r = requeteEvenementielle();
        defaut.accept(r);
        assertThatThrownBy(() -> service.createTontine(gestionnaire.getKeycloakId(), r))
            .isInstanceOf(BadRequestException.class);
      }
    }
  }

  @Nested
  @DisplayName("lecture")
  class Lecture {

    @Test
    @DisplayName("détail : membre -> relation MEMBRE avec son identifiant ; tiers -> 403")
    void detail() throws Exception {
      User awa = Fixtures.user("Awa");
      TontineMembre m = Fixtures.membre(tontine, awa, 2);
      when(userRepository.findByKeycloakId(awa.getKeycloakId())).thenReturn(Optional.of(awa));
      when(membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
              tontine.getId(), awa.getId()))
          .thenReturn(true);
      when(membreRepository.findByTontine_IdAndUser_IdAndDeletedAtIsNull(
              tontine.getId(), awa.getId()))
          .thenReturn(Optional.of(m));
      when(membreRepository.countByTontine_IdAndDeletedAtIsNull(tontine.getId())).thenReturn(4L);

      TontineResponse r = service.getTontine(awa.getKeycloakId(), tontine.getId());

      assertThat(r.getMonRole()).isEqualTo(MonRoleTontine.MEMBRE);
      assertThat(r.getMonMembreId()).isEqualTo(m.getId());
      assertThat(r.getMonStatutMembre()).isEqualTo(MembreStatut.ACTIF);
      assertThat(r.getNombreMembresActuels()).isEqualTo(4);

      assertThatThrownBy(() -> service.getTontine(etranger.getKeycloakId(), tontine.getId()))
          .isInstanceOf(ForbiddenException.class);
      UUID inconnue = UUID.randomUUID();
      when(tontineRepository.findByIdAndDeletedAtIsNull(inconnue)).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.getTontine(gestionnaire.getKeycloakId(), inconnue))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("SUPER_ADMIN : détail en LECTURE sans être membre")
    void superAdmin() throws Exception {
      when(roleRepository.existsByUserIdAndRole(etranger.getId(), UserRole.SUPER_ADMIN))
          .thenReturn(true);
      assertThat(service.getTontine(etranger.getKeycloakId(), tontine.getId()).getMonRole())
          .isEqualTo(MonRoleTontine.LECTURE);
    }

    @Test
    @DisplayName(
        "liste : relation de l'appelant calculée par tontine (GERE, COTISE, défaut, SUPER_ADMIN)")
    @SuppressWarnings("unchecked")
    void liste() throws Exception {
      User awa = Fixtures.user("Awa");
      Tontine cotisee = Fixtures.tontine(gestionnaire);
      TontineMembre m = Fixtures.membre(cotisee, awa, 1);
      Tontine geree = Fixtures.tontine(awa);
      PageRequest page = PageRequest.of(0, 20);
      when(userRepository.findByKeycloakId(awa.getKeycloakId())).thenReturn(Optional.of(awa));
      when(tontineRepository.findAll(any(Specification.class), eq(page)))
          .thenReturn(new PageImpl<>(List.of(cotisee, geree)));
      when(membreRepository.findByUser_IdAndTontine_IdInAndDeletedAtIsNull(
              eq(awa.getId()), anyList()))
          .thenReturn(List.of(m));

      for (RoleFiltreTontine role :
          new RoleFiltreTontine[] {null, RoleFiltreTontine.GERE, RoleFiltreTontine.COTISE}) {
        List<TontineResponse> r =
            service.listTontines(awa.getKeycloakId(), role, null, null, page).getContent();
        assertThat(r)
            .extracting(TontineResponse::getMonRole)
            .containsExactly(MonRoleTontine.MEMBRE, MonRoleTontine.GESTIONNAIRE);
        assertThat(r.get(0).getMonMembreId()).isEqualTo(m.getId());
      }

      when(roleRepository.existsByUserIdAndRole(etranger.getId(), UserRole.SUPER_ADMIN))
          .thenReturn(true);
      when(tontineRepository.findAll(any(Specification.class), eq(page)))
          .thenReturn(new PageImpl<>(List.of()));
      assertThat(
              service.listTontines(etranger.getKeycloakId(), null, TontineStatut.ACTIVE, "x", page))
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("modification")
  class Modification {

    @Test
    @DisplayName("BROUILLON : chaque champ fourni est modifié et audité")
    void tousLesChamps() throws Exception {
      UpdateTontineRequest r = new UpdateTontineRequest();
      r.setNom(" Nouveau ");
      r.setDescription("Desc");
      r.setMontant(new BigDecimal("2000"));
      r.setFrequence("HEBDOMADAIRE");
      r.setOrdreBeneficiaire("ALEATOIRE");
      r.setDateDebut(LocalDate.of(2026, 2, 1));
      r.setNombreMembres(8);
      r.setNombreGagnants(2);

      TontineResponse t = service.updateTontine(gestionnaire.getKeycloakId(), tontine.getId(), r);

      assertThat(t.getNom()).isEqualTo("Nouveau");
      assertThat(t.getMontant()).isEqualByComparingTo("2000");
      assertThat(t.getNombreMembres()).isEqualTo(8);
      assertThat(t.getNombreGagnants()).isEqualTo(2);
      assertThat(t.getFrequence()).isEqualTo("HEBDOMADAIRE");
      verify(auditService)
          .log(gestionnaire, "tontines", tontine.getId(), "montant", "1000", "2000");
      verify(auditService, org.mockito.Mockito.times(8))
          .log(eq(gestionnaire), eq("tontines"), eq(tontine.getId()), any(), any(), any());
    }

    @Test
    @DisplayName(
        "champs EVENEMENTIELLE appliqués seulement à ce type ; échéance antérieure refusée")
    void champsEvenementiels() throws Exception {
      UpdateTontineRequest r = new UpdateTontineRequest();
      r.setDateEcheance(tontine.getDateDebut().plusMonths(2));
      r.setNomEvenement("Korité");
      r.setMontantMinimum(new BigDecimal("300"));
      r.setMontantLibre(true);

      service.updateTontine(gestionnaire.getKeycloakId(), tontine.getId(), r);
      assertThat(tontine.getNomEvenement()).isNull();

      tontine.setTontineType(TontineType.EVENEMENTIELLE);
      service.updateTontine(gestionnaire.getKeycloakId(), tontine.getId(), r);
      assertThat(tontine.getNomEvenement()).isEqualTo("Korité");
      assertThat(tontine.isMontantLibre()).isTrue();
      assertThat(tontine.getMontantMinimum()).isEqualByComparingTo("300");
      assertThat(tontine.getDateEcheance()).isEqualTo(tontine.getDateDebut().plusMonths(2));

      r.setDateEcheance(tontine.getDateDebut());
      assertThatThrownBy(
              () -> service.updateTontine(gestionnaire.getKeycloakId(), tontine.getId(), r))
          .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("hors BROUILLON -> 400 ; tiers -> 403")
    void refus() {
      assertThatThrownBy(
              () ->
                  service.updateTontine(
                      etranger.getKeycloakId(), tontine.getId(), new UpdateTontineRequest()))
          .isInstanceOf(ForbiddenException.class);
      tontine.setStatut(TontineStatut.ACTIVE);
      assertThatThrownBy(
              () ->
                  service.updateTontine(
                      gestionnaire.getKeycloakId(), tontine.getId(), new UpdateTontineRequest()))
          .isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  @DisplayName("suppression : logique en BROUILLON ; refusée sinon")
  void suppression() throws Exception {
    service.deleteTontine(gestionnaire.getKeycloakId(), tontine.getId());
    assertThat(tontine.getDeletedAt()).isNotNull();
    verify(auditService).logDelete(gestionnaire, "tontines", tontine.getId());

    tontine.setStatut(TontineStatut.ACTIVE);
    assertThatThrownBy(() -> service.deleteTontine(gestionnaire.getKeycloakId(), tontine.getId()))
        .isInstanceOf(BadRequestException.class);
  }

  @Nested
  @DisplayName("activation et suspension")
  class Activation {

    @Test
    @DisplayName("ROTATIVE MANUEL : ACTIVE sans génération de cycles")
    void manuel() throws Exception {
      assertThat(service.activateTontine(gestionnaire.getKeycloakId(), tontine.getId()).getStatut())
          .isEqualTo(TontineStatut.ACTIVE);
      assertThat(cyclesCrees).isEmpty();
      verify(auditService)
          .log(gestionnaire, "tontines", tontine.getId(), "statut", "BROUILLON", "ACTIVE");
    }

    @Test
    @DisplayName(
        "ROTATIVE AUTOMATIQUE : 1 cycle par groupe de gagnants, premier EN_COURS, dates mensuelles")
    void automatique() throws Exception {
      tontine.setModeCycle(ModeCycle.AUTOMATIQUE);
      tontine.setNombreGagnants(2);
      tontine.setDateDebut(LocalDate.of(2026, 1, 1));
      List<TontineMembre> membres = new ArrayList<>();
      for (int i = 5; i >= 1; i--) membres.add(Fixtures.membre(tontine, Fixtures.user("M" + i), i));
      when(membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), MembreStatut.ACTIF))
          .thenReturn(membres);

      service.activateTontine(gestionnaire.getKeycloakId(), tontine.getId());

      assertThat(cyclesCrees).hasSize(3);
      assertThat(cyclesCrees)
          .extracting(CycleTontine::getStatut)
          .containsExactly(CycleStatut.EN_COURS, CycleStatut.EN_ATTENTE, CycleStatut.EN_ATTENTE);
      assertThat(cyclesCrees.get(0).getDateFin()).isEqualTo(LocalDate.of(2026, 1, 31));
      assertThat(cyclesCrees.get(1).getDateDebut()).isEqualTo(LocalDate.of(2026, 2, 1));
      verify(membreRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("ordre ALEATOIRE : positions 1..n redistribuées et enregistrées")
    void aleatoire() throws Exception {
      tontine.setModeCycle(ModeCycle.AUTOMATIQUE);
      tontine.setOrdreBeneficiaire("ALEATOIRE");
      List<TontineMembre> membres =
          new ArrayList<>(
              List.of(
                  Fixtures.membre(tontine, Fixtures.user("A"), 7),
                  Fixtures.membre(tontine, Fixtures.user("B"), 9),
                  Fixtures.membre(tontine, Fixtures.user("C"), 8)));
      when(membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), MembreStatut.ACTIF))
          .thenReturn(membres);

      service.activateTontine(gestionnaire.getKeycloakId(), tontine.getId());

      assertThat(membres)
          .extracting(TontineMembre::getOrdreJackpot)
          .containsExactlyInAnyOrder(1, 2, 3);
      verify(membreRepository).saveAll(any());
      assertThat(cyclesCrees).hasSize(3);
    }

    @Test
    @DisplayName("cycles déjà présents (réactivation) : aucune nouvelle génération")
    void reactivation() throws Exception {
      tontine.setModeCycle(ModeCycle.AUTOMATIQUE);
      tontine.setStatut(TontineStatut.SUSPENDUE);
      when(cycleRepository.existsByTontine_IdAndDeletedAtIsNull(tontine.getId())).thenReturn(true);
      service.activateTontine(gestionnaire.getKeycloakId(), tontine.getId());
      assertThat(cyclesCrees).isEmpty();
    }

    @Test
    @DisplayName("EVENEMENTIELLE hebdomadaire : sous-cycles jusqu'à l'échéance, le dernier tronqué")
    void evenementielle() throws Exception {
      tontine.setTontineType(TontineType.EVENEMENTIELLE);
      tontine.setFrequence("HEBDOMADAIRE");
      tontine.setDateDebut(LocalDate.of(2026, 1, 1));
      tontine.setDateEcheance(LocalDate.of(2026, 1, 17));

      service.activateTontine(gestionnaire.getKeycloakId(), tontine.getId());

      assertThat(cyclesCrees).hasSize(3);
      assertThat(cyclesCrees.get(0).getDateFin()).isEqualTo(LocalDate.of(2026, 1, 7));
      assertThat(cyclesCrees.get(2).getDateDebut()).isEqualTo(LocalDate.of(2026, 1, 15));
      assertThat(cyclesCrees.get(2).getDateFin()).isEqualTo(LocalDate.of(2026, 1, 17));
      assertThat(cyclesCrees.get(0).getStatut()).isEqualTo(CycleStatut.EN_COURS);
    }

    @Test
    @DisplayName("durées des fréquences : journalière, bimensuelle, trimestrielle")
    void frequences() throws Exception {
      tontine.setTontineType(TontineType.EVENEMENTIELLE);
      tontine.setDateDebut(LocalDate.of(2026, 1, 1));

      tontine.setFrequence("journaliere");
      tontine.setDateEcheance(LocalDate.of(2026, 1, 3));
      service.activateTontine(gestionnaire.getKeycloakId(), tontine.getId());
      assertThat(cyclesCrees).hasSize(3);

      cyclesCrees.clear();
      tontine.setStatut(TontineStatut.BROUILLON);
      tontine.setFrequence("BIMENSUEL");
      tontine.setDateEcheance(LocalDate.of(2026, 1, 28));
      service.activateTontine(gestionnaire.getKeycloakId(), tontine.getId());
      assertThat(cyclesCrees)
          .extracting(CycleTontine::getDateFin)
          .containsExactly(LocalDate.of(2026, 1, 14), LocalDate.of(2026, 1, 28));

      cyclesCrees.clear();
      tontine.setStatut(TontineStatut.BROUILLON);
      tontine.setFrequence("TRIMESTRIEL");
      tontine.setDateEcheance(LocalDate.of(2026, 12, 31));
      service.activateTontine(gestionnaire.getKeycloakId(), tontine.getId());
      assertThat(cyclesCrees).hasSize(4);
      assertThat(cyclesCrees.get(0).getDateFin()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    @DisplayName("activation d'une tontine ACTIVE ou TERMINEE refusée")
    void activationRefusee() {
      tontine.setStatut(TontineStatut.ACTIVE);
      assertThatThrownBy(
              () -> service.activateTontine(gestionnaire.getKeycloakId(), tontine.getId()))
          .isInstanceOf(BadRequestException.class);
      tontine.setStatut(TontineStatut.TERMINEE);
      assertThatThrownBy(
              () -> service.activateTontine(gestionnaire.getKeycloakId(), tontine.getId()))
          .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("suspension : ACTIVE -> SUSPENDUE, auditée ; refusée hors ACTIVE")
    void suspension() throws Exception {
      tontine.setStatut(TontineStatut.ACTIVE);
      assertThat(service.suspendTontine(gestionnaire.getKeycloakId(), tontine.getId()).getStatut())
          .isEqualTo(TontineStatut.SUSPENDUE);
      verify(auditService)
          .log(gestionnaire, "tontines", tontine.getId(), "statut", "ACTIVE", "SUSPENDUE");
      assertThatThrownBy(
              () -> service.suspendTontine(gestionnaire.getKeycloakId(), tontine.getId()))
          .isInstanceOf(BadRequestException.class);
    }
  }

  @Nested
  @DisplayName("changement d'ordre des bénéficiaires en cours de tontine")
  class ChangementOrdre {

    private ChangerOrdreBeneficiaireRequest vers(String ordre) {
      ChangerOrdreBeneficiaireRequest r = new ChangerOrdreBeneficiaireRequest();
      r.setOrdreBeneficiaire(ordre);
      return r;
    }

    private String kc() {
      return gestionnaire.getKeycloakId();
    }

    @Test
    @DisplayName("ACTIVE : ROTATION -> HYBRIDE appliqué, audité, chaque membre actif notifié")
    void active() throws Exception {
      tontine.setStatut(TontineStatut.ACTIVE);
      lenient()
          .when(
              membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
                  tontine.getId(), MembreStatut.ACTIF))
          .thenReturn(
              List.of(
                  Fixtures.membre(tontine, Fixtures.user("A"), 1),
                  Fixtures.membre(tontine, Fixtures.user("B"), 2)));

      TontineResponse r =
          service.changerOrdreBeneficiaire(kc(), tontine.getId(), vers(" HYBRIDE "));

      assertThat(r.getOrdreBeneficiaire()).isEqualTo("HYBRIDE");
      verify(auditService)
          .log(
              gestionnaire,
              "tontines",
              tontine.getId(),
              "ordreBeneficiaire",
              "ROTATION",
              "HYBRIDE");
      verify(notificationService, org.mockito.Mockito.times(2))
          .notify(
              any(),
              eq(NotificationType.ORDRE_BENEFICIAIRE_MODIFIE),
              any(),
              org.mockito.ArgumentMatchers.contains("à chaque tour"),
              eq(tontine.getId()));
    }

    @Test
    @DisplayName("libellés des notifications pour ROTATION, ALEATOIRE et MANUEL")
    void libelles() throws Exception {
      tontine.setStatut(TontineStatut.SUSPENDUE);
      tontine.setOrdreBeneficiaire("MANUEL");
      when(membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), MembreStatut.ACTIF))
          .thenReturn(List.of(Fixtures.membre(tontine, Fixtures.user("A"), 1)));
      for (String[] cas :
          new String[][] {
            {"ROTATION", "ordre de passage"},
            {"ALEATOIRE", "tirage au sort"},
            {"MANUEL", "choix du gestionnaire"}
          }) {
        service.changerOrdreBeneficiaire(kc(), tontine.getId(), vers(cas[0]));
        verify(notificationService)
            .notify(
                any(),
                eq(NotificationType.ORDRE_BENEFICIAIRE_MODIFIE),
                any(),
                org.mockito.ArgumentMatchers.contains(cas[1]),
                any());
      }
    }

    @Test
    @DisplayName("même valeur : rien n'est modifié ni notifié")
    void inchange() throws Exception {
      tontine.setStatut(TontineStatut.ACTIVE);
      service.changerOrdreBeneficiaire(kc(), tontine.getId(), vers("ROTATION"));
      verify(tontineRepository, never()).save(any());
      verify(notificationService, never()).notify(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("vers un ordre automatique : refusé tant qu'un tour attend ses gagnants")
    void designationEnAttente() throws Exception {
      tontine.setStatut(TontineStatut.ACTIVE);
      tontine.setOrdreBeneficiaire("HYBRIDE");
      when(cycleRepository.existsTermineSansGagnant(tontine.getId())).thenReturn(true);

      assertThatThrownBy(
              () -> service.changerOrdreBeneficiaire(kc(), tontine.getId(), vers("ALEATOIRE")))
          .isInstanceOfSatisfying(
              ConflictException.class,
              e -> assertThat(e.getCode()).isEqualTo(ErrorCode.DESIGNATION_EN_ATTENTE));

      // Vers MANUEL, le tour en attente reste désignable : accepté
      assertThat(
              service
                  .changerOrdreBeneficiaire(kc(), tontine.getId(), vers("MANUEL"))
                  .getOrdreBeneficiaire())
          .isEqualTo("MANUEL");
    }

    @Test
    @DisplayName("refus : événementielle, terminée, valeur inconnue, non gestionnaire")
    void refus() {
      tontine.setStatut(TontineStatut.TERMINEE);
      assertThatThrownBy(
              () -> service.changerOrdreBeneficiaire(kc(), tontine.getId(), vers("HYBRIDE")))
          .isInstanceOf(BadRequestException.class);

      tontine.setStatut(TontineStatut.ACTIVE);
      tontine.setTontineType(TontineType.EVENEMENTIELLE);
      assertThatThrownBy(
              () -> service.changerOrdreBeneficiaire(kc(), tontine.getId(), vers("HYBRIDE")))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("rotative");

      tontine.setTontineType(TontineType.ROTATIVE);
      assertThatThrownBy(
              () -> service.changerOrdreBeneficiaire(kc(), tontine.getId(), vers("FIXE")))
          .isInstanceOfSatisfying(
              BadRequestException.class,
              e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETER));

      assertThatThrownBy(
              () ->
                  service.changerOrdreBeneficiaire(
                      etranger.getKeycloakId(), tontine.getId(), vers("HYBRIDE")))
          .isInstanceOf(ForbiddenException.class);
    }
  }
}
