package com.africa.dinthialma_backend.me.service.impl;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.contribution.repository.CotisationSpecifications;
import com.africa.dinthialma_backend.me.codeList.StatutCotisationDue;
import com.africa.dinthialma_backend.me.dto.MeSummaryResponse;
import com.africa.dinthialma_backend.me.service.interfaces.MeService;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleGagnantRepository;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeServiceImpl implements MeService {

  private final UserRepository userRepository;
  private final CotisationRepository cotisationRepository;
  private final TontineMembreRepository membreRepository;
  private final CycleTontineRepository cycleRepository;
  private final CycleGagnantRepository gagnantRepository;

  // ─── Résumé « Je cotise » ────────────────────────────────────────────────

  @Override
  public MeSummaryResponse getSummary(String keycloakId) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);

    List<TontineMembre> memberships =
        membreRepository
            .findByUser_IdAndDeletedAtIsNullAndTontine_DeletedAtIsNullOrderByCreatedAtDesc(
                caller.getId());
    BigDecimal totalJackpots = gagnantRepository.sumMontantRecuByUserId(caller.getId());

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

    List<UUID> membreIds = memberships.stream().map(TontineMembre::getId).toList();
    List<UUID> tontineIds = memberships.stream().map(m -> m.getTontine().getId()).toList();

    // Agrégats en 4 requêtes, quel que soit le nombre de tontines
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

    List<UUID> cycleEnCoursIds =
        cycleEnCoursParTontine.values().stream().map(CycleTontine::getId).toList();
    Map<UUID, Cotisation> cotisationEnCoursParMembre =
        cycleEnCoursIds.isEmpty()
            ? Map.of()
            : cotisationRepository
                .findByCycle_IdInAndMembre_IdInAndDeletedAtIsNull(cycleEnCoursIds, membreIds)
                .stream()
                .collect(Collectors.toMap(c -> c.getMembre().getId(), c -> c, (a, b) -> a));

    BigDecimal totalValide = BigDecimal.ZERO;
    BigDecimal totalEnAttente = BigDecimal.ZERO;
    long nombreDues = 0;
    BigDecimal montantDu = BigDecimal.ZERO;
    List<MeSummaryResponse.TontineCotisant> lignes = new ArrayList<>();

    for (TontineMembre m : memberships) {
      Tontine t = m.getTontine();
      Map<CotisationStatut, Object[]> stats = statsParMembre.getOrDefault(m.getId(), Map.of());
      BigDecimal valide = montant(stats.get(CotisationStatut.VALIDE));
      BigDecimal enAttente = montant(stats.get(CotisationStatut.EN_ATTENTE));
      totalValide = totalValide.add(valide);
      totalEnAttente = totalEnAttente.add(enAttente);

      CycleTontine cycle = cycleEnCoursParTontine.get(t.getId());
      MeSummaryResponse.CotisationDue due =
          buildCotisationDue(t, m, cycle, cotisationEnCoursParMembre.get(m.getId()));
      if (due != null && due.getStatut() == StatutCotisationDue.A_PAYER) {
        nombreDues++;
        if (due.getMontant() != null) montantDu = montantDu.add(due.getMontant());
      }

      User gestionnaire = t.getCreePar();
      lignes.add(
          MeSummaryResponse.TontineCotisant.builder()
              .tontineId(t.getId())
              .nom(t.getNom())
              .tontineType(t.getTontineType())
              .statut(t.getStatut())
              .frequence(t.getFrequence())
              .nomEvenement(t.getNomEvenement())
              .dateEcheance(t.getDateEcheance())
              .monMembreId(m.getId())
              .monStatutMembre(m.getStatut())
              .ordreJackpot(m.getOrdreJackpot())
              .aRecuJackpot(m.isARecuJackpot())
              .totalCotiseValide(valide)
              .totalEnAttente(enAttente)
              .toursPayes(nombre(stats.get(CotisationStatut.VALIDE)))
              .toursTotal(toursParTontine.getOrDefault(t.getId(), 0L))
              .cycleEnCours(
                  cycle == null
                      ? null
                      : MeSummaryResponse.CycleEnCours.builder()
                          .cycleId(cycle.getId())
                          .numeroCycle(cycle.getNumeroCycle())
                          .dateDebut(cycle.getDateDebut())
                          .dateFin(cycle.getDateFin())
                          .build())
              .cotisationDue(due)
              .gestionnaire(
                  gestionnaire == null
                      ? null
                      : MeSummaryResponse.Gestionnaire.builder()
                          .id(gestionnaire.getId())
                          .firstName(gestionnaire.getFirstName())
                          .lastName(gestionnaire.getLastName())
                          .phone(gestionnaire.getPhone())
                          .build())
              .build());
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
        .build();
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

    return cotisationRepository.findAll(spec, pageable).map(CotisationResponse::from);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  private User findUserByKeycloakId(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }
}
