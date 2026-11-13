package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.jackpot.service.interfaces.PrioriteJackpotService;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.service.SchedulerService;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.calendrier.Rythme;
import com.africa.dinthialma_backend.tontine.codeList.CommissionType;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.codeList.UniteRythme;
import com.africa.dinthialma_backend.tontine.dto.CycleResponse;
import com.africa.dinthialma_backend.tontine.dto.OpenCycleRequest;
import com.africa.dinthialma_backend.tontine.dto.SelectionnerBeneficiaireRequest;
import com.africa.dinthialma_backend.tontine.entity.CycleGagnant;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.DistributionEvenementielle;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.entity.TontineCommission;
import com.africa.dinthialma_backend.tontine.mapper.CycleMapper;
import com.africa.dinthialma_backend.tontine.repository.CycleGagnantRepository;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.DistributionEvenementielleRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineCommissionRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import com.africa.dinthialma_backend.tontine.service.impl.CycleServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("CycleServiceImpl")
class CycleServiceImplTest {

  @Mock private CycleTontineRepository cycleRepository;
  @Spy private CycleMapper cycleMapper = Mappers.getMapper(CycleMapper.class);
  @Mock private CycleGagnantRepository gagnantRepository;
  @Mock private TontineRepository tontineRepository;
  @Mock private TontineMembreRepository membreRepository;
  @Mock private CotisationRepository cotisationRepository;
  @Mock private TontineCommissionRepository commissionRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserRoleAssignmentRepository roleRepository;
  @Mock private AuditService auditService;
  @Mock private SchedulerService schedulerService;
  @Mock private WhatsappService whatsappService;
  @Mock private NotificationService notificationService;
  @Mock private DistributionEvenementielleRepository distributionRepository;
  @Mock private PrioriteJackpotService prioriteService;
  @InjectMocks private CycleServiceImpl service;

  private User gestionnaire;
  private User etranger;
  private Tontine tontine;
  private String kc;

  @BeforeEach
  void setUp() {
    gestionnaire = Fixtures.user("Gestionnaire");
    etranger = Fixtures.user("Étranger");
    tontine = Fixtures.tontine(gestionnaire);
    kc = gestionnaire.getKeycloakId();
    lenient().when(userRepository.findByKeycloakId(kc)).thenReturn(Optional.of(gestionnaire));
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
        .when(cycleRepository.save(any()))
        .thenAnswer(
            i -> {
              CycleTontine c = i.getArgument(0);
              if (c.getId() == null) c.setId(UUID.randomUUID());
              return c;
            });
    lenient().when(gagnantRepository.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  private CycleTontine cycleConnu(int numero, CycleStatut statut) {
    CycleTontine c = Fixtures.cycle(tontine, numero, statut);
    lenient()
        .when(cycleRepository.findByIdAndTontine_IdAndDeletedAtIsNull(c.getId(), tontine.getId()))
        .thenReturn(Optional.of(c));
    return c;
  }

  private Cotisation cotisation(
      TontineMembre m, CycleTontine c, CotisationStatut statut, String montant) {
    Cotisation co = Fixtures.cotisation(m, c, statut);
    co.setMontant(new BigDecimal(montant));
    return co;
  }

  private TontineCommission commission(CommissionType type, String valeur) {
    return TontineCommission.builder()
        .id(UUID.randomUUID())
        .tontine(tontine)
        .type(type)
        .valeur(new BigDecimal(valeur))
        .build();
  }

  private List<TontineMembre> membresActifs(int n) {
    List<TontineMembre> membres = new ArrayList<>();
    for (int i = 1; i <= n; i++) membres.add(Fixtures.membre(tontine, Fixtures.user("M" + i), i));
    lenient()
        .when(
            membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
                tontine.getId(), MembreStatut.ACTIF))
        .thenReturn(membres);
    return membres;
  }

  @Nested
  @DisplayName("lecture")
  class Lecture {

    @Test
    @DisplayName("liste, détail et historique des bénéficiaires pour un membre ; tiers -> 403")
    void lecture() throws Exception {
      User awa = Fixtures.user("Awa");
      when(userRepository.findByKeycloakId(awa.getKeycloakId())).thenReturn(Optional.of(awa));
      when(membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
              tontine.getId(), awa.getId()))
          .thenReturn(true);
      CycleTontine c = cycleConnu(1, CycleStatut.TERMINE);
      PageRequest page = PageRequest.of(0, 20);
      when(cycleRepository.findByTontine_IdAndDeletedAtIsNull(tontine.getId(), page))
          .thenReturn(new PageImpl<>(List.of(c)));
      when(cycleRepository.findTerminesAvecGagnants(tontine.getId(), CycleStatut.TERMINE, page))
          .thenReturn(new PageImpl<>(List.of(c)));

      assertThat(service.listCycles(awa.getKeycloakId(), tontine.getId(), page).getContent())
          .hasSize(1);
      assertThat(service.getCycle(awa.getKeycloakId(), tontine.getId(), c.getId()).getNumeroCycle())
          .isEqualTo(1);
      assertThat(
              service
                  .listBeneficiaires(awa.getKeycloakId(), tontine.getId(), page)
                  .getContent()
                  .get(0)
                  .getCycleId())
          .isEqualTo(c.getId());

      assertThatThrownBy(() -> service.listCycles(etranger.getKeycloakId(), tontine.getId(), page))
          .isInstanceOf(ForbiddenException.class);
      UUID inconnu = UUID.randomUUID();
      when(cycleRepository.findByIdAndTontine_IdAndDeletedAtIsNull(inconnu, tontine.getId()))
          .thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.getCycle(kc, tontine.getId(), inconnu))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("ouverture manuelle")
  class Ouverture {

    private OpenCycleRequest req(LocalDate debut, LocalDate fin) {
      OpenCycleRequest r = new OpenCycleRequest();
      r.setDateDebut(debut);
      r.setDateFin(fin);
      return r;
    }

    @Test
    @DisplayName("numéro suivant, EN_COURS, audité, chaque membre actif notifié (CYCLE_OUVERT)")
    void succes() throws Exception {
      membresActifs(3);
      when(cycleRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), CycleStatut.EN_COURS))
          .thenReturn(Optional.empty());
      when(cycleRepository.findTopByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleDesc(
              tontine.getId()))
          .thenReturn(Optional.of(Fixtures.cycle(tontine, 4, CycleStatut.TERMINE)));

      CycleResponse r =
          service.openCycle(
              kc, tontine.getId(), req(LocalDate.now(), LocalDate.now().plusDays(30)));

      assertThat(r.getNumeroCycle()).isEqualTo(5);
      assertThat(r.getStatut()).isEqualTo(CycleStatut.EN_COURS);
      verify(auditService).logCreate(eq(gestionnaire), eq("cycles_tontine"), any());
      verify(notificationService, times(3))
          .notify(
              any(),
              eq(NotificationType.CYCLE_OUVERT),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
    }

    @Test
    @DisplayName("premier cycle numéroté 1")
    void premier() throws Exception {
      membresActifs(0);
      when(cycleRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), CycleStatut.EN_COURS))
          .thenReturn(Optional.empty());
      when(cycleRepository.findTopByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleDesc(
              tontine.getId()))
          .thenReturn(Optional.empty());
      assertThat(
              service
                  .openCycle(kc, tontine.getId(), req(LocalDate.now(), LocalDate.now()))
                  .getNumeroCycle())
          .isEqualTo(1);
    }

    @Test
    @DisplayName("sans dateFin (B26) : calculée selon le rythme, échéance créée avec le tour")
    void dateFinCalculee() throws Exception {
      membresActifs(0);
      tontine.appliquerRythme(new Rythme(1, UniteRythme.SEMAINE, "DIMANCHE", LocalTime.of(20, 0)));
      when(cycleRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), CycleStatut.EN_COURS))
          .thenReturn(Optional.empty());
      CycleTontine precedent = Fixtures.cycle(tontine, 1, CycleStatut.TERMINE);
      precedent.setDateFin(LocalDate.of(2026, 11, 15));
      when(cycleRepository.findTopByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleDesc(
              tontine.getId()))
          .thenReturn(Optional.of(precedent));
      ArgumentCaptor<CycleTontine> enregistre = ArgumentCaptor.forClass(CycleTontine.class);

      CycleResponse r =
          service.openCycle(kc, tontine.getId(), req(LocalDate.of(2026, 11, 16), null));

      assertThat(r.getDateFin()).isEqualTo(LocalDate.of(2026, 11, 22));
      assertThat(r.getEcheanceAt())
          .isEqualTo(OffsetDateTime.of(2026, 11, 22, 20, 0, 0, 0, ZoneOffset.UTC));
      verify(cycleRepository).save(enregistre.capture());
      assertThat(enregistre.getValue().getEcheances()).hasSize(1);
    }

    @Test
    @DisplayName(
        "refus : cycle déjà en cours (CYCLE_ALREADY_IN_PROGRESS), dates inversées, statut, mode, type, tiers")
    void refus() {
      OpenCycleRequest ok = req(LocalDate.now(), LocalDate.now().plusDays(1));
      when(cycleRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), CycleStatut.EN_COURS))
          .thenReturn(Optional.of(Fixtures.cycle(tontine, 1, CycleStatut.EN_COURS)));
      assertThatThrownBy(() -> service.openCycle(kc, tontine.getId(), ok))
          .isInstanceOfSatisfying(
              BadRequestException.class,
              e -> assertThat(e.getCode()).isEqualTo(ErrorCode.CYCLE_ALREADY_IN_PROGRESS));

      assertThatThrownBy(
              () ->
                  service.openCycle(
                      kc, tontine.getId(), req(LocalDate.now(), LocalDate.now().minusDays(1))))
          .isInstanceOf(BadRequestException.class);
      tontine.setStatut(TontineStatut.SUSPENDUE);
      assertThatThrownBy(() -> service.openCycle(kc, tontine.getId(), ok))
          .isInstanceOf(BadRequestException.class);
      tontine.setModeCycle(ModeCycle.AUTOMATIQUE);
      assertThatThrownBy(() -> service.openCycle(kc, tontine.getId(), ok))
          .isInstanceOf(BadRequestException.class);
      tontine.setTontineType(TontineType.EVENEMENTIELLE);
      assertThatThrownBy(() -> service.openCycle(kc, tontine.getId(), ok))
          .isInstanceOf(BadRequestException.class);
      assertThatThrownBy(() -> service.openCycle(etranger.getKeycloakId(), tontine.getId(), ok))
          .isInstanceOf(ForbiddenException.class);
      verify(cycleRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("clôture ROTATIVE")
  class ClotureRotative {

    @Test
    @DisplayName(
        "jackpot = VALIDE, commissions (10 % + 200 fixes, adhésion ignorée), gagnant ROTATION payé, retards signalés")
    void calculs() throws Exception {
      List<TontineMembre> membres = membresActifs(3);
      membres.get(0).setARecuJackpot(true);
      CycleTontine cycle = cycleConnu(2, CycleStatut.EN_COURS);
      when(cotisationRepository.findByCycle_IdAndStatutAndDeletedAtIsNull(
              cycle.getId(), CotisationStatut.VALIDE))
          .thenReturn(
              List.of(
                  cotisation(membres.get(0), cycle, CotisationStatut.VALIDE, "1000"),
                  cotisation(membres.get(1), cycle, CotisationStatut.VALIDE, "1000")));
      Cotisation enAttente = cotisation(membres.get(2), cycle, CotisationStatut.EN_ATTENTE, "1000");
      when(cotisationRepository.findByCycle_IdAndStatutAndDeletedAtIsNull(
              cycle.getId(), CotisationStatut.EN_ATTENTE))
          .thenReturn(List.of(enAttente));
      when(commissionRepository.findByTontine_IdAndDeletedAtIsNull(tontine.getId()))
          .thenReturn(
              List.of(
                  commission(CommissionType.POURCENTAGE_JACKPOT, "10"),
                  commission(CommissionType.FRAIS_FIXES_PAR_CYCLE, "200"),
                  commission(CommissionType.FRAIS_ADHESION, "5000")));

      CycleResponse r = service.closeCycle(kc, tontine.getId(), cycle.getId());

      assertThat(r.getStatut()).isEqualTo(CycleStatut.TERMINE);
      assertThat(r.getMontantJackpot()).isEqualByComparingTo("2000");
      assertThat(r.getMontantCommission()).isEqualByComparingTo("400");
      assertThat(r.getMontantNet()).isEqualByComparingTo("1600");
      assertThat(r.getDateRemise()).isEqualTo(LocalDate.now());
      assertThat(r.getGagnants())
          .singleElement()
          .satisfies(
              g -> {
                assertThat(g.getMembreId()).isEqualTo(membres.get(1).getId());
                assertThat(g.getMontantRecu()).isEqualByComparingTo("1600");
              });
      assertThat(membres.get(1).isARecuJackpot()).isTrue();
      assertThat(membres.get(1).getDateJackpot()).isNotNull();
      assertThat(enAttente.getStatut()).isEqualTo(CotisationStatut.EN_RETARD);
      verify(schedulerService).annoncerGagnants(eq(cycle), any());
      verify(notificationService, times(2))
          .notify(
              any(),
              eq(NotificationType.JACKPOT_DISTRIBUE),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
      verify(notificationService)
          .notify(
              eq(gestionnaire.getId()),
              eq(NotificationType.PAIEMENT_EN_RETARD),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
      verify(auditService)
          .log(gestionnaire, "cycles_tontine", cycle.getId(), "statut", "EN_COURS", "TERMINE");
    }

    @Test
    @DisplayName(
        "2 gagnants : net partagé en francs entiers, somme exacte (3 001 -> 1 501 + 1 500) ;"
            + " mode AUTOMATIQUE : cycle suivant activé")
    void plusieursGagnantsEtSuivant() throws Exception {
      tontine.setNombreGagnants(2);
      tontine.setModeCycle(ModeCycle.AUTOMATIQUE);
      membresActifs(3);
      CycleTontine cycle = cycleConnu(1, CycleStatut.EN_COURS);
      CycleTontine suivant = Fixtures.cycle(tontine, 2, CycleStatut.EN_ATTENTE);
      CycleTontine apres = Fixtures.cycle(tontine, 3, CycleStatut.EN_ATTENTE);
      when(cotisationRepository.findByCycle_IdAndStatutAndDeletedAtIsNull(
              cycle.getId(), CotisationStatut.VALIDE))
          .thenReturn(
              List.of(
                  cotisation(
                      Fixtures.membre(tontine, gestionnaire, 9),
                      cycle,
                      CotisationStatut.VALIDE,
                      "3001")));
      when(cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(tontine.getId()))
          .thenReturn(List.of(cycle, suivant, apres));

      CycleResponse r = service.closeCycle(kc, tontine.getId(), cycle.getId());

      assertThat(r.getGagnants())
          .extracting(CycleResponse.GagnantInfo::getMontantRecu)
          .usingElementComparator(BigDecimal::compareTo)
          .containsExactly(new BigDecimal("1501"), new BigDecimal("1500"));
      assertThat(suivant.getStatut()).isEqualTo(CycleStatut.EN_COURS);
      assertThat(apres.getStatut()).isEqualTo(CycleStatut.EN_ATTENTE);
    }

    @Test
    @DisplayName("ordre ALEATOIRE : un gagnant éligible tiré ; MANUEL : aucun gagnant à la clôture")
    void aleatoireEtManuel() throws Exception {
      List<TontineMembre> membres = membresActifs(3);
      tontine.setOrdreBeneficiaire("ALEATOIRE");
      CycleTontine c1 = cycleConnu(1, CycleStatut.EN_COURS);
      CycleResponse r = service.closeCycle(kc, tontine.getId(), c1.getId());
      assertThat(r.getGagnants()).hasSize(1);
      assertThat(membres.stream().filter(TontineMembre::isARecuJackpot)).hasSize(1);

      tontine.setOrdreBeneficiaire("MANUEL");
      CycleTontine c2 = cycleConnu(2, CycleStatut.EN_COURS);
      assertThat(service.closeCycle(kc, tontine.getId(), c2.getId()).getGagnants()).isEmpty();
      verify(schedulerService, times(1)).annoncerGagnants(any(), any());
    }

    @Test
    @DisplayName(
        "PAIEMENT_EN_RETARD : « déclaré à temps, non validé » si déclarée avant l'échéance,"
            + " « n'a pas cotisé » sinon")
    void texteRetardSelonPonctualite() throws Exception {
      List<TontineMembre> membres = membresActifs(2);
      CycleTontine c = cycleConnu(1, CycleStatut.EN_COURS);
      Cotisation aTemps = cotisation(membres.get(0), c, CotisationStatut.EN_ATTENTE, "1000");
      Cotisation tardive = cotisation(membres.get(1), c, CotisationStatut.EN_ATTENTE, "1000");
      tardive.setCreatedAt(c.getDateFin().plusDays(1).atTime(8, 0));
      when(cotisationRepository.findByCycle_IdAndStatutAndDeletedAtIsNull(
              c.getId(), CotisationStatut.VALIDE))
          .thenReturn(List.of());
      when(cotisationRepository.findByCycle_IdAndStatutAndDeletedAtIsNull(
              c.getId(), CotisationStatut.EN_ATTENTE))
          .thenReturn(List.of(aTemps, tardive));

      service.closeCycle(kc, tontine.getId(), c.getId());

      verify(notificationService)
          .notify(
              eq(gestionnaire.getId()),
              eq(NotificationType.PAIEMENT_EN_RETARD),
              anyString(),
              org.mockito.ArgumentMatchers.contains("déclaré son paiement à temps"),
              eq(tontine.getId()),
              any());
      verify(notificationService)
          .notify(
              eq(gestionnaire.getId()),
              eq(NotificationType.PAIEMENT_EN_RETARD),
              anyString(),
              org.mockito.ArgumentMatchers.contains("n'a pas cotisé"),
              eq(tontine.getId()),
              any());
      assertThat(aTemps.getStatut()).isEqualTo(CotisationStatut.EN_RETARD);
    }

    @Test
    @DisplayName("tous les membres déjà servis : aucun gagnant ; cycle non EN_COURS -> 400")
    void casLimites() throws Exception {
      membresActifs(2).forEach(m -> m.setARecuJackpot(true));
      CycleTontine c = cycleConnu(1, CycleStatut.EN_COURS);
      assertThat(service.closeCycle(kc, tontine.getId(), c.getId()).getGagnants()).isEmpty();
      verify(gagnantRepository, never()).save(any());

      assertThatThrownBy(() -> service.closeCycle(kc, tontine.getId(), c.getId()))
          .isInstanceOf(BadRequestException.class);
    }
  }

  @Nested
  @DisplayName("clôture EVENEMENTIELLE")
  class ClotureEvenementielle {

    @BeforeEach
    void evenementielle() {
      tontine.setTontineType(TontineType.EVENEMENTIELLE);
    }

    @Test
    @DisplayName(
        "sous-cycle intermédiaire : sans commission ni distribution, sous-cycle suivant activé")
    void intermediaire() throws Exception {
      List<TontineMembre> membres = membresActifs(1);
      CycleTontine c1 = cycleConnu(1, CycleStatut.EN_COURS);
      CycleTontine c2 = Fixtures.cycle(tontine, 2, CycleStatut.EN_ATTENTE);
      when(cotisationRepository.findByCycle_IdAndStatutAndDeletedAtIsNull(
              c1.getId(), CotisationStatut.VALIDE))
          .thenReturn(List.of(cotisation(membres.getFirst(), c1, CotisationStatut.VALIDE, "700")));
      Cotisation enAttente = cotisation(membres.getFirst(), c1, CotisationStatut.EN_ATTENTE, "700");
      when(cotisationRepository.findByCycle_IdAndStatutAndDeletedAtIsNull(
              c1.getId(), CotisationStatut.EN_ATTENTE))
          .thenReturn(List.of(enAttente));
      when(cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(tontine.getId()))
          .thenReturn(List.of(c1, c2));

      CycleResponse r = service.closeCycle(kc, tontine.getId(), c1.getId());

      assertThat(r.getMontantJackpot()).isEqualByComparingTo("700");
      assertThat(r.getMontantCommission()).isEqualByComparingTo("0");
      assertThat(r.getDistributionParMembre()).isNull();
      assertThat(c2.getStatut()).isEqualTo(CycleStatut.EN_COURS);
      assertThat(enAttente.getStatut()).isEqualTo(CotisationStatut.EN_RETARD);
      verify(whatsappService, never()).sendDistributionFinale(any(), any(), any(), any());
    }

    @Test
    @DisplayName(
        "dernier sous-cycle : distribution proportionnelle (10 % + 300 fixes), WhatsApp et notifications")
    void finale() throws Exception {
      List<TontineMembre> membres = membresActifs(2);
      CycleTontine c1 = Fixtures.cycle(tontine, 1, CycleStatut.TERMINE);
      CycleTontine c2 = cycleConnu(2, CycleStatut.EN_COURS);
      when(cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(tontine.getId()))
          .thenReturn(List.of(c1, c2));
      TontineMembre sorti = Fixtures.membre(tontine, Fixtures.user("Sorti"), 9);
      when(cotisationRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), CotisationStatut.VALIDE))
          .thenReturn(
              List.of(
                  cotisation(membres.get(0), c1, CotisationStatut.VALIDE, "1000"),
                  cotisation(membres.get(0), c2, CotisationStatut.VALIDE, "1000"),
                  cotisation(membres.get(1), c1, CotisationStatut.VALIDE, "1000"),
                  cotisation(sorti, c1, CotisationStatut.VALIDE, "5000")));
      when(commissionRepository.findByTontine_IdAndDeletedAtIsNull(tontine.getId()))
          .thenReturn(
              List.of(
                  commission(CommissionType.POURCENTAGE_JACKPOT, "10"),
                  commission(CommissionType.FRAIS_FIXES_PAR_CYCLE, "300"),
                  commission(CommissionType.FRAIS_ADHESION, "1000")));
      doThrow(new RuntimeException("WhatsApp KO"))
          .when(whatsappService)
          .sendDistributionFinaleAdmin(any(), any(), any(), anyInt());

      CycleResponse r = service.closeCycle(kc, tontine.getId(), c2.getId());

      assertThat(r.getDistributionParMembre()).hasSize(2);
      CycleResponse.MembreDistributionInfo awa =
          r.getDistributionParMembre().stream()
              .filter(d -> d.getMembreId().equals(membres.getFirst().getId()))
              .findFirst()
              .orElseThrow();
      assertThat(awa.getMontantCotise()).isEqualByComparingTo("2000");
      // 10 % de 2000 = 200 ; frais fixes : 300 × 2000 / 8000 (cagnotte, sortant compris) = 75
      assertThat(awa.getMontantCommission()).isEqualByComparingTo("275");
      assertThat(awa.getMontantNet()).isEqualByComparingTo("1725");
      assertThat(r.getMontantJackpot()).isEqualByComparingTo("3000");

      // V034 : une ligne par membre distribué, montants identiques à la réponse
      ArgumentCaptor<List<DistributionEvenementielle>> lignes = ArgumentCaptor.forClass(List.class);
      verify(distributionRepository).saveAll(lignes.capture());
      assertThat(lignes.getValue()).hasSize(2);
      DistributionEvenementielle ligneAwa =
          lignes.getValue().stream()
              .filter(l -> l.getMontantCotise().compareTo(new BigDecimal("2000")) == 0)
              .findFirst()
              .orElseThrow();
      assertThat(ligneAwa.getMontantCommission()).isEqualByComparingTo("275");
      assertThat(ligneAwa.getMontantNet()).isEqualByComparingTo("1725");
      assertThat(ligneAwa.getTontine()).isSameAs(tontine);
      assertThat(ligneAwa.getCycle()).isSameAs(c2);
      verify(whatsappService, times(2))
          .sendDistributionFinale(any(), eq(tontine.getNom()), any(), any());
      verify(notificationService, times(3))
          .notify(
              any(),
              eq(NotificationType.DISTRIBUTION_FINALE),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
    }

    @Test
    @DisplayName(
        "distribution : commissions en francs entiers, somme exacte (100 F / 3 -> 34+33+33)")
    void distributionSommeExacte() throws Exception {
      List<TontineMembre> membres = membresActifs(3);
      CycleTontine c1 = cycleConnu(1, CycleStatut.EN_COURS);
      when(cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(tontine.getId()))
          .thenReturn(List.of(c1));
      when(cotisationRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), CotisationStatut.VALIDE))
          .thenReturn(
              membres.stream()
                  .map(m -> cotisation(m, c1, CotisationStatut.VALIDE, "1000"))
                  .toList());
      when(commissionRepository.findByTontine_IdAndDeletedAtIsNull(tontine.getId()))
          .thenReturn(List.of(commission(CommissionType.FRAIS_FIXES_PAR_CYCLE, "100")));

      CycleResponse r = service.closeCycle(kc, tontine.getId(), c1.getId());

      assertThat(r.getDistributionParMembre())
          .extracting(CycleResponse.MembreDistributionInfo::getMontantCommission)
          .usingElementComparator(BigDecimal::compareTo)
          .containsExactly(new BigDecimal("34"), new BigDecimal("33"), new BigDecimal("33"));
      assertThat(r.getMontantCommission()).isEqualByComparingTo("100");
      assertThat(r.getMontantNet()).isEqualByComparingTo("2900");
    }

    @Test
    @DisplayName("distribution déjà enregistrée pour la tontine : aucune seconde écriture")
    void distributionDejaEnregistree() throws Exception {
      List<TontineMembre> membres = membresActifs(1);
      CycleTontine c = cycleConnu(1, CycleStatut.EN_COURS);
      when(cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(tontine.getId()))
          .thenReturn(List.of(c));
      when(cotisationRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), CotisationStatut.VALIDE))
          .thenReturn(List.of(cotisation(membres.get(0), c, CotisationStatut.VALIDE, "500")));
      when(distributionRepository.existsByTontine_IdAndDeletedAtIsNull(tontine.getId()))
          .thenReturn(true);

      assertThat(service.closeCycle(kc, tontine.getId(), c.getId()).getDistributionParMembre())
          .hasSize(1);
      verify(distributionRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName(
        "dernier sous-cycle sans aucune cotisation : distribution vide, gestionnaire notifié")
    void finaleVide() throws Exception {
      membresActifs(1);
      CycleTontine c = cycleConnu(1, CycleStatut.EN_COURS);
      when(cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(tontine.getId()))
          .thenReturn(List.of(c));
      doThrow(new RuntimeException("KO"))
          .when(whatsappService)
          .sendDistributionFinaleAdmin(any(), any(), any(), anyInt());

      CycleResponse r = service.closeCycle(kc, tontine.getId(), c.getId());

      assertThat(r.getDistributionParMembre()).isEmpty();
      verify(distributionRepository, never()).saveAll(any());
      verify(notificationService)
          .notify(
              eq(gestionnaire.getId()),
              eq(NotificationType.DISTRIBUTION_FINALE),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
    }
  }

  @Nested
  @DisplayName("ordre HYBRIDE : tirage au sort ou choix, tour par tour")
  class Hybride {

    @BeforeEach
    void hybride() {
      tontine.setOrdreBeneficiaire("HYBRIDE");
    }

    @Test
    @DisplayName("clôture : aucun gagnant désigné automatiquement")
    void cloture() throws Exception {
      membresActifs(3);
      CycleTontine c = cycleConnu(1, CycleStatut.EN_COURS);
      assertThat(service.closeCycle(kc, tontine.getId(), c.getId()).getGagnants()).isEmpty();
      verify(gagnantRepository, never()).save(any());
      verify(schedulerService, never()).annoncerGagnants(any(), any());
    }

    @Test
    @DisplayName("après clôture : tirage au sort (liste vide) puis choix explicite au tour suivant")
    void tirageEtChoix() throws Exception {
      List<TontineMembre> membres = membresActifs(3);
      CycleTontine tour1 = cycleConnu(1, CycleStatut.TERMINE);
      tour1.setMontantNet(new BigDecimal("3000"));
      SelectionnerBeneficiaireRequest tirage = new SelectionnerBeneficiaireRequest();

      CycleResponse r1 =
          service.selectionnerBeneficiaire(kc, tontine.getId(), tour1.getId(), tirage);
      assertThat(r1.getGagnants()).hasSize(1);
      assertThat(r1.getGagnants().get(0).getMontantRecu()).isEqualByComparingTo("3000");

      TontineMembre restant =
          membres.stream().filter(m -> !m.isARecuJackpot()).findFirst().orElseThrow();
      when(membreRepository.findByIdAndTontine_IdAndDeletedAtIsNull(
              restant.getId(), tontine.getId()))
          .thenReturn(Optional.of(restant));
      CycleTontine tour2 = cycleConnu(2, CycleStatut.TERMINE);
      tour2.setMontantNet(new BigDecimal("3000"));
      SelectionnerBeneficiaireRequest choix = new SelectionnerBeneficiaireRequest();
      choix.setMembreIds(List.of(restant.getId()));

      CycleResponse r2 =
          service.selectionnerBeneficiaire(kc, tontine.getId(), tour2.getId(), choix);
      assertThat(r2.getGagnants())
          .extracting(CycleResponse.GagnantInfo::getMembreId)
          .containsExactly(restant.getId());
    }

    @Test
    @DisplayName("ROTATION et ALEATOIRE : désignation après clôture refusée")
    void autresOrdresRefuses() {
      CycleTontine c = cycleConnu(1, CycleStatut.TERMINE);
      for (String ordre : new String[] {"ROTATION", "ALEATOIRE"}) {
        tontine.setOrdreBeneficiaire(ordre);
        assertThatThrownBy(
                () ->
                    service.selectionnerBeneficiaire(
                        kc, tontine.getId(), c.getId(), new SelectionnerBeneficiaireRequest()))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("HYBRIDE");
      }
    }
  }

  @Nested
  @DisplayName("désignation des gagnants (MANUEL)")
  class Designation {

    private CycleTontine cycle;

    @BeforeEach
    void manuel() {
      tontine.setOrdreBeneficiaire("MANUEL");
      tontine.setNombreGagnants(2);
      cycle = cycleConnu(1, CycleStatut.TERMINE);
      cycle.setMontantNet(new BigDecimal("3000"));
    }

    private SelectionnerBeneficiaireRequest choix(UUID... ids) {
      SelectionnerBeneficiaireRequest r = new SelectionnerBeneficiaireRequest();
      r.setMembreIds(ids.length == 0 ? null : List.of(ids));
      return r;
    }

    private TontineMembre connu(TontineMembre m) {
      lenient()
          .when(
              membreRepository.findByIdAndTontine_IdAndDeletedAtIsNull(m.getId(), tontine.getId()))
          .thenReturn(Optional.of(m));
      return m;
    }

    @Test
    @DisplayName(
        "choix explicite : net partagé, membres marqués, audit, annonce, WhatsApp admin non bloquant")
    void explicite() throws Exception {
      List<TontineMembre> membres = membresActifs(3);
      connu(membres.get(0));
      connu(membres.get(2));
      doThrow(new RuntimeException("KO"))
          .when(whatsappService)
          .sendJackpotManuelAdmin(any(), any(), anyInt(), any());

      CycleResponse r =
          service.selectionnerBeneficiaire(
              kc,
              tontine.getId(),
              cycle.getId(),
              choix(membres.get(0).getId(), membres.get(2).getId()));

      assertThat(r.getGagnants())
          .extracting(CycleResponse.GagnantInfo::getMembreId)
          .containsExactly(membres.get(0).getId(), membres.get(2).getId());
      assertThat(r.getGagnants())
          .allSatisfy(g -> assertThat(g.getMontantRecu()).isEqualByComparingTo("1500"));
      assertThat(membres.get(0).isARecuJackpot()).isTrue();
      verify(auditService, times(2))
          .log(
              eq(gestionnaire),
              eq("cycles_tontine"),
              eq(cycle.getId()),
              eq("gagnant_ajoute"),
              any(),
              any());
      verify(schedulerService).annoncerGagnants(eq(cycle), any());
    }

    @Test
    @DisplayName("liste vide : tirage aléatoire de nombreGagnants parmi les éligibles")
    void tirage() throws Exception {
      List<TontineMembre> membres = membresActifs(4);
      membres.get(0).setARecuJackpot(true);

      CycleResponse r =
          service.selectionnerBeneficiaire(kc, tontine.getId(), cycle.getId(), choix());

      assertThat(r.getGagnants())
          .hasSize(2)
          .noneMatch(g -> g.getMembreId().equals(membres.get(0).getId()));
    }

    @Test
    @DisplayName("refus : doublon, membre non actif, déjà servi, inconnu")
    void refusMembres() {
      List<TontineMembre> membres = membresActifs(3);
      UUID id0 = connu(membres.get(0)).getId();
      assertThatThrownBy(
              () ->
                  service.selectionnerBeneficiaire(
                      kc, tontine.getId(), cycle.getId(), choix(id0, id0)))
          .isInstanceOf(BadRequestException.class);

      TontineMembre suspendu = connu(Fixtures.membre(tontine, Fixtures.user("S"), 8));
      suspendu.setStatut(MembreStatut.SUSPENDU);
      assertThatThrownBy(
              () ->
                  service.selectionnerBeneficiaire(
                      kc, tontine.getId(), cycle.getId(), choix(suspendu.getId())))
          .isInstanceOf(BadRequestException.class);

      TontineMembre servi = connu(membres.get(1));
      servi.setARecuJackpot(true);
      assertThatThrownBy(
              () ->
                  service.selectionnerBeneficiaire(
                      kc, tontine.getId(), cycle.getId(), choix(servi.getId())))
          .isInstanceOf(BadRequestException.class);

      UUID inconnu = UUID.randomUUID();
      when(membreRepository.findByIdAndTontine_IdAndDeletedAtIsNull(inconnu, tontine.getId()))
          .thenReturn(Optional.empty());
      assertThatThrownBy(
              () ->
                  service.selectionnerBeneficiaire(
                      kc, tontine.getId(), cycle.getId(), choix(inconnu)))
          .isInstanceOf(NotFoundException.class);
      verify(gagnantRepository, never()).save(any());
    }

    @Test
    @DisplayName(
        "refus : déjà désignés, aucun éligible, ordre non MANUEL, cycle non terminé, événementielle")
    void refusContexte() {
      when(gagnantRepository.existsByCycle_IdAndDeletedAtIsNull(cycle.getId())).thenReturn(true);
      assertThatThrownBy(
              () -> service.selectionnerBeneficiaire(kc, tontine.getId(), cycle.getId(), choix()))
          .isInstanceOf(BadRequestException.class);

      when(gagnantRepository.existsByCycle_IdAndDeletedAtIsNull(cycle.getId())).thenReturn(false);
      membresActifs(1).get(0).setARecuJackpot(true);
      assertThatThrownBy(
              () -> service.selectionnerBeneficiaire(kc, tontine.getId(), cycle.getId(), choix()))
          .isInstanceOf(BadRequestException.class);

      tontine.setOrdreBeneficiaire("ROTATION");
      assertThatThrownBy(
              () -> service.selectionnerBeneficiaire(kc, tontine.getId(), cycle.getId(), choix()))
          .isInstanceOf(BadRequestException.class);
      cycle.setStatut(CycleStatut.EN_COURS);
      assertThatThrownBy(
              () -> service.selectionnerBeneficiaire(kc, tontine.getId(), cycle.getId(), choix()))
          .isInstanceOf(BadRequestException.class);
      tontine.setTontineType(TontineType.EVENEMENTIELLE);
      assertThatThrownBy(
              () -> service.selectionnerBeneficiaire(kc, tontine.getId(), cycle.getId(), choix()))
          .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("gagnants supprimés logiquement absents de la réponse du cycle")
    void gagnantsSupprimesFiltres() throws Exception {
      CycleGagnant actif =
          CycleGagnant.builder()
              .cycle(cycle)
              .membre(Fixtures.membre(tontine, Fixtures.user("A"), 1))
              .montantRecu(BigDecimal.TEN)
              .build();
      CycleGagnant supprime =
          CycleGagnant.builder()
              .cycle(cycle)
              .membre(Fixtures.membre(tontine, Fixtures.user("B"), 2))
              .montantRecu(BigDecimal.TEN)
              .build();
      supprime.setDeletedAt(LocalDateTime.now());
      cycle.setGagnants(new ArrayList<>(List.of(actif, supprime)));
      assertThat(service.getCycle(kc, tontine.getId(), cycle.getId()).getGagnants()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("priorités de jackpot : un membre dans le besoin passe en premier")
  class Priorites {

    @Test
    @DisplayName("ROTATION : le prioritaire gagne avant l'ordre de passage ; priorité servie")
    void rotation() throws Exception {
      List<TontineMembre> membres = membresActifs(3);
      when(prioriteService.prioritairesEligibles(tontine.getId()))
          .thenReturn(List.of(membres.get(2)));
      CycleTontine c = cycleConnu(1, CycleStatut.EN_COURS);

      CycleResponse r = service.closeCycle(kc, tontine.getId(), c.getId());

      assertThat(r.getGagnants())
          .extracting(CycleResponse.GagnantInfo::getMembreId)
          .containsExactly(membres.get(2).getId());
      verify(prioriteService).marquerServies(c, List.of(membres.get(2)));
    }

    @Test
    @DisplayName("ALEATOIRE, 2 gagnants : le prioritaire + un tiré au sort parmi les autres")
    void aleatoire() throws Exception {
      List<TontineMembre> membres = membresActifs(4);
      tontine.setOrdreBeneficiaire("ALEATOIRE");
      tontine.setNombreGagnants(2);
      when(prioriteService.prioritairesEligibles(tontine.getId()))
          .thenReturn(List.of(membres.get(3)));
      CycleTontine c = cycleConnu(1, CycleStatut.EN_COURS);

      CycleResponse r = service.closeCycle(kc, tontine.getId(), c.getId());

      assertThat(r.getGagnants()).hasSize(2);
      assertThat(r.getGagnants().get(0).getMembreId()).isEqualTo(membres.get(3).getId());
      assertThat(r.getGagnants().get(1).getMembreId()).isNotEqualTo(membres.get(3).getId());
    }

    @Test
    @DisplayName("HYBRIDE : clôture -> GAGNANTS_A_DESIGNER au gestionnaire ; tirage -> prioritaire")
    void hybride() throws Exception {
      List<TontineMembre> membres = membresActifs(3);
      tontine.setOrdreBeneficiaire("HYBRIDE");
      CycleTontine c = cycleConnu(1, CycleStatut.EN_COURS);

      assertThat(service.closeCycle(kc, tontine.getId(), c.getId()).getGagnants()).isEmpty();
      verify(notificationService)
          .notify(
              eq(gestionnaire.getId()),
              eq(NotificationType.GAGNANTS_A_DESIGNER),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
      verify(prioriteService, never()).prioritairesEligibles(any());

      c.setMontantNet(new BigDecimal("3000"));
      when(prioriteService.prioritairesEligibles(tontine.getId()))
          .thenReturn(List.of(membres.get(1)));
      CycleResponse r =
          service.selectionnerBeneficiaire(
              kc, tontine.getId(), c.getId(), new SelectionnerBeneficiaireRequest());
      assertThat(r.getGagnants())
          .extracting(CycleResponse.GagnantInfo::getMembreId)
          .containsExactly(membres.get(1).getId());
      verify(prioriteService).marquerServies(c, List.of(membres.get(1)));
    }

    @Test
    @DisplayName("HYBRIDE sans éligible : pas de notification GAGNANTS_A_DESIGNER")
    void hybrideSansEligible() throws Exception {
      membresActifs(2).forEach(m -> m.setARecuJackpot(true));
      tontine.setOrdreBeneficiaire("HYBRIDE");
      CycleTontine c = cycleConnu(1, CycleStatut.EN_COURS);

      service.closeCycle(kc, tontine.getId(), c.getId());

      verify(notificationService, never())
          .notify(
              any(),
              eq(NotificationType.GAGNANTS_A_DESIGNER),
              anyString(),
              anyString(),
              any(),
              any());
    }

    @Test
    @DisplayName("choix explicite du gestionnaire : priorités des membres choisis servies")
    void choixExplicite() throws Exception {
      List<TontineMembre> membres = membresActifs(2);
      tontine.setOrdreBeneficiaire("MANUEL");
      when(membreRepository.findByIdAndTontine_IdAndDeletedAtIsNull(
              membres.get(0).getId(), tontine.getId()))
          .thenReturn(Optional.of(membres.get(0)));
      CycleTontine c = cycleConnu(1, CycleStatut.TERMINE);
      c.setMontantNet(new BigDecimal("2000"));
      SelectionnerBeneficiaireRequest choix = new SelectionnerBeneficiaireRequest();
      choix.setMembreIds(List.of(membres.get(0).getId()));

      service.selectionnerBeneficiaire(kc, tontine.getId(), c.getId(), choix);

      verify(prioriteService).marquerServies(c, List.of(membres.get(0)));
      verify(prioriteService, never()).prioritairesEligibles(any());
    }
  }
}
