package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.service.SchedulerService;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.CycleGagnant;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleGagnantRepository;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulerService")
class SchedulerServiceTest {

  @Mock private WhatsappService whatsappService;
  @Mock private CycleTontineRepository cycleRepository;
  @Mock private CotisationRepository cotisationRepository;
  @Mock private CycleGagnantRepository gagnantRepository;
  @Mock private TontineMembreRepository membreRepository;
  @Mock private NotificationService notificationService;
  @InjectMocks private SchedulerService scheduler;

  private User gestionnaire;
  private Tontine tontine;

  @BeforeEach
  void setUp() {
    gestionnaire = Fixtures.user("Gestionnaire");
    tontine = Fixtures.tontine(gestionnaire);
  }

  @Nested
  @DisplayName("rappel quotidien des cotisations")
  class Rappels {

    @Test
    @DisplayName(
        "ROTATIVE : chaque cotisation EN_ATTENTE du tour en cours -> WhatsApp + RAPPEL_COTISATION")
    void rotative() {
      CycleTontine cycle = Fixtures.cycle(tontine, 1, CycleStatut.EN_COURS);
      TontineMembre m = Fixtures.membre(tontine, Fixtures.user("Awa"), 1);
      Cotisation c = Fixtures.cotisation(m, cycle, CotisationStatut.EN_ATTENTE);
      when(cycleRepository.findByStatutAndDeletedAtIsNull(CycleStatut.EN_COURS))
          .thenReturn(List.of(cycle));
      when(cotisationRepository.findByCycle_IdAndStatutAndDeletedAtIsNull(
              cycle.getId(), CotisationStatut.EN_ATTENTE))
          .thenReturn(List.of(c));

      scheduler.rappelCotisationsEnAttente();

      verify(whatsappService)
          .sendContributionReminder(m.getUser().getPhone(), tontine.getNom(), "1000 FCFA");
      verify(notificationService)
          .notify(
              eq(m.getUser().getId()),
              eq(NotificationType.RAPPEL_COTISATION),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
    }

    private CycleTontine evenementielle(Long joursAvantEcheance) {
      tontine.setTontineType(TontineType.EVENEMENTIELLE);
      tontine.setNomEvenement("Tabaski");
      tontine.setDateEcheance(
          joursAvantEcheance == null ? null : LocalDate.now().plusDays(joursAvantEcheance));
      CycleTontine cycle = Fixtures.cycle(tontine, 1, CycleStatut.EN_COURS);
      when(cycleRepository.findByStatutAndDeletedAtIsNull(CycleStatut.EN_COURS))
          .thenReturn(List.of(cycle));
      return cycle;
    }

    @Test
    @DisplayName(
        "EVENEMENTIELLE jour clé (J-7) : rappel des EN_ATTENTE + message urgent à tous les actifs")
    void jourCle() {
      CycleTontine cycle = evenementielle(7L);
      TontineMembre a = Fixtures.membre(tontine, Fixtures.user("A"), 1);
      TontineMembre b = Fixtures.membre(tontine, Fixtures.user("B"), 2);
      when(cotisationRepository.findByCycle_IdAndStatutAndDeletedAtIsNull(
              cycle.getId(), CotisationStatut.EN_ATTENTE))
          .thenReturn(List.of(Fixtures.cotisation(a, cycle, CotisationStatut.EN_ATTENTE)));
      when(membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), MembreStatut.ACTIF))
          .thenReturn(List.of(a, b));
      when(cotisationRepository.sumMontantValideByTontineAndMembre(eq(tontine.getId()), any()))
          .thenReturn(new BigDecimal("3000"));

      scheduler.rappelCotisationsEnAttente();

      verify(whatsappService)
          .sendEvenementielleRappelAttente(
              a.getUser().getPhone(), tontine.getNom(), "Tabaski", 7L, "1000 FCFA");
      verify(whatsappService)
          .sendEvenementielleUrgent(
              a.getUser().getPhone(), tontine.getNom(), "Tabaski", 7L, "3000 FCFA");
      verify(whatsappService)
          .sendEvenementielleUrgent(
              b.getUser().getPhone(), tontine.getNom(), "Tabaski", 7L, "3000 FCFA");
      verify(whatsappService, never())
          .sendEvenementielleRappelPeriode(any(), any(), any(), anyLong(), any());
      verify(notificationService, times(3))
          .notify(
              any(),
              eq(NotificationType.RAPPEL_COTISATION),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
    }

    @Test
    @DisplayName(
        "EVENEMENTIELLE jour ordinaire : rappel de période seulement aux membres sans cotisation active")
    void jourOrdinaire() {
      CycleTontine cycle = evenementielle(10L);
      tontine.setNomEvenement(null);
      TontineMembre aJour = Fixtures.membre(tontine, Fixtures.user("A"), 1);
      TontineMembre enRetard = Fixtures.membre(tontine, Fixtures.user("B"), 2);
      when(membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), MembreStatut.ACTIF))
          .thenReturn(List.of(aJour, enRetard));
      when(cotisationRepository.existsActiveByCycleAndMembre(cycle.getId(), aJour.getId()))
          .thenReturn(true);
      when(cotisationRepository.existsActiveByCycleAndMembre(cycle.getId(), enRetard.getId()))
          .thenReturn(false);
      when(cotisationRepository.sumMontantValideByTontineAndMembre(
              tontine.getId(), enRetard.getId()))
          .thenReturn(BigDecimal.ZERO);

      scheduler.rappelCotisationsEnAttente();

      verify(whatsappService)
          .sendEvenementielleRappelPeriode(
              enRetard.getUser().getPhone(), tontine.getNom(), tontine.getNom(), 10L, "0 FCFA");
      verify(whatsappService, never())
          .sendEvenementielleRappelPeriode(
              eq(aJour.getUser().getPhone()), any(), any(), anyLong(), any());
      verify(whatsappService, never())
          .sendEvenementielleUrgent(any(), any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("EVENEMENTIELLE sans échéance ou échéance passée : aucun rappel")
    void sansEcheance() {
      evenementielle(null);
      scheduler.rappelCotisationsEnAttente();
      evenementielle(-1L);
      scheduler.rappelCotisationsEnAttente();
      verifyNoInteractions(whatsappService, notificationService);
    }
  }

  @Test
  @DisplayName(
      "tours proches : J-2 -> gestionnaire (CYCLE_BIENTOT_CLOTURE), J-3 ROTATION -> futur bénéficiaire (TOUR_PROCHE)")
  void toursProches() {
    CycleTontine j2 = Fixtures.cycle(tontine, 1, CycleStatut.EN_COURS);
    CycleTontine j3 = Fixtures.cycle(tontine, 2, CycleStatut.EN_COURS);
    Tontine aleatoire = Fixtures.tontine(gestionnaire);
    aleatoire.setOrdreBeneficiaire("ALEATOIRE");
    CycleTontine j3Aleatoire = Fixtures.cycle(aleatoire, 1, CycleStatut.EN_COURS);
    TontineMembre futur = Fixtures.membre(tontine, Fixtures.user("Futur"), 2);
    when(cycleRepository.findByStatutAndDateFinAndDeletedAtIsNull(
            CycleStatut.EN_COURS, LocalDate.now().plusDays(2)))
        .thenReturn(List.of(j2));
    when(cycleRepository.findByStatutAndDateFinAndDeletedAtIsNull(
            CycleStatut.EN_COURS, LocalDate.now().plusDays(3)))
        .thenReturn(List.of(j3, j3Aleatoire));
    when(membreRepository.findByTontine_IdAndOrdreJackpotAndStatutAndDeletedAtIsNull(
            tontine.getId(), 2, MembreStatut.ACTIF))
        .thenReturn(Optional.of(futur));

    scheduler.rappelsCyclesProches();

    verify(notificationService)
        .notify(
            eq(gestionnaire.getId()),
            eq(NotificationType.CYCLE_BIENTOT_CLOTURE),
            anyString(),
            anyString(),
            eq(tontine.getId()),
            any());
    verify(notificationService)
        .notify(
            eq(futur.getUser().getId()),
            eq(NotificationType.TOUR_PROCHE),
            anyString(),
            anyString(),
            eq(tontine.getId()),
            any());
    verify(membreRepository, never())
        .findByTontine_IdAndOrdreJackpotAndStatutAndDeletedAtIsNull(
            eq(aleatoire.getId()), anyInt(), any());
  }

  @Nested
  @DisplayName("annonce des gagnants")
  class Annonce {

    @Test
    @DisplayName(
        "montant reçu annoncé ; à défaut, estimation montant × membres / gagnants ; échec WhatsApp ignoré")
    void annonce() {
      CycleTontine cycle = Fixtures.cycle(tontine, 1, CycleStatut.TERMINE);
      TontineMembre a = Fixtures.membre(tontine, Fixtures.user("A"), 1);
      TontineMembre b = Fixtures.membre(tontine, Fixtures.user("B"), 2);
      TontineMembre c = Fixtures.membre(tontine, Fixtures.user("C"), 3);
      doThrow(new RuntimeException("KO"))
          .when(whatsappService)
          .sendJackpotNotification(eq(c.getUser().getPhone()), any(), any());

      assertThatCode(
              () ->
                  scheduler.annoncerGagnants(
                      cycle,
                      List.of(
                          CycleGagnant.builder()
                              .cycle(cycle)
                              .membre(a)
                              .montantRecu(new BigDecimal("4500"))
                              .build(),
                          CycleGagnant.builder().cycle(cycle).membre(b).build(),
                          CycleGagnant.builder().cycle(cycle).membre(c).build())))
          .doesNotThrowAnyException();

      verify(whatsappService)
          .sendJackpotNotification(a.getUser().getPhone(), tontine.getNom(), "4500 FCFA");
      verify(whatsappService)
          .sendJackpotNotification(b.getUser().getPhone(), tontine.getNom(), "5000 FCFA");
    }

    @Test
    @DisplayName("liste vide : rien n'est envoyé")
    void vide() {
      scheduler.annoncerGagnants(Fixtures.cycle(tontine, 1, CycleStatut.TERMINE), List.of());
      verifyNoInteractions(whatsappService);
    }
  }
}
