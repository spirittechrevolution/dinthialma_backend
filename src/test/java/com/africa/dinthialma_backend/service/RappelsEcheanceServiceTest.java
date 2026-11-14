package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.CanalRappel;
import com.africa.dinthialma_backend.notification.codeList.NatureRappel;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.service.JournalRappels;
import com.africa.dinthialma_backend.notification.service.RappelsEcheanceService;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.calendrier.CalendrierTours;
import com.africa.dinthialma_backend.tontine.calendrier.CalendrierTours.PeriodeTour;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.EcheanceCycle;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.EcheanceCycleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RappelsEcheanceService – rappels et cotisations manquantes (B26)")
class RappelsEcheanceServiceTest {

  @Mock private EcheanceCycleRepository echeanceRepository;
  @Mock private TontineMembreRepository membreRepository;
  @Mock private CotisationRepository cotisationRepository;
  @Mock private NotificationService notificationService;
  @Mock private WhatsappService whatsappService;
  @Mock private JournalRappels journal;
  @InjectMocks private RappelsEcheanceService service;

  /** Échéance le dimanche 22/11/2026 à 20h00. */
  private static final LocalDateTime ECHEANCE = LocalDate.of(2026, 11, 22).atTime(20, 0);

  private User gestionnaire;
  private Tontine tontine;
  private CycleTontine cycle;
  private EcheanceCycle echeance;
  private TontineMembre payeur;
  private TontineMembre refuse;
  private TontineMembre absent;
  private final List<TontineMembre> membres = new ArrayList<>();

  @BeforeEach
  void setUp() {
    gestionnaire = Fixtures.user("Gestionnaire");
    tontine = Fixtures.tontine(gestionnaire);
    tontine.setNom("Hebdo");
    tontine.setMontant(new BigDecimal("5000"));
    tontine.setRappelVeille(true);
    tontine.setRappelHeuresAvant(3);
    cycle =
        CalendrierTours.tour(
            tontine,
            4,
            new PeriodeTour(ECHEANCE.toLocalDate().minusDays(6), ECHEANCE.toLocalDate(), ECHEANCE),
            CycleStatut.EN_COURS);
    cycle.setId(UUID.randomUUID());
    echeance = cycle.getEcheances().get(0);
    echeance.setId(UUID.randomUUID());

    payeur = Fixtures.membre(tontine, Fixtures.user("Payeur"), 1);
    refuse = Fixtures.membre(tontine, Fixtures.user("Awa"), 2);
    absent = Fixtures.membre(tontine, Fixtures.user("Moussa"), 3);
    membres.addAll(List.of(payeur, refuse, absent));

    lenient()
        .when(
            echeanceRepository.findActivesEntre(
                any(), any(), eq(CycleStatut.EN_COURS), eq(TontineStatut.ACTIVE)))
        .thenReturn(List.of(echeance));
    lenient()
        .when(
            membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
                tontine.getId(), MembreStatut.ACTIF))
        .thenReturn(membres);
    lenient()
        .when(cotisationRepository.findByCycle_IdAndDeletedAtIsNull(cycle.getId()))
        .thenReturn(
            List.of(
                Fixtures.cotisation(payeur, cycle, CotisationStatut.VALIDE),
                Fixtures.cotisation(refuse, cycle, CotisationStatut.REFUSE)));
    lenient().when(journal.reserver(any(), any(), any(), any())).thenReturn(true);
  }

  private void verifierRappels(int fois, String extrait) {
    verify(notificationService, times(fois))
        .notify(
            any(),
            eq(NotificationType.RAPPEL_COTISATION),
            anyString(),
            argThat(m -> m.contains(extrait)),
            eq(tontine.getId()),
            any());
  }

  @Test
  @DisplayName("veille à 18h00 : in-app + WhatsApp aux seuls membres sans déclaration retenue")
  void veille() {
    service.traiter(ECHEANCE.minusDays(1).withHour(18));

    verifierRappels(2, "5 000 F pour Hebdo est attendue demain avant 20h00");
    verify(notificationService, never())
        .notify(eq(payeur.getUser().getId()), any(), any(), any(), any(), any());
    verify(whatsappService).send(eq(refuse.getUser().getPhone()), anyString());
    verify(whatsappService).send(eq(absent.getUser().getPhone()), anyString());
    verify(journal)
        .reserver(
            echeance.getId(), absent.getUser().getId(), NatureRappel.VEILLE, CanalRappel.IN_APP);
  }

  @Test
  @DisplayName(
      "avant 18h la veille : rien ; trois heures avant : « Plus que 3 h », qui remplace la veille")
  void heuresAvant() {
    service.traiter(ECHEANCE.minusDays(1).withHour(17).withMinute(59));
    verify(journal, never()).reserver(any(), any(), any(), any());

    service.traiter(ECHEANCE.minusHours(3));
    verifierRappels(2, "Plus que 3 h : votre cotisation de 5 000 F");
    verify(notificationService, never())
        .notify(any(), any(), any(), argThat(m -> m.contains("demain")), any(), any());
    verify(journal, never()).reserver(any(), any(), eq(NatureRappel.VEILLE), any());
  }

  @Test
  @DisplayName("heures calmes (21h30) : in-app tout de suite, WhatsApp reporté")
  void heuresCalmes() {
    service.traiter(ECHEANCE.minusDays(1).withHour(21).withMinute(30));
    verifierRappels(2, "demain");
    verify(whatsappService, never()).send(anyString(), anyString());
    verify(journal, never()).reserver(any(), any(), any(), eq(CanalRappel.WHATSAPP));
  }

  @Test
  @DisplayName("rappel déjà réservé (autre passage, autre instance) : rien n'est renvoyé")
  void dejaEnvoye() {
    when(journal.reserver(any(), any(), any(), any())).thenReturn(false);
    service.traiter(ECHEANCE.minusHours(1));
    verify(notificationService, never()).notify(any(), any(), any(), any(), any(), any());
    verify(whatsappService, never()).send(anyString(), anyString());
  }

  @Test
  @DisplayName("échéance passée : COTISATIONS_MANQUANTES au gestionnaire, aucun rappel aux membres")
  void cotisationsManquantes() {
    service.traiter(ECHEANCE.plusMinutes(5));

    verify(notificationService)
        .notify(
            eq(gestionnaire.getId()),
            eq(NotificationType.COTISATIONS_MANQUANTES),
            anyString(),
            argThat(
                m ->
                    m.contains("tour n°4 de Hebdo")
                        && m.contains("2 membres n'ont")
                        && m.contains("Awa T., Moussa T.")),
            eq(tontine.getId()),
            any());
    verify(notificationService, never())
        .notify(any(), eq(NotificationType.RAPPEL_COTISATION), any(), any(), any(), any());
    verify(whatsappService, never()).send(anyString(), anyString());

    // au-delà de 24 h : plus rien
    clearInvocations(notificationService, journal);
    service.traiter(ECHEANCE.plusHours(24));
    verify(journal, never()).reserver(any(), any(), any(), any());
  }

  @Test
  @DisplayName("un seul retardataire, sans nom : « 1 membre n'a », téléphone affiché")
  void unSeulManquant() {
    membres.remove(refuse);
    absent.getUser().setFirstName(null);
    absent.getUser().setLastName(null);
    service.traiter(ECHEANCE.plusHours(1));
    verify(notificationService)
        .notify(
            any(),
            eq(NotificationType.COTISATIONS_MANQUANTES),
            any(),
            argThat(m -> m.contains("1 membre n'a") && m.contains(absent.getUser().getPhone())),
            any(),
            any());
  }

  @Test
  @DisplayName("plus de 5 retardataires : 5 noms puis « … » ; gestionnaire déjà prévenu : rien")
  void beaucoupDeManquants() {
    for (int i = 0; i < 5; i++) {
      membres.add(Fixtures.membre(tontine, Fixtures.user("M" + i), 10 + i));
    }
    service.traiter(ECHEANCE.plusHours(2));
    verify(notificationService)
        .notify(
            any(),
            any(),
            any(),
            argThat(m -> m.contains("7 membres") && m.contains("…")),
            any(),
            any());

    clearInvocations(notificationService);
    when(journal.reserver(any(), any(), eq(NatureRappel.COTISATIONS_MANQUANTES), any()))
        .thenReturn(false);
    service.traiter(ECHEANCE.plusHours(3));
    verify(notificationService, never()).notify(any(), any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("tout le monde a déclaré, rappels désactivés, montant libre : cas limites")
  void casLimites() {
    when(cotisationRepository.findByCycle_IdAndDeletedAtIsNull(cycle.getId()))
        .thenReturn(
            List.of(
                Fixtures.cotisation(payeur, cycle, CotisationStatut.VALIDE),
                Fixtures.cotisation(refuse, cycle, CotisationStatut.EN_ATTENTE),
                Fixtures.cotisation(absent, cycle, CotisationStatut.VALIDE)));
    service.traiter(ECHEANCE.plusHours(1));
    verify(journal, never()).reserver(any(), any(), any(), any());

    // rappels désactivés : rien avant l'échéance
    tontine.setRappelVeille(false);
    tontine.setRappelHeuresAvant(0);
    service.traiter(ECHEANCE.minusHours(1));
    verify(journal, never()).reserver(any(), any(), any(), any());

    // montant libre sans plancher : pas de montant dans le message
    when(cotisationRepository.findByCycle_IdAndDeletedAtIsNull(cycle.getId()))
        .thenReturn(List.of());
    tontine.setRappelHeuresAvant(2);
    tontine.setMontantLibre(true);
    tontine.setMontantMinimum(null);
    service.traiter(ECHEANCE.minusHours(1));
    verifierRappels(3, "Plus que 2 h : votre cotisation pour Hebdo");
  }

  @Test
  @DisplayName(
      "membre sans téléphone : in-app seulement ; gestionnaire absent : pas de signalement")
  void sansTelephoneNiGestionnaire() {
    absent.getUser().setPhone(null);
    service.traiter(ECHEANCE.minusHours(2));
    verify(whatsappService, times(1)).send(anyString(), anyString());

    tontine.setCreePar(null);
    service.traiter(ECHEANCE.plusHours(2));
    verify(notificationService, never())
        .notify(any(), eq(NotificationType.COTISATIONS_MANQUANTES), any(), any(), any(), any());
  }

  @Test
  @DisplayName("une échéance en erreur n'empêche pas les autres ; passage planifié = maintenant")
  void robustesse() {
    EcheanceCycle cassee = EcheanceCycle.builder().echeanceAt(ECHEANCE).build();
    when(echeanceRepository.findActivesEntre(any(), any(), any(), any()))
        .thenReturn(List.of(cassee, echeance));
    service.traiter(ECHEANCE.minusHours(1));
    verifierRappels(2, "Plus que 3 h");

    service.planifie();
    verify(echeanceRepository, times(2)).findActivesEntre(any(), any(), any(), any());
  }

  @Test
  @DisplayName("heures WhatsApp de 8h00 à 21h00 ; montants en francs groupés")
  void utilitaires() {
    LocalDate jour = LocalDate.of(2026, 11, 22);
    assertThat(RappelsEcheanceService.whatsappAutorise(jour.atTime(7, 59))).isFalse();
    assertThat(RappelsEcheanceService.whatsappAutorise(jour.atTime(8, 0))).isTrue();
    assertThat(RappelsEcheanceService.whatsappAutorise(jour.atTime(20, 59))).isTrue();
    assertThat(RappelsEcheanceService.whatsappAutorise(jour.atTime(21, 0))).isFalse();
    assertThat(RappelsEcheanceService.francs(new BigDecimal("1000000.00")))
        .isEqualTo("1 000 000 F");
    assertThat(RappelsEcheanceService.francs(new BigDecimal("500"))).isEqualTo("500 F");
  }
}
