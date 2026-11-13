package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
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
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.calendrier.CalendrierTours;
import com.africa.dinthialma_backend.tontine.calendrier.CalendrierTours.PeriodeTour;
import com.africa.dinthialma_backend.tontine.calendrier.Rythme;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.codeList.UniteRythme;
import com.africa.dinthialma_backend.tontine.dto.ModifierEcheanceRequest;
import com.africa.dinthialma_backend.tontine.dto.RappelsEcheance;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import com.africa.dinthialma_backend.tontine.service.impl.EcheanceServiceImpl;
import com.africa.dinthialma_backend.tontine.service.interfaces.TontineService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EcheanceServiceImpl – échéance modifiée en cours (B26)")
class EcheanceServiceImplTest {

  @Mock private TontineRepository tontineRepository;
  @Mock private CycleTontineRepository cycleRepository;
  @Mock private TontineMembreRepository membreRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserRoleAssignmentRepository roleRepository;
  @Mock private AuditService auditService;
  @Mock private NotificationService notificationService;
  @Mock private TontineService tontineService;
  @InjectMocks private EcheanceServiceImpl service;

  private User gestionnaire;
  private Tontine tontine;
  private String kc;
  private final List<CycleTontine> tours = new ArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    gestionnaire = Fixtures.user("Gestionnaire");
    kc = gestionnaire.getKeycloakId();
    tontine = Fixtures.tontine(gestionnaire);
    tontine.appliquerRythme(
        new Rythme(1, UniteRythme.SEMAINE, "DIMANCHE", Rythme.HEURE_PAR_DEFAUT));
    lenient().when(userRepository.findByKeycloakId(kc)).thenReturn(Optional.of(gestionnaire));
    lenient()
        .when(tontineRepository.findByIdAndDeletedAtIsNull(tontine.getId()))
        .thenReturn(Optional.of(tontine));
    lenient()
        .when(
            cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(
                tontine.getId()))
        .thenReturn(tours);
    lenient()
        .when(
            membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
                tontine.getId(), MembreStatut.ACTIF))
        .thenReturn(
            List.of(
                Fixtures.membre(tontine, Fixtures.user("A"), 1),
                Fixtures.membre(tontine, Fixtures.user("B"), 2)));
    lenient()
        .when(tontineService.getTontine(kc, tontine.getId()))
        .thenReturn(TontineResponse.builder().id(tontine.getId()).build());
  }

  private void tour(int numero, CycleStatut statut, String debut, String fin) {
    LocalDate f = LocalDate.parse(fin);
    tours.add(
        CalendrierTours.tour(
            tontine, numero, new PeriodeTour(LocalDate.parse(debut), f, f.atTime(23, 59)), statut));
  }

  private static ModifierEcheanceRequest requete(String jour, String heure) {
    ModifierEcheanceRequest r = new ModifierEcheanceRequest();
    r.setJourEcheance(jour);
    r.setHeureEcheance(heure);
    return r;
  }

  private void verifierNotifications(int fois, String extrait) {
    verify(notificationService, times(fois))
        .notify(
            any(),
            eq(NotificationType.REGLES_MODIFIEES),
            anyString(),
            argThat(m -> m.contains(extrait)),
            eq(tontine.getId()));
  }

  @Test
  @DisplayName("rotative : tour en cours inchangé, tours à venir replacés sur le nouveau jour")
  void rotative() throws Exception {
    tour(1, CycleStatut.TERMINE, "2026-11-09", "2026-11-15");
    tour(2, CycleStatut.EN_COURS, "2026-11-16", "2026-11-22");
    tour(3, CycleStatut.EN_ATTENTE, "2026-11-23", "2026-11-29");
    tour(4, CycleStatut.EN_ATTENTE, "2026-11-30", "2026-12-06");

    service.modifierEcheance(kc, tontine.getId(), requete("samedi", "20:00"));

    assertThat(tours.get(1).getDateFin()).isEqualTo(LocalDate.of(2026, 11, 22));
    assertThat(tours.get(1).getEcheanceAt()).isEqualTo(LocalDate.of(2026, 11, 22).atTime(23, 59));
    // l'ancien tour se terminait un dimanche : le suivant s'arrête au premier samedi
    assertThat(tours.get(2).getDateDebut()).isEqualTo(LocalDate.of(2026, 11, 23));
    assertThat(tours.get(2).getEcheanceAt()).isEqualTo(LocalDate.of(2026, 11, 28).atTime(20, 0));
    assertThat(tours.get(2).getEcheances().get(0).getEcheanceAt())
        .isEqualTo(tours.get(2).getEcheanceAt());
    assertThat(tours.get(3).getDateDebut()).isEqualTo(LocalDate.of(2026, 11, 29));
    assertThat(tours.get(3).getDateFin()).isEqualTo(LocalDate.of(2026, 12, 5));
    assertThat(tontine.getJourEcheance()).isEqualTo("SAMEDI");
    assertThat(tontine.getHeureEcheance()).isEqualTo(LocalTime.of(20, 0));
    verify(auditService)
        .log(
            gestionnaire,
            "tontines",
            tontine.getId(),
            "echeance",
            "chaque semaine le dimanche, avant 23h59",
            "chaque semaine le samedi, avant 20h00");
    verifierNotifications(2, "à partir du tour n°3");
    verify(cycleRepository, times(2)).save(any());
  }

  @Test
  @DisplayName("événementielle : un tour à venir de moins quand les périodes s'allongent")
  void evenementielleRetrait() throws Exception {
    tontine.setTontineType(TontineType.EVENEMENTIELLE);
    tontine.setDateEcheance(LocalDate.of(2026, 11, 29));
    tontine.appliquerRythme(new Rythme(1, UniteRythme.SEMAINE, null, Rythme.HEURE_PAR_DEFAUT));
    tour(1, CycleStatut.EN_COURS, "2026-11-01", "2026-11-07");
    tour(2, CycleStatut.EN_ATTENTE, "2026-11-08", "2026-11-14");
    tour(3, CycleStatut.EN_ATTENTE, "2026-11-15", "2026-11-21");
    tour(4, CycleStatut.EN_ATTENTE, "2026-11-22", "2026-11-28");
    tour(5, CycleStatut.EN_ATTENTE, "2026-11-29", "2026-11-29");

    service.modifierEcheance(kc, tontine.getId(), requete("DIMANCHE", null));

    assertThat(tours)
        .extracting(CycleTontine::getDateFin)
        .containsExactly(
            LocalDate.of(2026, 11, 7),
            LocalDate.of(2026, 11, 15),
            LocalDate.of(2026, 11, 22),
            LocalDate.of(2026, 11, 29),
            LocalDate.of(2026, 11, 29));
    assertThat(tours.get(4).getDeletedAt()).isNotNull();
    assertThat(tours.get(4).getEcheances().get(0).getDeletedAt()).isNotNull();
    assertThat(tours.get(3).getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("événementielle : un tour ajouté (numéro suivant) quand les périodes raccourcissent")
  void evenementielleAjout() throws Exception {
    tontine.setTontineType(TontineType.EVENEMENTIELLE);
    tontine.setDateEcheance(LocalDate.of(2026, 11, 29));
    tour(1, CycleStatut.EN_COURS, "2026-11-01", "2026-11-08");
    tour(2, CycleStatut.EN_ATTENTE, "2026-11-09", "2026-11-15");
    tour(3, CycleStatut.EN_ATTENTE, "2026-11-16", "2026-11-22");
    tour(4, CycleStatut.EN_ATTENTE, "2026-11-23", "2026-11-29");
    ArgumentCaptor<CycleTontine> enregistres = ArgumentCaptor.forClass(CycleTontine.class);

    service.modifierEcheance(kc, tontine.getId(), requete("MERCREDI", null));

    verify(cycleRepository, times(4)).save(enregistres.capture());
    CycleTontine nouveau = enregistres.getAllValues().get(3);
    assertThat(nouveau.getNumeroCycle()).isEqualTo(5);
    assertThat(nouveau.getStatut()).isEqualTo(CycleStatut.EN_ATTENTE);
    assertThat(nouveau.getDateDebut()).isEqualTo(LocalDate.of(2026, 11, 26));
    assertThat(nouveau.getDateFin()).isEqualTo(LocalDate.of(2026, 11, 29));
    assertThat(tours.get(1).getDateFin()).isEqualTo(LocalDate.of(2026, 11, 11));
  }

  @Test
  @DisplayName(
      "heure seule : mêmes dates, nouvelle heure ; rappels seuls : ni replanification ni notification")
  void heureEtRappels() throws Exception {
    tour(1, CycleStatut.EN_COURS, "2026-11-16", "2026-11-22");
    tour(2, CycleStatut.EN_ATTENTE, "2026-11-23", "2026-11-29");

    service.modifierEcheance(kc, tontine.getId(), requete(null, "18:30"));
    assertThat(tours.get(1).getDateFin()).isEqualTo(LocalDate.of(2026, 11, 29));
    assertThat(tours.get(1).getEcheanceAt()).isEqualTo(LocalDate.of(2026, 11, 29).atTime(18, 30));

    clearInvocations(notificationService, cycleRepository, auditService);
    tontine.setRappelVeille(true);
    tontine.setRappelHeuresAvant(3);
    ModifierEcheanceRequest rappels = new ModifierEcheanceRequest();
    rappels.setRappels(RappelsEcheance.builder().veille(false).heuresAvant(6).build());
    service.modifierEcheance(kc, tontine.getId(), rappels);

    assertThat(tontine.isRappelVeille()).isFalse();
    assertThat(tontine.getRappelHeuresAvant()).isEqualTo(6);
    verify(auditService, times(2)).log(any(), any(), any(), anyString(), anyString(), anyString());
    verify(cycleRepository, never()).save(any());
    verify(notificationService, never()).notify(any(), any(), any(), any(), any());

    // valeurs identiques : rien n'est audité
    clearInvocations(auditService);
    service.modifierEcheance(kc, tontine.getId(), rappels);
    service.modifierEcheance(kc, tontine.getId(), new ModifierEcheanceRequest());
    verify(auditService, never()).log(any(), any(), any(), anyString(), any(), any());
  }

  @Test
  @DisplayName("sans tour à venir (manuel) : notification « prochain tour » ; brouillon : aucune")
  void sansTourAVenir() throws Exception {
    tour(1, CycleStatut.EN_COURS, "2026-11-16", "2026-11-22");
    service.modifierEcheance(kc, tontine.getId(), requete("LUNDI", null));
    verifierNotifications(2, "à partir du prochain tour");

    clearInvocations(notificationService);
    tours.clear();
    tontine.setStatut(TontineStatut.BROUILLON);
    service.modifierEcheance(kc, tontine.getId(), requete("MARDI", null));
    assertThat(tontine.getJourEcheance()).isEqualTo("MARDI");
    verify(notificationService, never()).notify(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("refus : tontine terminée, jour invalide, tiers ; SUPER_ADMIN autorisé ; 404")
  void refus() throws Exception {
    assertThatThrownBy(() -> service.modifierEcheance(kc, tontine.getId(), requete("15", null)))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.JOUR_ECHEANCE_INVALIDE));

    User tiers = Fixtures.user("Tiers");
    when(userRepository.findByKeycloakId(tiers.getKeycloakId())).thenReturn(Optional.of(tiers));
    assertThatThrownBy(
            () ->
                service.modifierEcheance(
                    tiers.getKeycloakId(), tontine.getId(), requete("LUNDI", null)))
        .isInstanceOf(ForbiddenException.class);
    when(roleRepository.existsByUserIdAndRole(tiers.getId(), UserRole.SUPER_ADMIN))
        .thenReturn(true);
    service.modifierEcheance(tiers.getKeycloakId(), tontine.getId(), requete("LUNDI", null));
    assertThat(tontine.getJourEcheance()).isEqualTo("LUNDI");

    tontine.setStatut(TontineStatut.TERMINEE);
    assertThatThrownBy(() -> service.modifierEcheance(kc, tontine.getId(), requete("MARDI", null)))
        .isInstanceOfSatisfying(
            BadRequestException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.ECHEANCE_NON_MODIFIABLE));

    UUID inconnue = UUID.randomUUID();
    when(tontineRepository.findByIdAndDeletedAtIsNull(inconnue)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.modifierEcheance(kc, inconnue, requete(null, null)))
        .isInstanceOf(NotFoundException.class);
    when(userRepository.findByKeycloakId("inconnu")).thenReturn(Optional.empty());
    assertThatThrownBy(
            () -> service.modifierEcheance("inconnu", tontine.getId(), requete(null, null)))
        .isInstanceOf(NotFoundException.class);
  }
}
