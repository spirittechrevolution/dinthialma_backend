package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.util.FichierExport;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.codeList.Ponctualite;
import com.africa.dinthialma_backend.contribution.dto.AdminRecordCotisationRequest;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.CycleRecapCotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.MembreTotalCotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.RecordCotisationRequest;
import com.africa.dinthialma_backend.contribution.dto.RefuseCotisationRequest;
import com.africa.dinthialma_backend.contribution.dto.UpdateCotisationRequest;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.contribution.mapper.CotisationMapper;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.contribution.service.impl.CotisationServiceImpl;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("CotisationServiceImpl")
class CotisationServiceImplTest {

  @Mock private CotisationRepository cotisationRepository;
  @Spy private CotisationMapper cotisationMapper = Mappers.getMapper(CotisationMapper.class);
  @Mock private TontineRepository tontineRepository;
  @Mock private CycleTontineRepository cycleRepository;
  @Mock private TontineMembreRepository membreRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserRoleAssignmentRepository roleRepository;
  @Mock private AuditService auditService;
  @Mock private WhatsappService whatsappService;
  @Mock private NotificationService notificationService;
  @InjectMocks private CotisationServiceImpl service;

  private User gestionnaire;
  private User awa;
  private User etranger;
  private Tontine tontine;
  private TontineMembre membre;
  private CycleTontine cycle;

  @BeforeEach
  void setUp() {
    gestionnaire = Fixtures.user("Gestionnaire");
    awa = Fixtures.user("Awa");
    etranger = Fixtures.user("Étranger");
    tontine = Fixtures.tontine(gestionnaire);
    membre = Fixtures.membre(tontine, awa, 1);
    cycle = Fixtures.cycle(tontine, 1, CycleStatut.EN_COURS);
    for (User u : List.of(gestionnaire, awa, etranger)) {
      lenient().when(userRepository.findByKeycloakId(u.getKeycloakId())).thenReturn(Optional.of(u));
    }
    lenient()
        .when(tontineRepository.findByIdAndDeletedAtIsNull(tontine.getId()))
        .thenReturn(Optional.of(tontine));
    lenient()
        .when(
            cycleRepository.findByIdAndTontine_IdAndDeletedAtIsNull(cycle.getId(), tontine.getId()))
        .thenReturn(Optional.of(cycle));
    lenient()
        .when(
            membreRepository.findByTontine_IdAndUser_IdAndDeletedAtIsNull(
                tontine.getId(), awa.getId()))
        .thenReturn(Optional.of(membre));
    lenient()
        .when(
            membreRepository.findByIdAndTontine_IdAndDeletedAtIsNull(
                membre.getId(), tontine.getId()))
        .thenReturn(Optional.of(membre));
    lenient()
        .when(roleRepository.existsByUserIdAndRole(any(), eq(UserRole.SUPER_ADMIN)))
        .thenReturn(false);
    lenient()
        .when(cotisationRepository.save(any()))
        .thenAnswer(
            i -> {
              Cotisation c = i.getArgument(0);
              if (c.getId() == null) c.setId(UUID.randomUUID());
              return c;
            });
  }

  private static void code(Throwable e, ErrorCode attendu) {
    assertThat(((CustomException) e).getCode()).isEqualTo(attendu);
  }

  private RecordCotisationRequest declaration(String montant) {
    RecordCotisationRequest r = new RecordCotisationRequest();
    r.setCycleId(cycle.getId());
    r.setMontant(new BigDecimal(montant));
    r.setMethodePaiement("WAVE");
    r.setReferenceTransaction("REF-1");
    return r;
  }

  private AdminRecordCotisationRequest saisie(String montant, String ref) {
    AdminRecordCotisationRequest r = new AdminRecordCotisationRequest();
    r.setMembreId(membre.getId());
    r.setCycleId(cycle.getId());
    r.setMontant(new BigDecimal(montant));
    r.setMethodePaiement("CASH");
    r.setReferenceTransaction(ref);
    return r;
  }

  private Cotisation existante(CotisationStatut statut) {
    Cotisation c = Fixtures.cotisation(membre, cycle, statut);
    lenient()
        .when(cotisationRepository.findByIdAndDeletedAtIsNull(c.getId()))
        .thenReturn(Optional.of(c));
    return c;
  }

  @Nested
  @DisplayName("déclaration par le membre")
  class Declaration {

    @Test
    @DisplayName(
        "EN_ATTENTE, saisie par le membre, membre + gestionnaire notifiés, WhatsApp non bloquant")
    void succes() throws Exception {
      doThrow(new RuntimeException("WhatsApp KO"))
          .when(whatsappService)
          .send(anyString(), anyString());

      CotisationResponse r =
          service.recordCotisation(awa.getKeycloakId(), tontine.getId(), declaration("1000"));

      assertThat(r.getStatut()).isEqualTo(CotisationStatut.EN_ATTENTE);
      assertThat(r.getMembre().getMembreId()).isEqualTo(membre.getId());
      assertThat(r.getEnregistrePar().getId()).isEqualTo(awa.getId());
      assertThat(r.getNumeroCycle()).isEqualTo(1);
      verify(notificationService)
          .notify(
              eq(awa.getId()),
              eq(NotificationType.COTISATION_SOUMISE),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
      verify(notificationService)
          .notify(
              eq(gestionnaire.getId()),
              eq(NotificationType.PAIEMENT_RECU),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
      verify(whatsappService, times(2)).send(anyString(), anyString());
      verify(auditService).logCreate(eq(awa), eq("cotisations"), any());
    }

    @Test
    @DisplayName(
        "refus : non membre (403), cycle non EN_COURS, déjà déclarée (409), cycle inconnu (404)")
    void refus() {
      assertThatThrownBy(
              () ->
                  service.recordCotisation(
                      etranger.getKeycloakId(), tontine.getId(), declaration("1000")))
          .isInstanceOf(ForbiddenException.class);

      when(cotisationRepository.existsActiveByCycleAndMembre(cycle.getId(), membre.getId()))
          .thenReturn(true);
      assertThatThrownBy(
              () ->
                  service.recordCotisation(
                      awa.getKeycloakId(), tontine.getId(), declaration("1000")))
          .satisfies(e -> code(e, ErrorCode.COTISATION_ALREADY_EXISTS));

      cycle.setStatut(CycleStatut.TERMINE);
      assertThatThrownBy(
              () ->
                  service.recordCotisation(
                      awa.getKeycloakId(), tontine.getId(), declaration("1000")))
          .satisfies(e -> code(e, ErrorCode.NO_CYCLE_IN_PROGRESS));

      RecordCotisationRequest r = declaration("1000");
      r.setCycleId(UUID.randomUUID());
      when(cycleRepository.findByIdAndTontine_IdAndDeletedAtIsNull(r.getCycleId(), tontine.getId()))
          .thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.recordCotisation(awa.getKeycloakId(), tontine.getId(), r))
          .isInstanceOf(NotFoundException.class);
      verify(cotisationRepository, never()).save(any());
    }

    @Test
    @DisplayName(
        "ROTATIVE : montant non contrôlé ; EVENEMENTIELLE fixe : montant exact (AMOUNT_MISMATCH)")
    void montantFixe() throws Exception {
      assertThat(
              service
                  .recordCotisation(awa.getKeycloakId(), tontine.getId(), declaration("1234"))
                  .getMontant())
          .isEqualByComparingTo("1234");

      tontine.setTontineType(TontineType.EVENEMENTIELLE);
      assertThatThrownBy(
              () ->
                  service.recordCotisation(
                      awa.getKeycloakId(), tontine.getId(), declaration("999")))
          .satisfies(e -> code(e, ErrorCode.AMOUNT_MISMATCH));
      assertThat(
              service.recordCotisation(
                  awa.getKeycloakId(), tontine.getId(), declaration("1000.00")))
          .isNotNull();
    }

    @Test
    @DisplayName(
        "EVENEMENTIELLE libre : plancher montantMinimum (AMOUNT_BELOW_MINIMUM), sinon libre")
    void montantLibre() throws Exception {
      tontine.setTontineType(TontineType.EVENEMENTIELLE);
      tontine.setMontantLibre(true);
      tontine.setMontantMinimum(new BigDecimal("500"));
      assertThatThrownBy(
              () ->
                  service.recordCotisation(
                      awa.getKeycloakId(), tontine.getId(), declaration("499")))
          .satisfies(e -> code(e, ErrorCode.AMOUNT_BELOW_MINIMUM));
      assertThat(
              service.recordCotisation(awa.getKeycloakId(), tontine.getId(), declaration("7500")))
          .isNotNull();
      tontine.setMontantMinimum(null);
      assertThat(service.recordCotisation(awa.getKeycloakId(), tontine.getId(), declaration("1")))
          .isNotNull();
    }
  }

  @Nested
  @DisplayName("saisie par le gestionnaire")
  class Saisie {

    @Test
    @DisplayName(
        "créée VALIDE, validée par le gestionnaire, membre notifié + WhatsApp avec référence")
    void succes() throws Exception {
      CotisationResponse r =
          service.adminRecordCotisation(
              gestionnaire.getKeycloakId(), tontine.getId(), saisie("1000", "OM-42"));

      assertThat(r.getStatut()).isEqualTo(CotisationStatut.VALIDE);
      assertThat(r.getValidePar().getId()).isEqualTo(gestionnaire.getId());
      assertThat(r.getDateValidation()).isNotNull();
      verify(notificationService)
          .notify(
              eq(awa.getId()),
              eq(NotificationType.COTISATION_VALIDEE),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
      verify(whatsappService).send(eq(awa.getPhone()), contains("Référence : OM-42"));
      verify(whatsappService).send(eq(gestionnaire.getPhone()), anyString());
    }

    @Test
    @DisplayName(
        "membre PRE_ENROLLED : pas de notification in-app, WhatsApp d'invitation à s'inscrire")
    void preInscrit() throws Exception {
      awa.setAccountStatus(AccountStatus.PRE_ENROLLED);
      doThrow(new RuntimeException("KO"))
          .when(whatsappService)
          .send(eq(gestionnaire.getPhone()), anyString());

      service.adminRecordCotisation(
          gestionnaire.getKeycloakId(), tontine.getId(), saisie("1000", null));

      verify(notificationService, never()).notify(any(), any(), any(), any(), any(), any());
      verify(whatsappService).send(eq(awa.getPhone()), contains("Inscrivez-vous"));
    }

    @Test
    @DisplayName(
        "paiement remis en main propre : datePaiement enregistrée et prise pour la ponctualité ;"
            + " avant le début du tour -> 400")
    void datePaiement() throws Exception {
      AdminRecordCotisationRequest hier = saisie("1000", null);
      hier.setDatePaiement(cycle.getDateFin().atTime(23, 0));
      CotisationResponse r =
          service.adminRecordCotisation(gestionnaire.getKeycloakId(), tontine.getId(), hier);
      assertThat(r.getDatePaiement()).isEqualTo(cycle.getDateFin().atTime(23, 0));
      assertThat(r.getPonctualite()).isEqualTo(Ponctualite.A_TEMPS);

      AdminRecordCotisationRequest tropTot = saisie("1000", null);
      tropTot.setDatePaiement(cycle.getDateDebut().minusDays(1).atStartOfDay());
      assertThatThrownBy(
              () ->
                  service.adminRecordCotisation(
                      gestionnaire.getKeycloakId(), tontine.getId(), tropTot))
          .isInstanceOf(BadRequestException.class)
          .satisfies(e -> code(e, ErrorCode.INVALID_PARAMETER));
    }

    @Test
    @DisplayName("sans référence : message sans ligne Référence")
    void sansReference() throws Exception {
      service.adminRecordCotisation(
          gestionnaire.getKeycloakId(), tontine.getId(), saisie("1000", " "));
      ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
      verify(whatsappService).send(eq(awa.getPhone()), msg.capture());
      assertThat(msg.getValue()).doesNotContain("Référence");
    }

    @Test
    @DisplayName(
        "refus : tiers (403), membre inconnu (404), cycle clôturé, doublon (409), montant événementiel")
    void refus() {
      assertThatThrownBy(
              () ->
                  service.adminRecordCotisation(
                      awa.getKeycloakId(), tontine.getId(), saisie("1000", null)))
          .isInstanceOf(ForbiddenException.class);

      AdminRecordCotisationRequest inconnu = saisie("1000", null);
      inconnu.setMembreId(UUID.randomUUID());
      when(membreRepository.findByIdAndTontine_IdAndDeletedAtIsNull(
              inconnu.getMembreId(), tontine.getId()))
          .thenReturn(Optional.empty());
      assertThatThrownBy(
              () ->
                  service.adminRecordCotisation(
                      gestionnaire.getKeycloakId(), tontine.getId(), inconnu))
          .isInstanceOf(NotFoundException.class);

      tontine.setTontineType(TontineType.EVENEMENTIELLE);
      assertThatThrownBy(
              () ->
                  service.adminRecordCotisation(
                      gestionnaire.getKeycloakId(), tontine.getId(), saisie("10", null)))
          .satisfies(e -> code(e, ErrorCode.AMOUNT_MISMATCH));

      when(cotisationRepository.existsActiveByCycleAndMembre(cycle.getId(), membre.getId()))
          .thenReturn(true);
      assertThatThrownBy(
              () ->
                  service.adminRecordCotisation(
                      gestionnaire.getKeycloakId(), tontine.getId(), saisie("1000", null)))
          .satisfies(e -> code(e, ErrorCode.COTISATION_ALREADY_EXISTS));

      cycle.setStatut(CycleStatut.EN_ATTENTE);
      assertThatThrownBy(
              () ->
                  service.adminRecordCotisation(
                      gestionnaire.getKeycloakId(), tontine.getId(), saisie("1000", null)))
          .satisfies(e -> code(e, ErrorCode.NO_CYCLE_IN_PROGRESS));
      verify(cotisationRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("validation et refus")
  class ValidationEtRefus {

    @Test
    @DisplayName("validation : VALIDE, validateur et date posés, audit, notification et WhatsApp")
    void validation() throws Exception {
      Cotisation c = existante(CotisationStatut.EN_ATTENTE);
      c.setReferenceTransaction("REF-9");
      doThrow(new RuntimeException("KO"))
          .when(whatsappService)
          .send(eq(gestionnaire.getPhone()), anyString());

      CotisationResponse r =
          service.validateCotisation(gestionnaire.getKeycloakId(), tontine.getId(), c.getId());

      assertThat(r.getStatut()).isEqualTo(CotisationStatut.VALIDE);
      assertThat(c.getValidePar()).isSameAs(gestionnaire);
      assertThat(c.getDateValidation()).isNotNull();
      verify(auditService)
          .log(gestionnaire, "cotisations", c.getId(), "statut", "EN_ATTENTE", "VALIDE");
      verify(notificationService)
          .notify(
              eq(awa.getId()),
              eq(NotificationType.COTISATION_VALIDEE),
              anyString(),
              anyString(),
              eq(tontine.getId()),
              any());
      verify(whatsappService).send(eq(awa.getPhone()), contains("Référence : REF-9"));
    }

    @Test
    @DisplayName("validation sans référence : message simple")
    void validationSansReference() throws Exception {
      Cotisation c = existante(CotisationStatut.EN_ATTENTE);
      service.validateCotisation(gestionnaire.getKeycloakId(), tontine.getId(), c.getId());
      ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
      verify(whatsappService).send(eq(awa.getPhone()), msg.capture());
      assertThat(msg.getValue()).doesNotContain("Référence");
    }

    @Test
    @DisplayName("refus : REFUSE, motif rogné, refuseur, membre notifié avec motif")
    void refus() throws Exception {
      Cotisation c = existante(CotisationStatut.EN_ATTENTE);
      RefuseCotisationRequest req = new RefuseCotisationRequest();
      req.setMotif("  Référence introuvable ");
      doThrow(new RuntimeException("KO")).when(whatsappService).send(anyString(), anyString());

      CotisationResponse r =
          service.refuseCotisation(gestionnaire.getKeycloakId(), tontine.getId(), c.getId(), req);

      assertThat(r.getStatut()).isEqualTo(CotisationStatut.REFUSE);
      assertThat(r.getMotifRefus()).isEqualTo("Référence introuvable");
      assertThat(r.getRefusePar().getId()).isEqualTo(gestionnaire.getId());
      assertThat(c.getDateRefus()).isNotNull();
      ArgumentCaptor<String> corps = ArgumentCaptor.forClass(String.class);
      verify(notificationService)
          .notify(
              eq(awa.getId()),
              eq(NotificationType.COTISATION_REFUSEE),
              anyString(),
              corps.capture(),
              eq(tontine.getId()),
              any());
      assertThat(corps.getValue()).contains("Référence introuvable");
    }

    @Test
    @DisplayName(
        "hors EN_ATTENTE (COTISATION_NOT_PENDING), autre tontine ou inconnue (404), tiers (403)")
    void transitionsInterdites() {
      Cotisation valide = existante(CotisationStatut.VALIDE);
      RefuseCotisationRequest req = new RefuseCotisationRequest();
      req.setMotif("x");
      assertThatThrownBy(
              () ->
                  service.validateCotisation(
                      gestionnaire.getKeycloakId(), tontine.getId(), valide.getId()))
          .satisfies(e -> code(e, ErrorCode.COTISATION_NOT_PENDING));
      assertThatThrownBy(
              () ->
                  service.refuseCotisation(
                      gestionnaire.getKeycloakId(), tontine.getId(), valide.getId(), req))
          .satisfies(e -> code(e, ErrorCode.COTISATION_NOT_PENDING));

      Cotisation ailleurs = existante(CotisationStatut.EN_ATTENTE);
      ailleurs.setTontine(Fixtures.tontine(gestionnaire));
      assertThatThrownBy(
              () ->
                  service.validateCotisation(
                      gestionnaire.getKeycloakId(), tontine.getId(), ailleurs.getId()))
          .isInstanceOf(NotFoundException.class);
      assertThatThrownBy(
              () ->
                  service.refuseCotisation(
                      gestionnaire.getKeycloakId(), tontine.getId(), ailleurs.getId(), req))
          .isInstanceOf(NotFoundException.class);

      UUID inconnue = UUID.randomUUID();
      when(cotisationRepository.findByIdAndDeletedAtIsNull(inconnue)).thenReturn(Optional.empty());
      assertThatThrownBy(
              () ->
                  service.validateCotisation(
                      gestionnaire.getKeycloakId(), tontine.getId(), inconnue))
          .isInstanceOf(NotFoundException.class);
      assertThatThrownBy(
              () ->
                  service.refuseCotisation(
                      gestionnaire.getKeycloakId(), tontine.getId(), inconnue, req))
          .isInstanceOf(NotFoundException.class);
      assertThatThrownBy(
              () ->
                  service.validateCotisation(awa.getKeycloakId(), tontine.getId(), valide.getId()))
          .isInstanceOf(ForbiddenException.class);
    }
  }

  @Nested
  @DisplayName("consultation")
  class Consultation {

    private final Pageable page = PageRequest.of(0, 20);

    @Test
    @DisplayName("liste : gestionnaire et membre ; tiers -> 403")
    @SuppressWarnings("unchecked")
    void liste() throws Exception {
      Cotisation c = existante(CotisationStatut.VALIDE);
      when(cotisationRepository.findAll(any(Specification.class), eq(page)))
          .thenReturn(new PageImpl<>(List.of(c)));

      assertThat(
              service.listCotisations(
                  gestionnaire.getKeycloakId(),
                  tontine.getId(),
                  cycle.getId(),
                  membre.getId(),
                  List.of(CotisationStatut.VALIDE),
                  "awa",
                  page))
          .hasSize(1);
      assertThat(
              service.listCotisations(
                  awa.getKeycloakId(), tontine.getId(), null, UUID.randomUUID(), null, null, page))
          .hasSize(1);
      assertThatThrownBy(
              () ->
                  service.listCotisations(
                      etranger.getKeycloakId(), tontine.getId(), null, null, null, null, page))
          .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName(
        "détail : propriétaire et gestionnaire oui ; autre membre ou tiers non ; autre tontine -> 404")
    void detail() throws Exception {
      Cotisation c = existante(CotisationStatut.EN_ATTENTE);
      assertThat(service.getCotisation(awa.getKeycloakId(), tontine.getId(), c.getId()).getId())
          .isEqualTo(c.getId());
      assertThat(service.getCotisation(gestionnaire.getKeycloakId(), tontine.getId(), c.getId()))
          .isNotNull();

      User fatou = Fixtures.user("Fatou");
      when(userRepository.findByKeycloakId(fatou.getKeycloakId())).thenReturn(Optional.of(fatou));
      when(membreRepository.findByTontine_IdAndUser_IdAndDeletedAtIsNull(
              tontine.getId(), fatou.getId()))
          .thenReturn(Optional.of(Fixtures.membre(tontine, fatou, 2)));
      assertThatThrownBy(
              () -> service.getCotisation(fatou.getKeycloakId(), tontine.getId(), c.getId()))
          .isInstanceOf(ForbiddenException.class);
      assertThatThrownBy(
              () -> service.getCotisation(etranger.getKeycloakId(), tontine.getId(), c.getId()))
          .isInstanceOf(ForbiddenException.class);

      c.setTontine(Fixtures.tontine(gestionnaire));
      assertThatThrownBy(
              () -> service.getCotisation(gestionnaire.getKeycloakId(), tontine.getId(), c.getId()))
          .isInstanceOf(NotFoundException.class);
      UUID inconnue = UUID.randomUUID();
      when(cotisationRepository.findByIdAndDeletedAtIsNull(inconnue)).thenReturn(Optional.empty());
      assertThatThrownBy(
              () -> service.getCotisation(gestionnaire.getKeycloakId(), tontine.getId(), inconnue))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName(
        "export CSV : en-tête, une ligne par cotisation, nom de fichier ; format autre que csv -> 400")
    @SuppressWarnings("unchecked")
    void export() throws Exception {
      Cotisation c = existante(CotisationStatut.REFUSE);
      c.setMotifRefus("doublon");
      when(cotisationRepository.findAll(any(Specification.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(c)));

      FichierExport f =
          service.exportCotisations(
              gestionnaire.getKeycloakId(), tontine.getId(), "CSV", null, null);

      String contenu = new String(f.contenu(), StandardCharsets.UTF_8);
      assertThat(f.nomFichier()).isEqualTo("cotisations-" + tontine.getId() + ".csv");
      assertThat(contenu)
          .contains("Tour", "Montant (FCFA)")
          .contains("Awa Test")
          .contains("doublon");
      assertThat(contenu.lines().filter(l -> !l.isBlank())).hasSize(2);

      assertThatThrownBy(
              () ->
                  service.exportCotisations(
                      gestionnaire.getKeycloakId(), tontine.getId(), "xlsx", null, null))
          .satisfies(e -> code(e, ErrorCode.INVALID_PARAMETER));
      assertThatThrownBy(
              () ->
                  service.exportCotisations(
                      awa.getKeycloakId(), tontine.getId(), "csv", null, null))
          .isInstanceOf(ForbiddenException.class);
    }
  }

  @Nested
  @DisplayName("correction par le gestionnaire")
  class Correction {

    private UpdateCotisationRequest req(String montant, String methode, String ref, String note) {
      UpdateCotisationRequest r = new UpdateCotisationRequest();
      r.setMontant(montant != null ? new BigDecimal(montant) : null);
      r.setMethodePaiement(methode);
      r.setReferenceTransaction(ref);
      r.setNote(note);
      return r;
    }

    @Test
    @DisplayName("EN_ATTENTE : champs fournis modifiés et audités ; montant identique non audité")
    void enAttente() throws Exception {
      Cotisation c = existante(CotisationStatut.EN_ATTENTE);

      CotisationResponse r =
          service.updateCotisation(
              gestionnaire.getKeycloakId(),
              tontine.getId(),
              c.getId(),
              req("1500", "OM", "R2", "corrigé"));

      assertThat(r.getMontant()).isEqualByComparingTo("1500");
      assertThat(r.getMethodePaiement()).isEqualTo("OM");
      assertThat(r.getReferenceTransaction()).isEqualTo("R2");
      assertThat(r.getNote()).isEqualTo("corrigé");
      verify(auditService).log(gestionnaire, "cotisations", c.getId(), "montant", "1000", "1500");
      verify(auditService, times(3))
          .log(eq(gestionnaire), eq("cotisations"), eq(c.getId()), anyString(), any(), any());

      service.updateCotisation(
          gestionnaire.getKeycloakId(),
          tontine.getId(),
          c.getId(),
          req("1500.00", null, null, null));
      verify(auditService, times(3))
          .log(eq(gestionnaire), eq("cotisations"), eq(c.getId()), anyString(), any(), any());
    }

    @Test
    @DisplayName(
        "VALIDE sur cycle EN_COURS autorisée ; VALIDE sur cycle TERMINE, EN_RETARD, REFUSE -> COTISATION_NOT_EDITABLE")
    void editabilite() throws Exception {
      Cotisation valide = existante(CotisationStatut.VALIDE);
      assertThat(
              service.updateCotisation(
                  gestionnaire.getKeycloakId(),
                  tontine.getId(),
                  valide.getId(),
                  req(null, null, null, "ok")))
          .isNotNull();

      cycle.setStatut(CycleStatut.TERMINE);
      assertThatThrownBy(
              () ->
                  service.updateCotisation(
                      gestionnaire.getKeycloakId(),
                      tontine.getId(),
                      valide.getId(),
                      req(null, null, null, "x")))
          .satisfies(e -> code(e, ErrorCode.COTISATION_NOT_EDITABLE));
      for (CotisationStatut s : List.of(CotisationStatut.EN_RETARD, CotisationStatut.REFUSE)) {
        Cotisation c = existante(s);
        assertThatThrownBy(
                () ->
                    service.updateCotisation(
                        gestionnaire.getKeycloakId(),
                        tontine.getId(),
                        c.getId(),
                        req(null, null, null, "x")))
            .satisfies(e -> code(e, ErrorCode.COTISATION_NOT_EDITABLE));
      }
    }

    @Test
    @DisplayName("montant hors règle événementielle refusé ; autre tontine -> 404 ; tiers -> 403")
    void refus() {
      Cotisation c = existante(CotisationStatut.EN_ATTENTE);
      tontine.setTontineType(TontineType.EVENEMENTIELLE);
      assertThatThrownBy(
              () ->
                  service.updateCotisation(
                      gestionnaire.getKeycloakId(),
                      tontine.getId(),
                      c.getId(),
                      req("1200", null, null, null)))
          .satisfies(e -> code(e, ErrorCode.AMOUNT_MISMATCH));
      assertThatThrownBy(
              () ->
                  service.updateCotisation(
                      awa.getKeycloakId(), tontine.getId(), c.getId(), req(null, null, null, null)))
          .isInstanceOf(ForbiddenException.class);
      c.setTontine(Fixtures.tontine(gestionnaire));
      assertThatThrownBy(
              () ->
                  service.updateCotisation(
                      gestionnaire.getKeycloakId(),
                      tontine.getId(),
                      c.getId(),
                      req(null, null, null, null)))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("récapitulatifs")
  class Recaps {

    private final Pageable page = PageRequest.of(0, 20);

    @Test
    @DisplayName("récap d'un tour : statut par membre, la cotisation active prime sur une refusée")
    void recapTour() throws Exception {
      TontineMembre fatou = Fixtures.membre(tontine, Fixtures.user("Fatou"), 2);
      when(membreRepository.findByTontine_IdAndStatutNotAndDeletedAtIsNull(
              tontine.getId(), MembreStatut.SORTI, page))
          .thenReturn(new PageImpl<>(List.of(membre, fatou)));
      Cotisation refusee = Fixtures.cotisation(membre, cycle, CotisationStatut.REFUSE);
      refusee.setCreatedAt(LocalDateTime.now());
      Cotisation active = Fixtures.cotisation(membre, cycle, CotisationStatut.EN_ATTENTE);
      active.setCreatedAt(LocalDateTime.now().minusHours(1));
      when(cotisationRepository.findByCycle_IdAndMembre_IdInAndDeletedAtIsNull(
              eq(cycle.getId()), anyList()))
          .thenReturn(List.of(refusee, active));

      List<CycleRecapCotisationResponse> r =
          service
              .getCycleRecap(gestionnaire.getKeycloakId(), tontine.getId(), cycle.getId(), page)
              .getContent();

      assertThat(r.get(0).getCotisationId()).isEqualTo(active.getId());
      assertThat(r.get(0).getStatutCotisation()).isEqualTo(CotisationStatut.EN_ATTENTE);
      assertThat(r.get(1).getCotisationId()).isNull();
      assertThat(r.get(1).getFirstName()).isEqualTo("Fatou");
    }

    @Test
    @DisplayName("récap d'un tour : page vide sans requête de cotisations ; cycle inconnu -> 404")
    void recapVide() throws Exception {
      when(membreRepository.findByTontine_IdAndStatutNotAndDeletedAtIsNull(
              tontine.getId(), MembreStatut.SORTI, page))
          .thenReturn(Page.empty(page));
      assertThat(
              service.getCycleRecap(
                  gestionnaire.getKeycloakId(), tontine.getId(), cycle.getId(), page))
          .isEmpty();
      verify(cotisationRepository, never())
          .findByCycle_IdAndMembre_IdInAndDeletedAtIsNull(any(), any());

      UUID inconnu = UUID.randomUUID();
      when(cycleRepository.findByIdAndTontine_IdAndDeletedAtIsNull(inconnu, tontine.getId()))
          .thenReturn(Optional.empty());
      assertThatThrownBy(
              () ->
                  service.getCycleRecap(
                      gestionnaire.getKeycloakId(), tontine.getId(), inconnu, page))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName(
        "récap total : somme et nombre de VALIDE par membre, 0 sans cotisation ; tiers -> 403")
    void recapTotal() throws Exception {
      TontineMembre fatou = Fixtures.membre(tontine, Fixtures.user("Fatou"), 2);
      when(membreRepository.findByTontine_IdAndStatutNotAndDeletedAtIsNull(
              tontine.getId(), MembreStatut.SORTI, page))
          .thenReturn(new PageImpl<>(List.of(membre, fatou)));
      List<Object[]> lignes =
          List.<Object[]>of(new Object[] {membre.getId(), new BigDecimal("3000"), 3L});
      when(cotisationRepository.sumAndCountValideGroupByMembreIn(eq(tontine.getId()), anyList()))
          .thenReturn(lignes);

      List<MembreTotalCotisationResponse> r =
          service
              .getTontineRecapTotal(gestionnaire.getKeycloakId(), tontine.getId(), page)
              .getContent();

      assertThat(r.get(0).getTotalCotise()).isEqualByComparingTo("3000");
      assertThat(r.get(0).getNombreCotisationsValidees()).isEqualTo(3);
      assertThat(r.get(1).getTotalCotise()).isEqualByComparingTo("0");
      assertThat(r.get(1).getNombreCotisationsValidees()).isZero();

      when(membreRepository.findByTontine_IdAndStatutNotAndDeletedAtIsNull(
              tontine.getId(), MembreStatut.SORTI, page))
          .thenReturn(Page.empty(page));
      assertThat(service.getTontineRecapTotal(gestionnaire.getKeycloakId(), tontine.getId(), page))
          .isEmpty();
      assertThatThrownBy(
              () -> service.getTontineRecapTotal(awa.getKeycloakId(), tontine.getId(), page))
          .isInstanceOf(ForbiddenException.class);
    }
  }
}
