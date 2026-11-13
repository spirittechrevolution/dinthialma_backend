package com.africa.dinthialma_backend.me.service.impl;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.util.CsvExport;
import com.africa.dinthialma_backend.common.util.CsvWriter;
import com.africa.dinthialma_backend.common.util.FichierExport;
import com.africa.dinthialma_backend.common.util.Fuseau;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.codeList.Ponctualite;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.contribution.mapper.CotisationMapper;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.contribution.repository.CotisationSpecifications;
import com.africa.dinthialma_backend.contribution.service.interfaces.CotisationService;
import com.africa.dinthialma_backend.jackpot.service.interfaces.PrioriteJackpotService;
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
import com.africa.dinthialma_backend.me.service.interfaces.MeService;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.tontine.calendrier.Rythme;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.DistributionEvenementielle;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleGagnantRepository;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.DistributionEvenementielleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeServiceImpl implements MeService {

  private final UserRepository userRepository;
  private final CotisationMapper cotisationMapper;
  private final ActiviteMapper activiteMapper;
  private final CotisationRepository cotisationRepository;
  private final TontineMembreRepository membreRepository;
  private final CycleTontineRepository cycleRepository;
  private final CycleGagnantRepository gagnantRepository;
  private final DistributionEvenementielleRepository distributionRepository;
  private final CotisationService cotisationService;
  private final ActiviteRepository activiteRepository;
  private final PrioriteJackpotService prioriteService;

  // ─── Résumé « Je cotise » ────────────────────────────────────────────────

  @Override
  public MeSummaryResponse getSummary(String keycloakId) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);

    List<TontineMembre> memberships =
        membreRepository
            .findByUser_IdAndDeletedAtIsNullAndTontine_DeletedAtIsNullOrderByCreatedAtDesc(
                caller.getId());
    BigDecimal totalJackpots =
        zeroSiNul(gagnantRepository.sumMontantRecuByUserId(caller.getId()))
            .add(zeroSiNul(distributionRepository.sumMontantNetByUserId(caller.getId())));

    if (memberships.isEmpty()) {
      return MeSummaryResponse.builder()
          .totalCotiseValide(BigDecimal.ZERO)
          .totalEnAttente(BigDecimal.ZERO)
          .totalJackpotsRecus(totalJackpots)
          .nombreCotisationsDues(0)
          .montantDu(BigDecimal.ZERO)
          .tontines(List.of())
          .build();
    }

    AgregatsSummary agregats = chargerAgregats(memberships);
    List<MeSummaryResponse.TontineCotisant> lignes =
        memberships.stream().map(m -> ligneTontine(m, agregats)).toList();

    BigDecimal totalValide = BigDecimal.ZERO;
    BigDecimal totalEnAttente = BigDecimal.ZERO;
    long nombreDues = 0;
    BigDecimal montantDu = BigDecimal.ZERO;
    for (MeSummaryResponse.TontineCotisant ligne : lignes) {
      totalValide = totalValide.add(ligne.getTotalCotiseValide());
      totalEnAttente = totalEnAttente.add(ligne.getTotalEnAttente());
      MeSummaryResponse.CotisationDue due = ligne.getCotisationDue();
      if (due != null && due.getStatut() == StatutCotisationDue.A_PAYER) {
        nombreDues++;
        montantDu = montantDu.add(zeroSiNul(due.getMontant()));
      }
    }

    return MeSummaryResponse.builder()
        .totalCotiseValide(totalValide)
        .totalEnAttente(totalEnAttente)
        .totalJackpotsRecus(totalJackpots)
        .nombreCotisationsDues(nombreDues)
        .montantDu(montantDu)
        .tontines(lignes)
        .build();
  }

  /**
   * Agrégats du résumé, chargés en un nombre fixe de requêtes quel que soit le nombre de tontines.
   */
  private record AgregatsSummary(
      Map<UUID, Map<CotisationStatut, Object[]>> statsParMembre,
      Map<UUID, Object[]> jackpotsParMembre,
      Map<UUID, Long> toursParTontine,
      Map<UUID, CycleTontine> cycleEnCoursParTontine,
      Map<UUID, Cotisation> cotisationEnCoursParMembre) {}

  private AgregatsSummary chargerAgregats(List<TontineMembre> memberships) {
    List<UUID> membreIds = memberships.stream().map(TontineMembre::getId).toList();
    List<UUID> tontineIds = memberships.stream().map(m -> m.getTontine().getId()).toList();

    Map<UUID, Map<CotisationStatut, Object[]>> statsParMembre = new HashMap<>();
    for (Object[] row : cotisationRepository.countAndSumByMembreAndStatut(membreIds)) {
      statsParMembre
          .computeIfAbsent((UUID) row[0], k -> new EnumMap<>(CotisationStatut.class))
          .put((CotisationStatut) row[1], row);
    }

    Map<UUID, Long> toursParTontine = new HashMap<>();
    for (Object[] row : cycleRepository.countByTontineIds(tontineIds)) {
      toursParTontine.put((UUID) row[0], (Long) row[1]);
    }

    Map<UUID, CycleTontine> cycleEnCoursParTontine =
        cycleRepository
            .findByTontine_IdInAndStatutAndDeletedAtIsNull(tontineIds, CycleStatut.EN_COURS)
            .stream()
            .collect(Collectors.toMap(c -> c.getTontine().getId(), c -> c, (a, b) -> a));

    return new AgregatsSummary(
        statsParMembre,
        jackpotsParMembre(membreIds),
        toursParTontine,
        cycleEnCoursParTontine,
        cotisationsEnCours(cycleEnCoursParTontine.values(), membreIds));
  }

  /** [membreId, montant, nombre] : jackpots ROTATIVE + distribution finale EVENEMENTIELLE. */
  private Map<UUID, Object[]> jackpotsParMembre(List<UUID> membreIds) {
    Map<UUID, Object[]> jackpots = new HashMap<>();
    for (Object[] row : gagnantRepository.sumAndCountByMembreIds(membreIds)) {
      jackpots.put((UUID) row[0], row);
    }
    for (Object[] row : distributionRepository.sumAndCountByMembreIds(membreIds)) {
      jackpots.merge(
          (UUID) row[0],
          row,
          (a, b) ->
              new Object[] {
                a[0], ((BigDecimal) a[1]).add((BigDecimal) b[1]), (Long) a[2] + (Long) b[2]
              });
    }
    return jackpots;
  }

  private Map<UUID, Cotisation> cotisationsEnCours(
      Collection<CycleTontine> cyclesEnCours, List<UUID> membreIds) {
    if (cyclesEnCours.isEmpty()) {
      return Map.of();
    }
    List<UUID> cycleIds = cyclesEnCours.stream().map(CycleTontine::getId).toList();
    return cotisationRepository
        .findByCycle_IdInAndMembre_IdInAndDeletedAtIsNull(cycleIds, membreIds)
        .stream()
        .collect(Collectors.toMap(c -> c.getMembre().getId(), c -> c, Cotisation::plusPertinente));
  }

  private MeSummaryResponse.TontineCotisant ligneTontine(
      TontineMembre m, AgregatsSummary agregats) {
    Tontine t = m.getTontine();
    Map<CotisationStatut, Object[]> stats =
        agregats.statsParMembre().getOrDefault(m.getId(), Map.of());
    Object[] jackpots = agregats.jackpotsParMembre().get(m.getId());
    CycleTontine cycle = agregats.cycleEnCoursParTontine().get(t.getId());
    return MeSummaryResponse.TontineCotisant.builder()
        .tontineId(t.getId())
        .nom(t.getNom())
        .tontineType(t.getTontineType())
        .statut(t.getStatut())
        .frequence(t.getFrequence())
        .rythmeIntervalle(t.getRythme().intervalle())
        .rythmeUnite(t.getRythme().unite())
        .jourEcheance(t.getRythme().jourEcheance())
        .heureEcheance(Rythme.formaterHeure(t.getRythme().heureEcheance()))
        .nomEvenement(t.getNomEvenement())
        .dateEcheance(t.getDateEcheance())
        .monMembreId(m.getId())
        .monStatutMembre(m.getStatut())
        .ordreJackpot(m.getOrdreJackpot())
        .aRecuJackpot(m.isARecuJackpot())
        .totalCotiseValide(montant(stats.get(CotisationStatut.VALIDE)))
        .totalEnAttente(montant(stats.get(CotisationStatut.EN_ATTENTE)))
        .totalJackpotsRecus(jackpots != null ? (BigDecimal) jackpots[1] : BigDecimal.ZERO)
        .nombreJackpotsRecus(jackpots != null ? (Long) jackpots[2] : 0L)
        .toursPayes(nombre(stats.get(CotisationStatut.VALIDE)))
        .toursTotal(agregats.toursParTontine().getOrDefault(t.getId(), 0L))
        .cycleEnCours(cycleEnCours(cycle))
        .cotisationDue(
            buildCotisationDue(t, m, cycle, agregats.cotisationEnCoursParMembre().get(m.getId())))
        .gestionnaire(gestionnaire(t.getCreePar()))
        .build();
  }

  private static MeSummaryResponse.CycleEnCours cycleEnCours(CycleTontine cycle) {
    if (cycle == null) {
      return null;
    }
    return MeSummaryResponse.CycleEnCours.builder()
        .cycleId(cycle.getId())
        .numeroCycle(cycle.getNumeroCycle())
        .dateDebut(cycle.getDateDebut())
        .dateFin(cycle.getDateFin())
        .echeanceAt(Fuseau.versOffset(cycle.getEcheanceAt()))
        .build();
  }

  private static MeSummaryResponse.Gestionnaire gestionnaire(User gestionnaire) {
    if (gestionnaire == null) {
      return null;
    }
    return MeSummaryResponse.Gestionnaire.builder()
        .id(gestionnaire.getId())
        .firstName(gestionnaire.getFirstName())
        .lastName(gestionnaire.getLastName())
        .phone(gestionnaire.getPhone())
        .build();
  }

  /**
   * Cotisation due sur le tour en cours. Null si pas de tour en cours, tontine non ACTIVE ou
   * membership non ACTIF (aucun paiement attendu).
   */
  private MeSummaryResponse.CotisationDue buildCotisationDue(
      Tontine tontine, TontineMembre membre, CycleTontine cycle, Cotisation cotisation) {
    if (cycle == null
        || tontine.getStatut() != TontineStatut.ACTIVE
        || membre.getStatut() != MembreStatut.ACTIF) {
      return null;
    }

    // Seul un refus sur ce tour : le membre doit déclarer à nouveau
    String motifRefus = null;
    if (cotisation != null && cotisation.getStatut() == CotisationStatut.REFUSE) {
      motifRefus = cotisation.getMotifRefus();
      cotisation = null;
    }

    StatutCotisationDue statut = StatutCotisationDue.A_PAYER;
    if (cotisation != null) {
      statut =
          cotisation.getStatut() == CotisationStatut.VALIDE
              ? StatutCotisationDue.VALIDE
              : StatutCotisationDue.EN_ATTENTE;
    }

    return MeSummaryResponse.CotisationDue.builder()
        .montant(tontine.getMontantAttenduParMembre())
        .montantLibre(tontine.isMontantLibre())
        .montantMinimum(tontine.getMontantMinimum())
        .dateLimite(cycle.getDateFin())
        .statut(statut)
        .cotisationId(cotisation != null ? cotisation.getId() : null)
        .ponctualite(Ponctualite.de(cotisation))
        .motifDernierRefus(motifRefus)
        .build();
  }

  private static BigDecimal zeroSiNul(BigDecimal valeur) {
    return valeur != null ? valeur : BigDecimal.ZERO;
  }

  private static BigDecimal montant(Object[] row) {
    return row != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
  }

  private static long nombre(Object[] row) {
    return row != null ? (Long) row[2] : 0L;
  }

  // ─── File « À valider » ──────────────────────────────────────────────────

  @Override
  public Page<CotisationResponse> listCotisationsGerees(
      String keycloakId,
      List<CotisationStatut> statuts,
      UUID tontineId,
      String q,
      Pageable pageable)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);

    Specification<Cotisation> spec =
        Specification.allOf(
            CotisationSpecifications.nonSupprimee(),
            CotisationSpecifications.deTontinesGereesPar(caller.getId()),
            CotisationSpecifications.deTontine(tontineId),
            CotisationSpecifications.statutParmi(statuts),
            CotisationSpecifications.recherche(q));

    return cotisationRepository.findAll(spec, pageable).map(cotisationMapper::toResponse);
  }

  // ─── Mes jackpots ────────────────────────────────────────────────────────

  @Override
  public MesJackpotsResponse getJackpots(String keycloakId) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);

    List<MesJackpotsResponse.JackpotRecu> recus =
        gagnantRepository.findRecusByUserId(caller.getId()).stream()
            .map(
                g -> {
                  CycleTontine c = g.getCycle();
                  return MesJackpotsResponse.JackpotRecu.builder()
                      .tontineId(c.getTontine().getId())
                      .tontineNom(c.getTontine().getNom())
                      .cycleId(c.getId())
                      .numeroCycle(c.getNumeroCycle())
                      .dateRemise(c.getDateRemise() != null ? c.getDateRemise() : c.getDateFin())
                      .montantRecu(g.getMontantRecu())
                      .nature("JACKPOT")
                      .build();
                })
            .collect(Collectors.toCollection(ArrayList::new));
    for (DistributionEvenementielle d : distributionRepository.findRecuesByUserId(caller.getId())) {
      CycleTontine c = d.getCycle();
      recus.add(
          MesJackpotsResponse.JackpotRecu.builder()
              .tontineId(d.getTontine().getId())
              .tontineNom(d.getTontine().getNom())
              .cycleId(c.getId())
              .numeroCycle(c.getNumeroCycle())
              .dateRemise(c.getDateRemise() != null ? c.getDateRemise() : c.getDateFin())
              .montantRecu(d.getMontantNet())
              .nature("DISTRIBUTION_FINALE")
              .build());
    }
    recus.sort(
        Comparator.comparing(
            MesJackpotsResponse.JackpotRecu::getDateRemise,
            Comparator.nullsLast(Comparator.reverseOrder())));
    BigDecimal totalRecu =
        recus.stream()
            .map(MesJackpotsResponse.JackpotRecu::getMontantRecu)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    List<MesJackpotsResponse.JackpotAVenir> aVenir = new ArrayList<>();
    for (TontineMembre m :
        membreRepository
            .findByUser_IdAndDeletedAtIsNullAndTontine_DeletedAtIsNullOrderByCreatedAtDesc(
                caller.getId())) {
      Tontine t = m.getTontine();
      if (t.getTontineType() != TontineType.ROTATIVE
          || t.getStatut() != TontineStatut.ACTIVE
          || m.getStatut() != MembreStatut.ACTIF
          || m.isARecuJackpot()) {
        continue;
      }
      aVenir.add(estimerProchainJackpot(t, m));
    }

    return MesJackpotsResponse.builder().totalRecu(totalRecu).recus(recus).aVenir(aVenir).build();
  }

  /**
   * Estimation alignée sur la désignation à la clôture (CycleServiceImpl.resolveGagnants) : en
   * ROTATION, les {@code nombreGagnants} premiers membres ACTIFS non gagnants par ordreJackpot
   * (puis date d'adhésion) gagnent le tour clôturé, après les priorités accordées. ALEATOIRE,
   * MANUEL et HYBRIDE : pas d'estimation.
   */
  private MesJackpotsResponse.JackpotAVenir estimerProchainJackpot(Tontine t, TontineMembre moi) {
    List<TontineMembre> actifs =
        membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(t.getId(), MembreStatut.ACTIF);
    int gagnantsParTour = Math.max(1, t.getNombreGagnants());
    BigDecimal montantEstime =
        t.getMontant() == null
            ? null
            : t.getMontant()
                .multiply(BigDecimal.valueOf(actifs.size()))
                .divide(BigDecimal.valueOf(gagnantsParTour), 0, RoundingMode.DOWN);

    MesJackpotsResponse.JackpotAVenir.JackpotAVenirBuilder ligne =
        MesJackpotsResponse.JackpotAVenir.builder()
            .tontineId(t.getId())
            .tontineNom(t.getNom())
            .monMembreId(moi.getId())
            .montantEstime(montantEstime);

    if (!"ROTATION".equals(t.getOrdreBeneficiaire())) {
      return ligne
          .modeAttribution(
              switch (t.getOrdreBeneficiaire() == null ? "" : t.getOrdreBeneficiaire()) {
                case "ALEATOIRE" -> "TIRAGE";
                case "HYBRIDE" -> "HYBRIDE";
                default -> "CHOIX";
              })
          .build();
    }

    // Priorités accordées d'abord (elles passent avant l'ordre convenu), puis l'ordre de passage
    List<TontineMembre> eligibles =
        new ArrayList<>(prioriteService.prioritairesEligibles(t.getId()));
    Set<UUID> prioritaires =
        eligibles.stream().map(TontineMembre::getId).collect(Collectors.toSet());
    actifs.stream()
        .filter(x -> !x.isARecuJackpot() && !prioritaires.contains(x.getId()))
        .sorted(
            Comparator.comparingInt(
                    (TontineMembre x) ->
                        x.getOrdreJackpot() != null ? x.getOrdreJackpot() : Integer.MAX_VALUE)
                .thenComparing(TontineMembre::getCreatedAt))
        .forEach(eligibles::add);
    int rang = 1;
    for (TontineMembre x : eligibles) {
      if (x.getId().equals(moi.getId())) break;
      rang++;
    }
    int toursAvantMoi = (rang - 1) / gagnantsParTour; // 0 = le tour en cours (ou le prochain)

    // Tour de référence : le tour EN_COURS (ses gagnants sont désignés à sa clôture), sinon le
    // prochain tour à ouvrir.
    List<CycleTontine> cycles =
        cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(t.getId());
    int numeroReference =
        cycles.stream()
            .filter(c -> c.getStatut() == CycleStatut.EN_COURS)
            .map(CycleTontine::getNumeroCycle)
            .findFirst()
            .orElseGet(
                () -> cycles.stream().mapToInt(CycleTontine::getNumeroCycle).max().orElse(0) + 1);
    int numeroEstime = numeroReference + toursAvantMoi;
    LocalDate dateEstimee =
        cycles.stream()
            .filter(c -> c.getNumeroCycle() == numeroEstime)
            .map(CycleTontine::getDateFin)
            .findFirst()
            .orElse(null);

    return ligne
        .modeAttribution("ORDRE")
        .position(rang)
        .numeroCycleEstime(numeroEstime)
        .dateEstimee(dateEstimee)
        .build();
  }

  // ─── Fil d'activité ──────────────────────────────────────────────────────

  @Override
  public Page<ActiviteResponse> getActivite(
      String keycloakId,
      ModeActivite mode,
      UUID tontineId,
      List<TypeActivite> types,
      CotisationStatut statut,
      Pageable pageable)
      throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    // Tri imposé par la requête (date décroissante) : on ne transmet que page et taille.
    Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    return activiteRepository
        .findActivite(
            caller.getId(),
            mode == ModeActivite.GERE,
            tontineId,
            types != null && !types.isEmpty(),
            types == null || types.isEmpty()
                ? List.of("-")
                : types.stream().map(TypeActivite::name).distinct().toList(),
            statut != null ? statut.name() : null,
            page)
        .map(activiteMapper::toResponse);
  }

  @Override
  public FichierExport exportActivite(
      String keycloakId,
      String format,
      ModeActivite mode,
      UUID tontineId,
      List<TypeActivite> types,
      CotisationStatut statut)
      throws CustomException {
    CsvExport.verifierFormat(format);
    Page<ActiviteResponse> lignes =
        getActivite(
            keycloakId, mode, tontineId, types, statut, PageRequest.of(0, CsvWriter.MAX_LIGNES));
    CsvWriter csv =
        new CsvWriter()
            .ligne("Date", "Type", "Tontine", "Tour", "Membre", "Montant (FCFA)", "Sens", "Statut");
    for (ActiviteResponse a : lignes.getContent()) {
      csv.ligne(
          a.getDate(),
          a.getType(),
          a.getTontineNom(),
          a.getNumeroCycle(),
          a.getMembreNom(),
          a.getMontant() != null ? a.getMontant().toPlainString() : null,
          a.getSens(),
          a.getStatut());
    }
    return FichierExport.csv(
        "activite-" + mode.name().toLowerCase(Locale.ROOT) + ".csv", csv.toBytes());
  }

  // ─── Validation groupée ──────────────────────────────────────────────────

  /**
   * Pas de transaction englobante : chaque validation s'exécute dans sa propre transaction (via le
   * proxy de {@link CotisationService}), si bien qu'un échec n'annule pas les autres.
   */
  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public ValidationGroupeeResponse validerCotisations(
      String keycloakId, ValidationGroupeeRequest request) throws CustomException {

    findUserByKeycloakId(keycloakId);
    List<UUID> ids = new ArrayList<>(new LinkedHashSet<>(request.getCotisationIds()));
    List<ValidationGroupeeResponse.Resultat> resultats = new ArrayList<>();

    for (UUID id : ids) {
      try {
        Cotisation cotisation =
            cotisationRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(
                    () -> new NotFoundException(ResponseMessageConstants.CONTRIBUTION_NOT_FOUND));
        CotisationResponse validee =
            cotisationService.validateCotisation(keycloakId, cotisation.getTontine().getId(), id);
        resultats.add(
            ValidationGroupeeResponse.Resultat.builder()
                .cotisationId(id)
                .succes(true)
                .statut(validee.getStatut())
                .build());
      } catch (CustomException e) {
        resultats.add(
            echec(
                id,
                e.getCode() != null ? e.getCode() : ErrorCode.fromStatus(e.getStatus()),
                e.getMessage()));
      } catch (Exception e) {
        log.error("Validation groupée : échec inattendu pour cotisationId={}", id, e);
        resultats.add(echec(id, ErrorCode.INTERNAL_ERROR, "Erreur interne du serveur"));
      }
    }

    int validees =
        (int) resultats.stream().filter(ValidationGroupeeResponse.Resultat::isSucces).count();
    log.info("Validation groupée – {} / {} cotisations validées", validees, ids.size());
    return ValidationGroupeeResponse.builder()
        .total(ids.size())
        .validees(validees)
        .echecs(ids.size() - validees)
        .resultats(resultats)
        .build();
  }

  private static ValidationGroupeeResponse.Resultat echec(UUID id, ErrorCode code, String message) {
    return ValidationGroupeeResponse.Resultat.builder()
        .cotisationId(id)
        .succes(false)
        .code(code)
        .message(message)
        .build();
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  private User findUserByKeycloakId(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }
}
