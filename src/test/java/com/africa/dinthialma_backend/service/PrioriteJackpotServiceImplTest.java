package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.security.AccessControl;
import com.africa.dinthialma_backend.jackpot.codeList.OriginePriorite;
import com.africa.dinthialma_backend.jackpot.codeList.StatutPriorite;
import com.africa.dinthialma_backend.jackpot.dto.PrioriteJackpotResponse;
import com.africa.dinthialma_backend.jackpot.dto.PrioriteRequests;
import com.africa.dinthialma_backend.jackpot.entity.PrioriteJackpot;
import com.africa.dinthialma_backend.jackpot.mapper.PrioriteJackpotMapper;
import com.africa.dinthialma_backend.jackpot.repository.PrioriteJackpotRepository;
import com.africa.dinthialma_backend.jackpot.service.impl.PrioriteJackpotServiceImpl;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrioriteJackpotServiceImpl")
class PrioriteJackpotServiceImplTest {

  @Mock private PrioriteJackpotRepository prioriteRepository;

  @Spy
  private PrioriteJackpotMapper prioriteMapper = Mappers.getMapper(PrioriteJackpotMapper.class);

  @Mock private TontineRepository tontineRepository;
  @Mock private TontineMembreRepository membreRepository;
  @Mock private AccessControl accessControl;
  @Mock private NotificationService notificationService;
  @Mock private AuditService auditService;
  @InjectMocks private PrioriteJackpotServiceImpl service;

  private User gestionnaire;
  private User awa;
  private User binta;
  private User etranger;
  private Tontine tontine;
  private TontineMembre membreAwa;
  private TontineMembre membreBinta;

  @BeforeEach
  void setUp() throws CustomException {
    gestionnaire = Fixtures.user("Gestionnaire");
    awa = Fixtures.user("Awa");
    binta = Fixtures.user("Binta");
    etranger = Fixtures.user("Étranger");
    tontine = Fixtures.tontine(gestionnaire);
    membreAwa = Fixtures.membre(tontine, awa, 1);
    membreBinta = Fixtures.membre(tontine, binta, 2);
    for (User u : List.of(gestionnaire, awa, binta, etranger)) {
      lenient().when(accessControl.utilisateur(u.getKeycloakId())).thenReturn(u);
    }
    lenient()
        .when(tontineRepository.findByIdAndDeletedAtIsNull(tontine.getId()))
        .thenReturn(Optional.of(tontine));
    lenient()
        .when(
            membreRepository.findByTontine_IdAndUser_IdAndDeletedAtIsNull(
                tontine.getId(), awa.getId()))
        .thenReturn(Optional.of(membreAwa));
    lenient()
        .when(
            membreRepository.findByIdAndTontine_IdAndDeletedAtIsNull(
                membreAwa.getId(), tontine.getId()))
        .thenReturn(Optional.of(membreAwa));
    lenient()
        .when(
            membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
                tontine.getId(), MembreStatut.ACTIF))
        .thenReturn(List.of(membreAwa, membreBinta));
    lenient()
        .when(prioriteRepository.save(any()))
        .thenAnswer(
            i -> {
              PrioriteJackpot p = i.getArgument(0);
              if (p.getId() == null) p.setId(UUID.randomUUID());
              return p;
            });
  }

  private PrioriteJackpot priorite(TontineMembre membre, StatutPriorite statut) {
    PrioriteJackpot p =
        PrioriteJackpot.builder()
            .id(UUID.randomUUID())
            .tontine(tontine)
            .membre(membre)
            .statut(statut)
            .origine(OriginePriorite.DEMANDE_MEMBRE)
            .motif("Frais d'hospitalisation")
            .createdAt(LocalDateTime.now())
            .build();
    lenient()
        .when(
            prioriteRepository.findByIdAndTontine_IdAndDeletedAtIsNull(p.getId(), tontine.getId()))
        .thenReturn(Optional.of(p));
    return p;
  }

  private PrioriteRequests.Demander demande(String motif) {
    PrioriteRequests.Demander r = new PrioriteRequests.Demander();
    r.setMotif(motif);
    return r;
  }

  private PrioriteRequests.Attribuer attribution(UUID membreId) {
    PrioriteRequests.Attribuer r = new PrioriteRequests.Attribuer();
    r.setMembreId(membreId);
    r.setMotif("Décidé en réunion");
    return r;
  }

  private static void codeAttendu(ThrowingCallable appel, ErrorCode code) {
    assertThatThrownBy(appel)
        .isInstanceOfSatisfying(
            CustomException.class, e -> assertThat(e.getCode()).isEqualTo(code));
  }

  private void verifierNotifie(UUID userId, NotificationType type) {
    verify(notificationService)
        .notify(eq(userId), eq(type), anyString(), anyString(), eq(tontine.getId()), any());
  }

  @Nested
  @DisplayName("demande du membre")
  class Demande {

    @Test
    @DisplayName("EN_ATTENTE, origine DEMANDE_MEMBRE, motif nettoyé, gestionnaire notifié")
    void succes() throws Exception {
      PrioriteJackpotResponse r =
          service.demander(awa.getKeycloakId(), tontine.getId(), demande("  Hospitalisation  "));

      assertThat(r.getStatut()).isEqualTo(StatutPriorite.EN_ATTENTE);
      assertThat(r.getOrigine()).isEqualTo(OriginePriorite.DEMANDE_MEMBRE);
      assertThat(r.getMotif()).isEqualTo("Hospitalisation");
      assertThat(r.getMembreId()).isEqualTo(membreAwa.getId());
      verifierNotifie(gestionnaire.getId(), NotificationType.PRIORITE_JACKPOT_DEMANDEE);
    }

    @Test
    @DisplayName("sans corps ou motif vide : motif null ; tontine SUSPENDUE acceptée")
    void sansMotif() throws Exception {
      tontine.setStatut(TontineStatut.SUSPENDUE);
      assertThat(service.demander(awa.getKeycloakId(), tontine.getId(), null).getMotif()).isNull();
      when(prioriteRepository.existsByMembre_IdAndStatutInAndDeletedAtIsNull(
              any(), anyCollection()))
          .thenReturn(false);
      assertThat(service.demander(awa.getKeycloakId(), tontine.getId(), demande("  ")).getMotif())
          .isNull();
    }

    @Test
    @DisplayName("refus : non membre, tontine introuvable, non rotative, non démarrée")
    void refusContexte() {
      assertThatThrownBy(() -> service.demander(etranger.getKeycloakId(), tontine.getId(), null))
          .isInstanceOf(ForbiddenException.class);
      assertThatThrownBy(() -> service.demander(awa.getKeycloakId(), UUID.randomUUID(), null))
          .isInstanceOf(NotFoundException.class);

      tontine.setTontineType(TontineType.EVENEMENTIELLE);
      codeAttendu(
          () -> service.demander(awa.getKeycloakId(), tontine.getId(), null),
          ErrorCode.PRIORITE_NON_DISPONIBLE);

      tontine.setTontineType(TontineType.ROTATIVE);
      tontine.setStatut(TontineStatut.BROUILLON);
      codeAttendu(
          () -> service.demander(awa.getKeycloakId(), tontine.getId(), null),
          ErrorCode.PRIORITE_NON_DISPONIBLE);
      verify(prioriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("refus : jackpot déjà reçu, membre suspendu, priorité déjà active")
    void refusMembre() {
      membreAwa.setARecuJackpot(true);
      codeAttendu(
          () -> service.demander(awa.getKeycloakId(), tontine.getId(), null),
          ErrorCode.MEMBRE_NON_ELIGIBLE_JACKPOT);

      membreAwa.setARecuJackpot(false);
      membreAwa.setStatut(MembreStatut.SUSPENDU);
      codeAttendu(
          () -> service.demander(awa.getKeycloakId(), tontine.getId(), null),
          ErrorCode.MEMBRE_NON_ELIGIBLE_JACKPOT);

      membreAwa.setStatut(MembreStatut.ACTIF);
      when(prioriteRepository.existsByMembre_IdAndStatutInAndDeletedAtIsNull(
              eq(membreAwa.getId()), anyCollection()))
          .thenReturn(true);
      assertThatThrownBy(() -> service.demander(awa.getKeycloakId(), tontine.getId(), null))
          .isInstanceOfSatisfying(
              ConflictException.class,
              e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PRIORITE_DEJA_ACTIVE));
    }
  }

  @Nested
  @DisplayName("décisions du gestionnaire")
  class Decisions {

    @Test
    @DisplayName(
        "attribution : ACCEPTEE d'emblée, auditée, bénéficiaire et autres membres notifiés")
    void attribuer() throws Exception {
      PrioriteJackpotResponse r =
          service.attribuer(
              gestionnaire.getKeycloakId(), tontine.getId(), attribution(membreAwa.getId()));

      assertThat(r.getStatut()).isEqualTo(StatutPriorite.ACCEPTEE);
      assertThat(r.getOrigine()).isEqualTo(OriginePriorite.GESTIONNAIRE);
      assertThat(r.getDateTraitement()).isNotNull();
      verify(auditService)
          .log(
              eq(gestionnaire),
              eq("priorites_jackpot"),
              any(),
              eq("statut"),
              any(),
              eq("ACCEPTEE"));
      verify(notificationService)
          .notify(
              eq(awa.getId()),
              eq(NotificationType.PRIORITE_JACKPOT_ACCORDEE),
              anyString(),
              contains("Vous recevrez"),
              eq(tontine.getId()),
              any());
      verify(notificationService)
          .notify(
              eq(binta.getId()),
              eq(NotificationType.PRIORITE_JACKPOT_ACCORDEE),
              anyString(),
              contains("Awa Test recevra en priorité"),
              eq(tontine.getId()),
              any());
    }

    @Test
    @DisplayName("attribution : SUPER_ADMIN autorisé ; tiers -> 403 ; membre inconnu -> 404")
    void attribuerDroits() throws Exception {
      when(accessControl.estSuperAdmin(etranger)).thenReturn(true);
      assertThat(
              service
                  .attribuer(
                      etranger.getKeycloakId(), tontine.getId(), attribution(membreAwa.getId()))
                  .getStatut())
          .isEqualTo(StatutPriorite.ACCEPTEE);

      assertThatThrownBy(
              () ->
                  service.attribuer(
                      binta.getKeycloakId(), tontine.getId(), attribution(membreAwa.getId())))
          .isInstanceOf(ForbiddenException.class);
      assertThatThrownBy(
              () ->
                  service.attribuer(
                      gestionnaire.getKeycloakId(),
                      tontine.getId(),
                      attribution(UUID.randomUUID())))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("acceptation : EN_ATTENTE -> ACCEPTEE, les deux membres actifs notifiés")
    void accepter() throws Exception {
      PrioriteJackpot p = priorite(membreAwa, StatutPriorite.EN_ATTENTE);

      PrioriteJackpotResponse r =
          service.accepter(gestionnaire.getKeycloakId(), tontine.getId(), p.getId());

      assertThat(r.getStatut()).isEqualTo(StatutPriorite.ACCEPTEE);
      assertThat(p.getTraiteePar()).isEqualTo(gestionnaire);
      verify(notificationService, times(2))
          .notify(
              any(),
              eq(NotificationType.PRIORITE_JACKPOT_ACCORDEE),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
    }

    @Test
    @DisplayName("acceptation refusée : déjà traitée, membre devenu inéligible, inconnue, tiers")
    void accepterRefus() {
      PrioriteJackpot acceptee = priorite(membreAwa, StatutPriorite.ACCEPTEE);
      codeAttendu(
          () -> service.accepter(gestionnaire.getKeycloakId(), tontine.getId(), acceptee.getId()),
          ErrorCode.PRIORITE_NOT_PENDING);

      PrioriteJackpot enAttente = priorite(membreBinta, StatutPriorite.EN_ATTENTE);
      membreBinta.setARecuJackpot(true);
      codeAttendu(
          () -> service.accepter(gestionnaire.getKeycloakId(), tontine.getId(), enAttente.getId()),
          ErrorCode.MEMBRE_NON_ELIGIBLE_JACKPOT);

      assertThatThrownBy(
              () ->
                  service.accepter(
                      gestionnaire.getKeycloakId(), tontine.getId(), UUID.randomUUID()))
          .isInstanceOf(NotFoundException.class);
      assertThatThrownBy(
              () -> service.accepter(awa.getKeycloakId(), tontine.getId(), enAttente.getId()))
          .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("refus : REFUSEE avec motif, membre notifié ; sans motif : message sans « Motif »")
    void refuser() throws Exception {
      PrioriteJackpot p = priorite(membreAwa, StatutPriorite.EN_ATTENTE);
      PrioriteRequests.Refuser motif = new PrioriteRequests.Refuser();
      motif.setMotif(" Tour déjà promis ");

      PrioriteJackpotResponse r =
          service.refuser(gestionnaire.getKeycloakId(), tontine.getId(), p.getId(), motif);

      assertThat(r.getStatut()).isEqualTo(StatutPriorite.REFUSEE);
      assertThat(r.getMotifRefus()).isEqualTo("Tour déjà promis");
      verify(notificationService)
          .notify(
              eq(awa.getId()),
              eq(NotificationType.PRIORITE_JACKPOT_REFUSEE),
              anyString(),
              contains("Motif : Tour déjà promis"),
              eq(tontine.getId()),
              any());

      PrioriteJackpot autre = priorite(membreBinta, StatutPriorite.EN_ATTENTE);
      service.refuser(gestionnaire.getKeycloakId(), tontine.getId(), autre.getId(), null);
      assertThat(autre.getMotifRefus()).isNull();
      verify(notificationService)
          .notify(
              eq(binta.getId()),
              eq(NotificationType.PRIORITE_JACKPOT_REFUSEE),
              anyString(),
              eq("Votre demande de priorité sur le jackpot de Tontine test a été refusée."),
              eq(tontine.getId()),
              any());
    }
  }

  @Nested
  @DisplayName("annulation")
  class Annulation {

    @Test
    @DisplayName("par le membre concerné : ANNULEE, aucune notification")
    void parLeMembre() throws Exception {
      PrioriteJackpot p = priorite(membreAwa, StatutPriorite.ACCEPTEE);
      assertThat(service.annuler(awa.getKeycloakId(), tontine.getId(), p.getId()).getStatut())
          .isEqualTo(StatutPriorite.ANNULEE);
      verify(notificationService, never())
          .notify(any(), any(), anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("par le gestionnaire : ANNULEE, membre notifié")
    void parLeGestionnaire() throws Exception {
      PrioriteJackpot p = priorite(membreAwa, StatutPriorite.EN_ATTENTE);
      service.annuler(gestionnaire.getKeycloakId(), tontine.getId(), p.getId());
      assertThat(p.getStatut()).isEqualTo(StatutPriorite.ANNULEE);
      verifierNotifie(awa.getId(), NotificationType.PRIORITE_JACKPOT_REFUSEE);
    }

    @Test
    @DisplayName("refus : autre membre -> 403 ; priorité déjà servie -> PRIORITE_NOT_PENDING")
    void refus() {
      PrioriteJackpot p = priorite(membreAwa, StatutPriorite.ACCEPTEE);
      assertThatThrownBy(() -> service.annuler(binta.getKeycloakId(), tontine.getId(), p.getId()))
          .isInstanceOf(ForbiddenException.class);

      p.setStatut(StatutPriorite.SERVIE);
      codeAttendu(
          () -> service.annuler(awa.getKeycloakId(), tontine.getId(), p.getId()),
          ErrorCode.PRIORITE_NOT_PENDING);
    }
  }

  @Nested
  @DisplayName("lecture")
  class Lecture {

    private void page(PrioriteJackpot... priorites) {
      when(prioriteRepository.findByTontine(eq(tontine.getId()), any(), any()))
          .thenReturn(new PageImpl<>(List.of(priorites)));
    }

    @Test
    @DisplayName("motif visible du gestionnaire et du membre concerné, masqué aux autres membres")
    void motif() throws Exception {
      PrioriteJackpot p = priorite(membreAwa, StatutPriorite.ACCEPTEE);
      page(p);
      when(membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(any(), any()))
          .thenReturn(true);

      assertThat(
              service
                  .lister(
                      gestionnaire.getKeycloakId(), tontine.getId(), null, PageRequest.of(0, 20))
                  .getContent()
                  .get(0)
                  .getMotif())
          .isEqualTo("Frais d'hospitalisation");
      assertThat(
              service
                  .lister(awa.getKeycloakId(), tontine.getId(), null, PageRequest.of(0, 20))
                  .getContent()
                  .get(0)
                  .getMotif())
          .isEqualTo("Frais d'hospitalisation");
      PrioriteJackpotResponse vueBinta =
          service
              .lister(
                  binta.getKeycloakId(),
                  tontine.getId(),
                  StatutPriorite.ACCEPTEE,
                  PageRequest.of(0, 20))
              .getContent()
              .get(0);
      assertThat(vueBinta.getMotif()).isNull();
      assertThat(vueBinta.getFirstName()).isEqualTo("Awa");
    }

    @Test
    @DisplayName("tiers -> 403 ; tontine introuvable -> 404")
    void refus() {
      assertThatThrownBy(
              () ->
                  service.lister(
                      etranger.getKeycloakId(), tontine.getId(), null, PageRequest.of(0, 20)))
          .isInstanceOf(ForbiddenException.class);
      assertThatThrownBy(
              () ->
                  service.lister(
                      gestionnaire.getKeycloakId(), UUID.randomUUID(), null, PageRequest.of(0, 20)))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("désignation des gagnants")
  class Designation {

    @Test
    @DisplayName(
        "prioritaires éligibles : ordre d'acceptation, membres servis ou suspendus écartés")
    void prioritairesEligibles() {
      TontineMembre servi = Fixtures.membre(tontine, Fixtures.user("Servi"), 3);
      servi.setARecuJackpot(true);
      TontineMembre suspendu = Fixtures.membre(tontine, Fixtures.user("Suspendu"), 4);
      suspendu.setStatut(MembreStatut.SUSPENDU);
      List<PrioriteJackpot> acceptees =
          List.of(
              priorite(membreBinta, StatutPriorite.ACCEPTEE),
              priorite(servi, StatutPriorite.ACCEPTEE),
              priorite(suspendu, StatutPriorite.ACCEPTEE),
              priorite(membreAwa, StatutPriorite.ACCEPTEE));
      when(prioriteRepository
              .findByTontine_IdAndStatutAndDeletedAtIsNullOrderByDateTraitementAscCreatedAtAsc(
                  tontine.getId(), StatutPriorite.ACCEPTEE))
          .thenReturn(acceptees);

      assertThat(service.prioritairesEligibles(tontine.getId()))
          .containsExactly(membreBinta, membreAwa);
    }

    @Test
    @DisplayName(
        "gagnants du tour : priorités actives -> SERVIE avec le tour ; aucun gagnant : rien")
    void marquerServies() {
      CycleTontine cycle = Fixtures.cycle(tontine, 1, CycleStatut.TERMINE);
      service.marquerServies(cycle, List.of());
      verify(prioriteRepository, never())
          .findByMembre_IdInAndStatutInAndDeletedAtIsNull(anyList(), anyCollection());

      PrioriteJackpot acceptee = priorite(membreAwa, StatutPriorite.ACCEPTEE);
      PrioriteJackpot enAttente = priorite(membreBinta, StatutPriorite.EN_ATTENTE);
      when(prioriteRepository.findByMembre_IdInAndStatutInAndDeletedAtIsNull(
              eq(List.of(membreAwa.getId(), membreBinta.getId())), anyCollection()))
          .thenReturn(List.of(acceptee, enAttente));

      service.marquerServies(cycle, List.of(membreAwa, membreBinta));

      assertThat(List.of(acceptee, enAttente))
          .allSatisfy(
              p -> {
                assertThat(p.getStatut()).isEqualTo(StatutPriorite.SERVIE);
                assertThat(p.getCycle()).isEqualTo(cycle);
              });
    }
  }
}
