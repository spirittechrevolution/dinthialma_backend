package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.adhesion.codeList.StatutDemande;
import com.africa.dinthialma_backend.adhesion.dto.DemandeAdhesionResponse;
import com.africa.dinthialma_backend.adhesion.dto.DemandeRequests;
import com.africa.dinthialma_backend.adhesion.dto.InvitationApercuResponse;
import com.africa.dinthialma_backend.adhesion.dto.InvitationResponse;
import com.africa.dinthialma_backend.adhesion.entity.DemandeAdhesion;
import com.africa.dinthialma_backend.adhesion.mapper.DemandeAdhesionMapper;
import com.africa.dinthialma_backend.adhesion.repository.DemandeAdhesionRepository;
import com.africa.dinthialma_backend.adhesion.service.impl.AdhesionServiceImpl;
import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.dto.AddMembreRequest;
import com.africa.dinthialma_backend.member.dto.MembreResponse;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.member.service.interfaces.MembreService;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.math.BigDecimal;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdhesionServiceImpl")
class AdhesionServiceImplTest {

  private static final String CODE = "ABCD-EFGH";

  @Mock private TontineRepository tontineRepository;
  @Spy private DemandeAdhesionMapper mapper = Mappers.getMapper(DemandeAdhesionMapper.class);
  @Mock private TontineMembreRepository membreRepository;
  @Mock private DemandeAdhesionRepository demandeRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserRoleAssignmentRepository roleRepository;
  @Mock private MembreService membreService;
  @Mock private NotificationService notificationService;
  @InjectMocks private AdhesionServiceImpl service;

  private User gestionnaire;
  private User demandeur;
  private Tontine tontine;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "appUrl", "https://app.test");
    gestionnaire = Fixtures.user("Gestionnaire");
    demandeur = Fixtures.user("Demandeur");
    tontine = Fixtures.tontine(gestionnaire);
    tontine.setCodeInvitation(CODE);
    lenient()
        .when(userRepository.findByKeycloakId(gestionnaire.getKeycloakId()))
        .thenReturn(Optional.of(gestionnaire));
    lenient()
        .when(userRepository.findByKeycloakId(demandeur.getKeycloakId()))
        .thenReturn(Optional.of(demandeur));
    lenient()
        .when(tontineRepository.findByIdAndDeletedAtIsNull(tontine.getId()))
        .thenReturn(Optional.of(tontine));
    lenient()
        .when(tontineRepository.findByCodeInvitationAndDeletedAtIsNull(CODE))
        .thenReturn(Optional.of(tontine));
    lenient()
        .when(roleRepository.existsByUserIdAndRole(any(), eq(UserRole.SUPER_ADMIN)))
        .thenReturn(false);
    lenient()
        .when(demandeRepository.save(any()))
        .thenAnswer(
            i -> {
              DemandeAdhesion d = i.getArgument(0);
              if (d.getId() == null) d.setId(UUID.randomUUID());
              return d;
            });
  }

  private static void code(Throwable e, ErrorCode attendu) {
    assertThat(((CustomException) e).getCode()).isEqualTo(attendu);
  }

  @Nested
  @DisplayName("invitation")
  class Invitation {

    @Test
    @DisplayName("première demande : code XXXX-XXXX généré (alphabet sans ambiguïté), URL et QR t_")
    void generation() throws Exception {
      tontine.setCodeInvitation(null);
      when(tontineRepository.existsByCodeInvitation(anyString())).thenReturn(true, false);

      InvitationResponse r =
          service.obtenirInvitation(gestionnaire.getKeycloakId(), tontine.getId(), false);

      assertThat(r.getCode()).matches("[2-9A-HJKMNP-Z]{4}-[2-9A-HJKMNP-Z]{4}");
      assertThat(r.getUrl()).isEqualTo("https://app.test/q/t/" + r.getCode());
      assertThat(r.getQrToken()).isEqualTo("t_" + r.getCode());
      verify(tontineRepository).save(tontine);
    }

    @Test
    @DisplayName("code existant conservé ; ?regenerer=true en produit un nouveau")
    void stabiliteEtRegeneration() throws Exception {
      assertThat(
              service
                  .obtenirInvitation(gestionnaire.getKeycloakId(), tontine.getId(), false)
                  .getCode())
          .isEqualTo(CODE);
      verify(tontineRepository, never()).save(any());
      assertThat(
              service
                  .obtenirInvitation(gestionnaire.getKeycloakId(), tontine.getId(), true)
                  .getCode())
          .isNotEqualTo(CODE);
    }

    @Test
    @DisplayName("tontine clôturée -> 400 TONTINE_NOT_OPEN ; tiers -> 403")
    void refus() {
      assertThatThrownBy(
              () -> service.obtenirInvitation(demandeur.getKeycloakId(), tontine.getId(), false))
          .isInstanceOf(ForbiddenException.class);
      tontine.setStatut(TontineStatut.SUSPENDUE);
      assertThatThrownBy(
              () -> service.obtenirInvitation(gestionnaire.getKeycloakId(), tontine.getId(), false))
          .satisfies(e -> code(e, ErrorCode.TONTINE_NOT_OPEN));
    }

    @Test
    @DisplayName("révocation : code supprimé (SUPER_ADMIN autorisé)")
    void revocation() throws Exception {
      when(roleRepository.existsByUserIdAndRole(demandeur.getId(), UserRole.SUPER_ADMIN))
          .thenReturn(true);
      service.revoquerInvitation(demandeur.getKeycloakId(), tontine.getId());
      assertThat(tontine.getCodeInvitation()).isNull();
      verify(tontineRepository).save(tontine);
    }
  }

  @Nested
  @DisplayName("aperçu public")
  class Apercu {

    @Test
    @DisplayName("code saisi en minuscules sans tiret normalisé ; places restantes calculées")
    void apercu() throws Exception {
      when(membreRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), MembreStatut.ACTIF))
          .thenReturn(3L);

      InvitationApercuResponse r = service.apercu(" abcd efgh ");

      assertThat(r.getCode()).isEqualTo(CODE);
      assertThat(r.getPlacesRestantes()).isEqualTo(2);
      assertThat(r.isOuverte()).isTrue();
      assertThat(r.getMontant()).isEqualByComparingTo("1000");
      assertThat(r.getGestionnairePrenom()).isEqualTo("Gestionnaire");
    }

    @Test
    @DisplayName("complète -> fermée ; événementielle à montant libre : sans plafond ni montant")
    void variantes() throws Exception {
      when(membreRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), MembreStatut.ACTIF))
          .thenReturn(7L);
      InvitationApercuResponse complete = service.apercu(CODE);
      assertThat(complete.getPlacesRestantes()).isZero();
      assertThat(complete.isOuverte()).isFalse();

      tontine.setTontineType(TontineType.EVENEMENTIELLE);
      tontine.setMontantLibre(true);
      tontine.setMontantMinimum(new BigDecimal("500"));
      InvitationApercuResponse evt = service.apercu(CODE);
      assertThat(evt.getPlacesRestantes()).isNull();
      assertThat(evt.getMontant()).isNull();
      assertThat(evt.isOuverte()).isTrue();
    }

    @Test
    @DisplayName("code inconnu, révoqué ou nul -> 404")
    void inconnu() {
      when(tontineRepository.findByCodeInvitationAndDeletedAtIsNull(anyString()))
          .thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.apercu("ZZZZ-ZZZZ")).isInstanceOf(NotFoundException.class);
      assertThatThrownBy(() -> service.apercu(null)).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  @DisplayName(
      "normalisation du code : minuscules, espaces, tiret absent ; code court ou nul introuvable")
  void normalisation() throws Exception {
    when(membreRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(any(), any()))
        .thenReturn(0L);
    assertThat(service.apercu("abcdefgh").getCode()).isEqualTo(CODE);
    assertThat(service.apercu("ab cd-efgh").getCode()).isEqualTo(CODE);
    when(tontineRepository.findByCodeInvitationAndDeletedAtIsNull("ABC"))
        .thenReturn(Optional.empty());
    when(tontineRepository.findByCodeInvitationAndDeletedAtIsNull("")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.apercu("abc")).isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> service.apercu(null)).isInstanceOf(NotFoundException.class);
  }

  @Nested
  @DisplayName("création de demande")
  class Creation {

    private DemandeRequests.Creer message(String m) {
      DemandeRequests.Creer r = new DemandeRequests.Creer();
      r.setMessage(m);
      return r;
    }

    @Test
    @DisplayName("demande EN_ATTENTE, message rogné, gestionnaire notifié, vue sans téléphone")
    void succes() throws Exception {
      DemandeAdhesionResponse r =
          service.creerDemande(demandeur.getKeycloakId(), CODE, message("  Bonjour  "));

      assertThat(r.getStatut()).isEqualTo(StatutDemande.EN_ATTENTE);
      assertThat(r.getMessage()).isEqualTo("Bonjour");
      assertThat(r.getPhone()).isNull();
      verify(notificationService)
          .notify(
              eq(gestionnaire.getId()),
              eq(NotificationType.DEMANDE_ADHESION),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
    }

    @Test
    @DisplayName("sans corps : message nul")
    void sansCorps() throws Exception {
      assertThat(service.creerDemande(demandeur.getKeycloakId(), CODE, null).getMessage()).isNull();
    }

    @Test
    @DisplayName("refus : gestionnaire lui-même, déjà membre, demande en attente, complète, fermée")
    void refus() {
      assertThatThrownBy(() -> service.creerDemande(gestionnaire.getKeycloakId(), CODE, null))
          .isInstanceOf(BadRequestException.class);

      when(membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
              tontine.getId(), demandeur.getId()))
          .thenReturn(true);
      assertThatThrownBy(() -> service.creerDemande(demandeur.getKeycloakId(), CODE, null))
          .satisfies(e -> code(e, ErrorCode.MEMBER_ALREADY_IN_TONTINE));

      when(membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
              tontine.getId(), demandeur.getId()))
          .thenReturn(false);
      when(demandeRepository.existsByTontine_IdAndUser_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), demandeur.getId(), StatutDemande.EN_ATTENTE))
          .thenReturn(true);
      assertThatThrownBy(() -> service.creerDemande(demandeur.getKeycloakId(), CODE, null))
          .satisfies(e -> code(e, ErrorCode.DEMANDE_DEJA_EN_ATTENTE));

      when(demandeRepository.existsByTontine_IdAndUser_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), demandeur.getId(), StatutDemande.EN_ATTENTE))
          .thenReturn(false);
      when(membreRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(
              tontine.getId(), MembreStatut.ACTIF))
          .thenReturn(5L);
      assertThatThrownBy(() -> service.creerDemande(demandeur.getKeycloakId(), CODE, null))
          .satisfies(e -> code(e, ErrorCode.TONTINE_FULL));

      tontine.setStatut(TontineStatut.TERMINEE);
      assertThatThrownBy(() -> service.creerDemande(demandeur.getKeycloakId(), CODE, null))
          .satisfies(e -> code(e, ErrorCode.TONTINE_NOT_OPEN));
      verify(demandeRepository, never()).save(any());
    }
  }

  private DemandeAdhesion demande(StatutDemande statut) {
    DemandeAdhesion d =
        DemandeAdhesion.builder()
            .id(UUID.randomUUID())
            .tontine(tontine)
            .user(demandeur)
            .statut(statut)
            .build();
    lenient()
        .when(demandeRepository.findByIdAndTontine_IdAndDeletedAtIsNull(d.getId(), tontine.getId()))
        .thenReturn(Optional.of(d));
    return d;
  }

  @Test
  @DisplayName("listes : demandes reçues (avec téléphone) et mes demandes (sans)")
  void listes() throws Exception {
    DemandeAdhesion d = demande(StatutDemande.EN_ATTENTE);
    PageRequest page = PageRequest.of(0, 20);
    when(demandeRepository.findRecues(gestionnaire.getId(), StatutDemande.EN_ATTENTE, null, page))
        .thenReturn(new PageImpl<>(List.of(d)));
    when(demandeRepository.findByUser_IdAndDeletedAtIsNull(demandeur.getId(), page))
        .thenReturn(new PageImpl<>(List.of(d)));

    assertThat(
            service
                .demandesRecues(gestionnaire.getKeycloakId(), StatutDemande.EN_ATTENTE, null, page)
                .getContent()
                .getFirst()
                .getPhone())
        .isEqualTo(demandeur.getPhone());
    assertThat(service.mesDemandes(demandeur.getKeycloakId(), page).getContent().get(0).getPhone())
        .isNull();
  }

  @Test
  @DisplayName(
      "acceptation : ajout via MembreService (ordre transmis), demande ACCEPTEE et liée au membre")
  void acceptation() throws Exception {
    DemandeAdhesion d = demande(StatutDemande.EN_ATTENTE);
    UUID membreId = UUID.randomUUID();
    TontineMembre ref = TontineMembre.builder().id(membreId).build();
    when(membreService.addMembre(eq(gestionnaire.getKeycloakId()), eq(tontine.getId()), any()))
        .thenReturn(MembreResponse.builder().id(membreId).build());
    when(membreRepository.getReferenceById(membreId)).thenReturn(ref);
    DemandeRequests.Accepter req = new DemandeRequests.Accepter();
    req.setOrdreJackpot(4);

    DemandeAdhesionResponse r =
        service.accepter(gestionnaire.getKeycloakId(), tontine.getId(), d.getId(), req);

    ArgumentCaptor<AddMembreRequest> ajout = ArgumentCaptor.forClass(AddMembreRequest.class);
    verify(membreService)
        .addMembre(eq(gestionnaire.getKeycloakId()), eq(tontine.getId()), ajout.capture());
    assertThat(ajout.getValue().getPhone()).isEqualTo(demandeur.getPhone());
    assertThat(ajout.getValue().getOrdreJackpot()).isEqualTo(4);
    assertThat(r.getStatut()).isEqualTo(StatutDemande.ACCEPTEE);
    assertThat(r.getMembreId()).isEqualTo(membreId);
    assertThat(d.getTraiteePar()).isSameAs(gestionnaire);
    assertThat(d.getDateTraitement()).isNotNull();
  }

  @Test
  @DisplayName(
      "acceptation refusée : demande déjà traitée (DEMANDE_NOT_PENDING), tiers (403), inconnue (404)")
  void acceptationRefusee() throws Exception {
    DemandeAdhesion traitee = demande(StatutDemande.REFUSEE);
    assertThatThrownBy(
            () ->
                service.accepter(
                    gestionnaire.getKeycloakId(), tontine.getId(), traitee.getId(), null))
        .satisfies(e -> code(e, ErrorCode.DEMANDE_NOT_PENDING));

    DemandeAdhesion d = demande(StatutDemande.EN_ATTENTE);
    assertThatThrownBy(
            () -> service.accepter(demandeur.getKeycloakId(), tontine.getId(), d.getId(), null))
        .isInstanceOf(ForbiddenException.class);

    UUID inconnue = UUID.randomUUID();
    when(demandeRepository.findByIdAndTontine_IdAndDeletedAtIsNull(inconnue, tontine.getId()))
        .thenReturn(Optional.empty());
    assertThatThrownBy(
            () -> service.accepter(gestionnaire.getKeycloakId(), tontine.getId(), inconnue, null))
        .isInstanceOf(NotFoundException.class);
    verify(membreService, never()).addMembre(any(), any(), any());
  }

  @Test
  @DisplayName("refus : motif rogné et transmis au demandeur ; motif vide -> null")
  void refusDemande() throws Exception {
    DemandeAdhesion d = demande(StatutDemande.EN_ATTENTE);
    DemandeRequests.Refuser req = new DemandeRequests.Refuser();
    req.setMotif("  Tontine réservée à la famille ");

    DemandeAdhesionResponse r =
        service.refuser(gestionnaire.getKeycloakId(), tontine.getId(), d.getId(), req);

    assertThat(r.getStatut()).isEqualTo(StatutDemande.REFUSEE);
    assertThat(r.getMotifRefus()).isEqualTo("Tontine réservée à la famille");
    ArgumentCaptor<String> corps = ArgumentCaptor.forClass(String.class);
    verify(notificationService)
        .notify(
            eq(demandeur.getId()),
            eq(NotificationType.ADHESION_REFUSEE),
            anyString(),
            corps.capture(),
            eq(tontine.getId()),
            any());
    assertThat(corps.getValue()).contains("Motif : Tontine réservée à la famille");

    DemandeAdhesion d2 = demande(StatutDemande.EN_ATTENTE);
    req.setMotif("  ");
    assertThat(
            service
                .refuser(gestionnaire.getKeycloakId(), tontine.getId(), d2.getId(), req)
                .getMotifRefus())
        .isNull();
    DemandeAdhesion d3 = demande(StatutDemande.EN_ATTENTE);
    assertThat(
            service
                .refuser(gestionnaire.getKeycloakId(), tontine.getId(), d3.getId(), null)
                .getMotifRefus())
        .isNull();
  }
}
