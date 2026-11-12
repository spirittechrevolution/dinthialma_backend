package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.util.FichierExport;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.contribution.mapper.CotisationMapper;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.contribution.service.interfaces.CotisationService;
import com.africa.dinthialma_backend.me.codeList.ModeActivite;
import com.africa.dinthialma_backend.me.codeList.StatutCotisationDue;
import com.africa.dinthialma_backend.me.codeList.TypeActivite;
import com.africa.dinthialma_backend.me.dto.ActiviteResponse;
import com.africa.dinthialma_backend.me.dto.MeSummaryResponse;
import com.africa.dinthialma_backend.me.dto.MesJackpotsResponse;
import com.africa.dinthialma_backend.me.dto.ValidationGroupeeRequest;
import com.africa.dinthialma_backend.me.dto.ValidationGroupeeResponse;
import com.africa.dinthialma_backend.me.mapper.ActiviteMapper;
import com.africa.dinthialma_backend.me.repository.ActiviteRepository;
import com.africa.dinthialma_backend.me.service.impl.MeServiceImpl;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.CycleGagnant;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleGagnantRepository;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("MeServiceImpl")
class MeServiceImplTest {

  @Mock private UserRepository userRepository;
  @Spy private CotisationMapper cotisationMapper = Mappers.getMapper(CotisationMapper.class);
  @Spy private ActiviteMapper activiteMapper = Mappers.getMapper(ActiviteMapper.class);
  @Mock private CotisationRepository cotisationRepository;
  @Mock private TontineMembreRepository membreRepository;
  @Mock private CycleTontineRepository cycleRepository;
  @Mock private CycleGagnantRepository gagnantRepository;
  @Mock private CotisationService cotisationService;
  @Mock private ActiviteRepository activiteRepository;
  @InjectMocks private MeServiceImpl service;

  private User moi;
  private User gestionnaire;
  private String kc;

  @BeforeEach
  void setUp() {
    moi = Fixtures.user("Awa");
    gestionnaire = Fixtures.user("Gestionnaire");
    kc = moi.getKeycloakId();
    lenient().when(userRepository.findByKeycloakId(kc)).thenReturn(Optional.of(moi));
  }

  private void mesAdhesions(TontineMembre... membres) {
    when(membreRepository
            .findByUser_IdAndDeletedAtIsNullAndTontine_DeletedAtIsNullOrderByCreatedAtDesc(
                moi.getId()))
        .thenReturn(List.of(membres));
  }

  @Nested
  @DisplayName("résumé « Je cotise »")
  class Resume {

    @Test
    @DisplayName("aucune adhésion : totaux à zéro, jackpots reçus repris")
    void vide() throws Exception {
      mesAdhesions();
      when(gagnantRepository.sumMontantRecuByUserId(moi.getId()))
          .thenReturn(new BigDecimal("5000"));

      MeSummaryResponse r = service.getSummary(kc);

      assertThat(r.getTontines()).isEmpty();
      assertThat(r.getTotalCotiseValide()).isEqualByComparingTo("0");
      assertThat(r.getTotalJackpotsRecus()).isEqualByComparingTo("5000");
      assertThat(r.getNombreCotisationsDues()).isZero();
    }

    @Test
    @DisplayName(
        "par tontine : totaux, tours, tour en cours, cotisation due et gestionnaire ; agrégats globaux")
    void complet() throws Exception {
      Tontine aPayer = Fixtures.tontine(gestionnaire);
      Tontine payee = Fixtures.tontine(gestionnaire);
      Tontine refusee = Fixtures.tontine(gestionnaire);
      Tontine sansTour = Fixtures.tontine(gestionnaire);
      TontineMembre m1 = Fixtures.membre(aPayer, moi, 1);
      TontineMembre m2 = Fixtures.membre(payee, moi, 1);
      TontineMembre m3 = Fixtures.membre(refusee, moi, 1);
      TontineMembre m4 = Fixtures.membre(sansTour, moi, 1);
      mesAdhesions(m1, m2, m3, m4);
      CycleTontine c1 = Fixtures.cycle(aPayer, 2, CycleStatut.EN_COURS);
      CycleTontine c2 = Fixtures.cycle(payee, 1, CycleStatut.EN_COURS);
      CycleTontine c3 = Fixtures.cycle(refusee, 1, CycleStatut.EN_COURS);
      when(gagnantRepository.sumMontantRecuByUserId(moi.getId())).thenReturn(BigDecimal.ZERO);
      when(cotisationRepository.countAndSumByMembreAndStatut(anyList()))
          .thenReturn(
              List.of(
                  new Object[] {m1.getId(), CotisationStatut.VALIDE, 1L, new BigDecimal("1000")},
                  new Object[] {m2.getId(), CotisationStatut.VALIDE, 2L, new BigDecimal("2000")},
                  new Object[] {
                    m2.getId(), CotisationStatut.EN_ATTENTE, 1L, new BigDecimal("1000")
                  }));
      when(cycleRepository.countByTontineIds(anyList()))
          .thenReturn(List.<Object[]>of(new Object[] {aPayer.getId(), 5L}));
      when(gagnantRepository.sumAndCountByMembreIds(anyList()))
          .thenReturn(List.<Object[]>of(new Object[] {m2.getId(), new BigDecimal("9000"), 2L}));
      when(cycleRepository.findByTontine_IdInAndStatutAndDeletedAtIsNull(
              anyList(), eq(CycleStatut.EN_COURS)))
          .thenReturn(List.of(c1, c2, c3));
      Cotisation valide = Fixtures.cotisation(m2, c2, CotisationStatut.VALIDE);
      Cotisation refus = Fixtures.cotisation(m3, c3, CotisationStatut.REFUSE);
      refus.setMotifRefus("Montant erroné");
      when(cotisationRepository.findByCycle_IdInAndMembre_IdInAndDeletedAtIsNull(
              anyList(), anyList()))
          .thenReturn(List.of(valide, refus));

      MeSummaryResponse r = service.getSummary(kc);

      assertThat(r.getTotalCotiseValide()).isEqualByComparingTo("3000");
      assertThat(r.getTotalEnAttente()).isEqualByComparingTo("1000");
      assertThat(r.getNombreCotisationsDues()).isEqualTo(2);
      assertThat(r.getMontantDu()).isEqualByComparingTo("2000");

      MeSummaryResponse.TontineCotisant l1 = r.getTontines().get(0);
      assertThat(l1.getTotalJackpotsRecus()).isEqualByComparingTo("0");
      assertThat(l1.getNombreJackpotsRecus()).isZero();
      assertThat(r.getTontines().get(1).getTotalJackpotsRecus()).isEqualByComparingTo("9000");
      assertThat(r.getTontines().get(1).getNombreJackpotsRecus()).isEqualTo(2);
      assertThat(l1.getToursPayes()).isEqualTo(1);
      assertThat(l1.getToursTotal()).isEqualTo(5);
      assertThat(l1.getCycleEnCours().getNumeroCycle()).isEqualTo(2);
      assertThat(l1.getCotisationDue().getStatut()).isEqualTo(StatutCotisationDue.A_PAYER);
      assertThat(l1.getCotisationDue().getMontant()).isEqualByComparingTo("1000");
      assertThat(l1.getGestionnaire().getId()).isEqualTo(gestionnaire.getId());

      assertThat(r.getTontines().get(1).getCotisationDue().getStatut())
          .isEqualTo(StatutCotisationDue.VALIDE);
      assertThat(r.getTontines().get(1).getCotisationDue().getCotisationId())
          .isEqualTo(valide.getId());
      MeSummaryResponse.CotisationDue dueRefusee = r.getTontines().get(2).getCotisationDue();
      assertThat(dueRefusee.getStatut()).isEqualTo(StatutCotisationDue.A_PAYER);
      assertThat(dueRefusee.getMotifDernierRefus()).isEqualTo("Montant erroné");
      assertThat(r.getTontines().get(3).getCycleEnCours()).isNull();
      assertThat(r.getTontines().get(3).getCotisationDue()).isNull();
      assertThat(r.getTontines().get(3).getToursTotal()).isZero();
    }

    @Test
    @DisplayName(
        "cotisation EN_ATTENTE -> due EN_ATTENTE ; membre suspendu ou tontine suspendue -> aucune due")
    void casParticuliers() throws Exception {
      Tontine t1 = Fixtures.tontine(gestionnaire);
      Tontine t2 = Fixtures.tontine(gestionnaire);
      t2.setStatut(TontineStatut.SUSPENDUE);
      Tontine t3 = Fixtures.tontine(gestionnaire);
      TontineMembre m1 = Fixtures.membre(t1, moi, 1);
      TontineMembre m2 = Fixtures.membre(t2, moi, 1);
      TontineMembre m3 = Fixtures.membre(t3, moi, 1);
      m3.setStatut(MembreStatut.SUSPENDU);
      mesAdhesions(m1, m2, m3);
      CycleTontine c1 = Fixtures.cycle(t1, 1, CycleStatut.EN_COURS);
      when(cycleRepository.findByTontine_IdInAndStatutAndDeletedAtIsNull(
              anyList(), eq(CycleStatut.EN_COURS)))
          .thenReturn(
              List.of(
                  c1,
                  Fixtures.cycle(t2, 1, CycleStatut.EN_COURS),
                  Fixtures.cycle(t3, 1, CycleStatut.EN_COURS)));
      when(cotisationRepository.findByCycle_IdInAndMembre_IdInAndDeletedAtIsNull(
              anyList(), anyList()))
          .thenReturn(List.of(Fixtures.cotisation(m1, c1, CotisationStatut.EN_ATTENTE)));

      MeSummaryResponse r = service.getSummary(kc);

      assertThat(r.getTontines().get(0).getCotisationDue().getStatut())
          .isEqualTo(StatutCotisationDue.EN_ATTENTE);
      assertThat(r.getTontines().get(1).getCotisationDue()).isNull();
      assertThat(r.getTontines().get(2).getCotisationDue()).isNull();
      assertThat(r.getNombreCotisationsDues()).isZero();
    }

    @Test
    @DisplayName("utilisateur inconnu -> 404")
    void inconnu() {
      when(userRepository.findByKeycloakId("x")).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.getSummary("x")).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  @DisplayName("file « À valider » : page des cotisations des tontines que je gère")
  @SuppressWarnings("unchecked")
  void cotisationsGerees() throws Exception {
    Tontine t = Fixtures.tontine(moi);
    TontineMembre m = Fixtures.membre(t, Fixtures.user("Fatou"), 1);
    Pageable page = PageRequest.of(0, 20);
    when(cotisationRepository.findAll(any(Specification.class), eq(page)))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    Fixtures.cotisation(
                        m,
                        Fixtures.cycle(t, 1, CycleStatut.EN_COURS),
                        CotisationStatut.EN_ATTENTE))));

    List<CotisationResponse> r =
        service
            .listCotisationsGerees(kc, List.of(CotisationStatut.EN_ATTENTE), null, "fatou", page)
            .getContent();

    assertThat(r)
        .singleElement()
        .satisfies(c -> assertThat(c.getMembre().getFirstName()).isEqualTo("Fatou"));
  }

  @Nested
  @DisplayName("mes jackpots")
  class Jackpots {

    @Test
    @DisplayName("reçus : total et date de remise (repli sur la fin du tour)")
    void recus() throws Exception {
      Tontine t = Fixtures.tontine(gestionnaire);
      CycleTontine c1 = Fixtures.cycle(t, 1, CycleStatut.TERMINE);
      c1.setDateRemise(LocalDate.of(2026, 2, 1));
      CycleTontine c2 = Fixtures.cycle(t, 2, CycleStatut.TERMINE);
      TontineMembre m = Fixtures.membre(t, moi, 1);
      when(gagnantRepository.findRecusByUserId(moi.getId()))
          .thenReturn(
              List.of(
                  CycleGagnant.builder()
                      .cycle(c1)
                      .membre(m)
                      .montantRecu(new BigDecimal("4000"))
                      .build(),
                  CycleGagnant.builder().cycle(c2).membre(m).montantRecu(null).build()));
      mesAdhesions();

      MesJackpotsResponse r = service.getJackpots(kc);

      assertThat(r.getTotalRecu()).isEqualByComparingTo("4000");
      assertThat(r.getRecus())
          .extracting(MesJackpotsResponse.JackpotRecu::getDateRemise)
          .containsExactly(LocalDate.of(2026, 2, 1), c2.getDateFin());
      assertThat(r.getAVenir()).isEmpty();
    }

    @Test
    @DisplayName(
        "à venir ROTATION : rang parmi les éligibles, tour et date estimés, montant estimé")
    void rotation() throws Exception {
      Tontine t = Fixtures.tontine(gestionnaire);
      t.setNombreGagnants(1);
      List<TontineMembre> actifs = new ArrayList<>();
      TontineMembre servi = Fixtures.membre(t, Fixtures.user("Servi"), 1);
      servi.setARecuJackpot(true);
      actifs.add(servi);
      actifs.add(Fixtures.membre(t, Fixtures.user("B"), 2));
      TontineMembre mien = Fixtures.membre(t, moi, 4);
      actifs.add(mien);
      actifs.add(Fixtures.membre(t, Fixtures.user("C"), 3));
      mesAdhesions(mien);
      when(gagnantRepository.findRecusByUserId(moi.getId())).thenReturn(List.of());
      when(membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              t.getId(), MembreStatut.ACTIF))
          .thenReturn(actifs);
      List<CycleTontine> cycles =
          List.of(
              Fixtures.cycle(t, 1, CycleStatut.TERMINE),
              Fixtures.cycle(t, 2, CycleStatut.EN_COURS),
              Fixtures.cycle(t, 3, CycleStatut.EN_ATTENTE),
              Fixtures.cycle(t, 4, CycleStatut.EN_ATTENTE));
      when(cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(t.getId()))
          .thenReturn(cycles);

      MesJackpotsResponse.JackpotAVenir a = service.getJackpots(kc).getAVenir().get(0);

      assertThat(a.getModeAttribution()).isEqualTo("ORDRE");
      assertThat(a.getPosition()).isEqualTo(3);
      assertThat(a.getNumeroCycleEstime()).isEqualTo(4);
      assertThat(a.getDateEstimee()).isEqualTo(cycles.get(3).getDateFin());
      assertThat(a.getMontantEstime()).isEqualByComparingTo("4000");
    }

    @Test
    @DisplayName("aucun tour en cours : référence = dernier tour + 1, date inconnue")
    void sansTourEnCours() throws Exception {
      Tontine t = Fixtures.tontine(gestionnaire);
      TontineMembre mien = Fixtures.membre(t, moi, 1);
      mesAdhesions(mien);
      when(gagnantRepository.findRecusByUserId(moi.getId())).thenReturn(List.of());
      when(membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              t.getId(), MembreStatut.ACTIF))
          .thenReturn(List.of(mien));
      when(cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(t.getId()))
          .thenReturn(List.of(Fixtures.cycle(t, 1, CycleStatut.TERMINE)));

      MesJackpotsResponse.JackpotAVenir a = service.getJackpots(kc).getAVenir().get(0);

      assertThat(a.getNumeroCycleEstime()).isEqualTo(2);
      assertThat(a.getDateEstimee()).isNull();
    }

    @Test
    @DisplayName(
        "ALEATOIRE -> TIRAGE, MANUEL -> CHOIX ; exclus : événementielle, inactive, suspendu, déjà servi")
    void modesEtExclusions() throws Exception {
      Tontine tirage = Fixtures.tontine(gestionnaire);
      tirage.setOrdreBeneficiaire("ALEATOIRE");
      tirage.setMontant(null);
      Tontine choix = Fixtures.tontine(gestionnaire);
      choix.setOrdreBeneficiaire("MANUEL");
      Tontine evt = Fixtures.tontine(gestionnaire);
      evt.setTontineType(TontineType.EVENEMENTIELLE);
      Tontine suspendue = Fixtures.tontine(gestionnaire);
      suspendue.setStatut(TontineStatut.SUSPENDUE);
      TontineMembre suspendu = Fixtures.membre(Fixtures.tontine(gestionnaire), moi, 1);
      suspendu.setStatut(MembreStatut.SUSPENDU);
      TontineMembre servi = Fixtures.membre(Fixtures.tontine(gestionnaire), moi, 1);
      servi.setARecuJackpot(true);
      mesAdhesions(
          Fixtures.membre(tirage, moi, 1),
          Fixtures.membre(choix, moi, 1),
          Fixtures.membre(evt, moi, 1),
          Fixtures.membre(suspendue, moi, 1),
          suspendu,
          servi);
      when(gagnantRepository.findRecusByUserId(moi.getId())).thenReturn(List.of());

      List<MesJackpotsResponse.JackpotAVenir> r = service.getJackpots(kc).getAVenir();

      assertThat(r)
          .extracting(MesJackpotsResponse.JackpotAVenir::getModeAttribution)
          .containsExactly("TIRAGE", "CHOIX");
      assertThat(r.get(0).getMontantEstime()).isNull();
      assertThat(r.get(0).getPosition()).isNull();
    }
  }

  @Nested
  @DisplayName("fil d'activité")
  class Activite {

    private ActiviteRepository.ActiviteRow ligne(String type, String prenom, String nom) {
      ActiviteRepository.ActiviteRow row = mock(ActiviteRepository.ActiviteRow.class);
      lenient().when(row.getId()).thenReturn(UUID.randomUUID());
      lenient().when(row.getType()).thenReturn(type);
      lenient().when(row.getDateEvenement()).thenReturn(LocalDateTime.of(2026, 3, 1, 10, 0));
      lenient().when(row.getMontant()).thenReturn(new BigDecimal("1000"));
      lenient().when(row.getSens()).thenReturn("SORTANT");
      lenient().when(row.getStatut()).thenReturn("VALIDE");
      lenient().when(row.getTontineNom()).thenReturn("Tontine test");
      lenient().when(row.getNumeroCycle()).thenReturn(2);
      lenient().when(row.getMembrePrenom()).thenReturn(prenom);
      lenient().when(row.getMembreNom()).thenReturn(nom);
      return row;
    }

    @Test
    @DisplayName("mode GERE et filtres transmis, tri imposé ignoré, lignes converties")
    void filtres() throws Exception {
      ActiviteRepository.ActiviteRow row = ligne("COTISATION", "Fatou", "Sow");
      when(activiteRepository.findActivite(
              eq(moi.getId()), eq(true), any(), eq(true), any(), eq("VALIDE"), any()))
          .thenReturn(new PageImpl<>(List.of(row)));

      List<ActiviteResponse> r =
          service
              .getActivite(
                  kc,
                  ModeActivite.GERE,
                  null,
                  List.of(TypeActivite.COTISATION_VALIDEE),
                  CotisationStatut.VALIDE,
                  PageRequest.of(1, 10, Sort.by("montant")))
              .getContent();

      assertThat(r)
          .singleElement()
          .satisfies(
              a -> {
                assertThat(a.getMembreNom()).isEqualTo("Fatou Sow");
                assertThat(a.getDate()).isEqualTo(LocalDateTime.of(2026, 3, 1, 10, 0));
              });
      ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
      verify(activiteRepository)
          .findActivite(any(), eq(true), any(), eq(true), any(), any(), page.capture());
      assertThat(page.getValue().getPageNumber()).isEqualTo(1);
      assertThat(page.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    @DisplayName(
        "type répétable : plusieurs types dédoublonnés transmis ; aucun type -> filtre désactivé")
    @SuppressWarnings("unchecked")
    void plusieursTypes() throws Exception {
      when(activiteRepository.findActivite(
              any(), eq(false), any(), any(Boolean.class), any(), any(), any()))
          .thenReturn(new PageImpl<>(List.of()));

      service.getActivite(
          kc,
          ModeActivite.COTISE,
          null,
          List.of(
              TypeActivite.COTISATION_VALIDEE,
              TypeActivite.COTISATION_ENCAISSEE,
              TypeActivite.COTISATION_VALIDEE),
          null,
          PageRequest.of(0, 20));
      service.getActivite(kc, ModeActivite.COTISE, null, List.of(), null, PageRequest.of(0, 20));
      service.getActivite(kc, ModeActivite.COTISE, null, null, null, PageRequest.of(0, 20));

      ArgumentCaptor<Boolean> filtrer = ArgumentCaptor.forClass(Boolean.class);
      ArgumentCaptor<java.util.Collection<String>> types =
          ArgumentCaptor.forClass(java.util.Collection.class);
      verify(activiteRepository, times(3))
          .findActivite(any(), eq(false), any(), filtrer.capture(), types.capture(), any(), any());
      assertThat(filtrer.getAllValues()).containsExactly(true, false, false);
      assertThat(types.getAllValues().get(0))
          .containsExactly("COTISATION_VALIDEE", "COTISATION_ENCAISSEE");
      // liste jamais vide (IN () invalide en SQL), filtre désactivé par le drapeau
      assertThat(types.getAllValues().get(1)).isNotEmpty();
    }

    @Test
    @DisplayName("export CSV : en-tête, lignes, nom selon le mode ; format autre que csv -> 400")
    void export() throws Exception {
      ActiviteRepository.ActiviteRow jackpot = ligne("JACKPOT", null, null);
      when(activiteRepository.findActivite(
              eq(moi.getId()), eq(false), any(), any(Boolean.class), any(), any(), any()))
          .thenReturn(new PageImpl<>(List.of(jackpot)));

      FichierExport f = service.exportActivite(kc, "csv", ModeActivite.COTISE, null, null, null);

      assertThat(f.nomFichier()).isEqualTo("activite-cotise.csv");
      String contenu = new String(f.contenu(), StandardCharsets.UTF_8);
      assertThat(contenu).contains("Date", "Montant (FCFA)", "JACKPOT", "Tontine test");
      assertThatThrownBy(
              () -> service.exportActivite(kc, "pdf", ModeActivite.COTISE, null, null, null))
          .isInstanceOfSatisfying(
              BadRequestException.class,
              e -> assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_PARAMETER));
    }
  }

  @Nested
  @DisplayName("validation groupée")
  class ValidationGroupee {

    private ValidationGroupeeRequest req(UUID... ids) {
      ValidationGroupeeRequest r = new ValidationGroupeeRequest();
      r.setCotisationIds(List.of(ids));
      return r;
    }

    @Test
    @DisplayName(
        "résultat par id (dédoublonné) : succès, code métier, code déduit du statut, erreur interne")
    void resultats() throws Exception {
      Tontine t = Fixtures.tontine(moi);
      TontineMembre m = Fixtures.membre(t, Fixtures.user("Fatou"), 1);
      CycleTontine c = Fixtures.cycle(t, 1, CycleStatut.EN_COURS);
      Cotisation ok = Fixtures.cotisation(m, c, CotisationStatut.EN_ATTENTE);
      Cotisation dejaValidee = Fixtures.cotisation(m, c, CotisationStatut.VALIDE);
      Cotisation interdite = Fixtures.cotisation(m, c, CotisationStatut.EN_ATTENTE);
      Cotisation plantage = Fixtures.cotisation(m, c, CotisationStatut.EN_ATTENTE);
      UUID inconnue = UUID.randomUUID();
      for (Cotisation co : List.of(ok, dejaValidee, interdite, plantage)) {
        when(cotisationRepository.findByIdAndDeletedAtIsNull(co.getId()))
            .thenReturn(Optional.of(co));
      }
      when(cotisationRepository.findByIdAndDeletedAtIsNull(inconnue)).thenReturn(Optional.empty());
      when(cotisationService.validateCotisation(kc, t.getId(), ok.getId()))
          .thenReturn(
              CotisationResponse.builder().id(ok.getId()).statut(CotisationStatut.VALIDE).build());
      when(cotisationService.validateCotisation(kc, t.getId(), dejaValidee.getId()))
          .thenThrow(new BadRequestException("déjà", ErrorCode.COTISATION_NOT_PENDING));
      when(cotisationService.validateCotisation(kc, t.getId(), interdite.getId()))
          .thenThrow(new ForbiddenException("pas à vous"));
      when(cotisationService.validateCotisation(kc, t.getId(), plantage.getId()))
          .thenThrow(new IllegalStateException("boum"));

      ValidationGroupeeResponse r =
          service.validerCotisations(
              kc,
              req(
                  ok.getId(),
                  ok.getId(),
                  dejaValidee.getId(),
                  interdite.getId(),
                  plantage.getId(),
                  inconnue));

      assertThat(r.getTotal()).isEqualTo(5);
      assertThat(r.getValidees()).isEqualTo(1);
      assertThat(r.getEchecs()).isEqualTo(4);
      assertThat(r.getResultats())
          .extracting(ValidationGroupeeResponse.Resultat::getCode)
          .containsExactly(
              null,
              ErrorCode.COTISATION_NOT_PENDING,
              ErrorCode.fromStatus(HttpStatus.FORBIDDEN),
              ErrorCode.INTERNAL_ERROR,
              ErrorCode.fromStatus(HttpStatus.NOT_FOUND));
      assertThat(r.getResultats().get(0).getStatut()).isEqualTo(CotisationStatut.VALIDE);
      verify(cotisationService, times(1)).validateCotisation(kc, t.getId(), ok.getId());
    }

    @Test
    @DisplayName("utilisateur inconnu -> 404 avant tout traitement")
    void inconnu() {
      when(userRepository.findByKeycloakId("x")).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.validerCotisations("x", req(UUID.randomUUID())))
          .isInstanceOf(CustomException.class);
    }
  }
}
