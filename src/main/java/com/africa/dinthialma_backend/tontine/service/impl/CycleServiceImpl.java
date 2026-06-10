package com.africa.dinthialma_backend.tontine.service.impl;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
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
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.dto.BeneficiaireHistoriqueResponse;
import com.africa.dinthialma_backend.tontine.dto.CycleResponse;
import com.africa.dinthialma_backend.tontine.dto.CycleResponse.MembreDistributionInfo;
import com.africa.dinthialma_backend.tontine.dto.OpenCycleRequest;
import com.africa.dinthialma_backend.tontine.dto.SelectionnerBeneficiaireRequest;
import com.africa.dinthialma_backend.tontine.entity.CycleGagnant;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.entity.TontineCommission;
import com.africa.dinthialma_backend.tontine.repository.CycleGagnantRepository;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineCommissionRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import com.africa.dinthialma_backend.tontine.service.interfaces.CycleService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CycleServiceImpl implements CycleService {

  private final CycleTontineRepository cycleRepository;
  private final CycleGagnantRepository cycleGagnantRepository;
  private final TontineRepository tontineRepository;
  private final TontineMembreRepository membreRepository;
  private final CotisationRepository cotisationRepository;
  private final TontineCommissionRepository commissionRepository;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository roleAssignmentRepository;
  private final AuditService auditService;
  private final SchedulerService schedulerService;
  private final WhatsappService whatsappService;
  private final NotificationService notificationService;

  // ─── Lecture ─────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<CycleResponse> listCycles(String keycloakId, UUID tontineId, Pageable pageable)
      throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertCanAccess(caller, tontine);

    return cycleRepository
        .findByTontine_IdAndDeletedAtIsNull(tontineId, pageable)
        .map(CycleResponse::from);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BeneficiaireHistoriqueResponse> listBeneficiaires(
      String keycloakId, UUID tontineId, Pageable pageable) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertCanAccess(caller, tontine);

    return cycleRepository
        .findTerminesAvecGagnants(tontineId, CycleStatut.TERMINE, pageable)
        .map(BeneficiaireHistoriqueResponse::from);
  }

  @Override
  @Transactional(readOnly = true)
  public CycleResponse getCycle(String keycloakId, UUID tontineId, UUID cycleId)
      throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertCanAccess(caller, tontine);

    CycleTontine cycle = findCycleById(tontineId, cycleId);
    return CycleResponse.from(cycle);
  }

  // ─── Ouverture manuelle ──────────────────────────────────────────────────

  @Override
  public CycleResponse openCycle(String keycloakId, UUID tontineId, OpenCycleRequest request)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    if (tontine.getTontineType() == TontineType.EVENEMENTIELLE) {
      throw new BadRequestException(
          "Une tontine événementielle gère ses cycles automatiquement – l'ouverture manuelle n'est pas disponible");
    }
    if (tontine.getModeCycle() != ModeCycle.MANUEL) {
      throw new BadRequestException("L'ouverture manuelle n'est possible qu'en mode MANUEL");
    }
    if (tontine.getStatut() != TontineStatut.ACTIVE) {
      throw new BadRequestException("La tontine doit être ACTIVE pour ouvrir un cycle");
    }
    if (request.getDateFin().isBefore(request.getDateDebut())) {
      throw new BadRequestException("La date de fin doit être postérieure à la date de début");
    }

    if (cycleRepository
        .findByTontine_IdAndStatutAndDeletedAtIsNull(tontineId, CycleStatut.EN_COURS)
        .isPresent()) {
      throw new BadRequestException(
          "Un cycle est déjà en cours. Clôturez-le avant d'en ouvrir un nouveau");
    }

    int nextNum =
        cycleRepository
            .findTopByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleDesc(tontineId)
            .map(c -> c.getNumeroCycle() + 1)
            .orElse(1);

    CycleTontine cycle =
        CycleTontine.builder()
            .tontine(tontine)
            .numeroCycle(nextNum)
            .dateDebut(request.getDateDebut())
            .dateFin(request.getDateFin())
            .statut(CycleStatut.EN_COURS)
            .build();

    CycleTontine saved = cycleRepository.save(cycle);
    auditService.logCreate(caller, "cycles_tontine", saved.getId());
    log.info(
        "Cycle manuel ouvert – tontineId={} cycleId={} num={}", tontineId, saved.getId(), nextNum);

    // Notification in-app à tous les membres actifs
    membreRepository
        .findByTontine_IdAndStatutAndDeletedAtIsNull(tontineId, MembreStatut.ACTIF)
        .forEach(
            m ->
                notificationService.notify(
                    m.getUser().getId(),
                    NotificationType.CYCLE_OUVERT,
                    "Cycle #" + nextNum + " ouvert",
                    "Le cycle #"
                        + nextNum
                        + " de la tontine "
                        + tontine.getNom()
                        + " est ouvert. Vous pouvez enregistrer votre cotisation.",
                    tontineId));

    return CycleResponse.from(saved);
  }

  // ─── Clôture ─────────────────────────────────────────────────────────────

  @Override
  public CycleResponse closeCycle(String keycloakId, UUID tontineId, UUID cycleId)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    CycleTontine cycle = findCycleById(tontineId, cycleId);

    if (cycle.getStatut() != CycleStatut.EN_COURS) {
      throw new BadRequestException("Seul un cycle EN_COURS peut être clôturé");
    }

    if (tontine.getTontineType() == TontineType.EVENEMENTIELLE) {
      return closeCycleEvenementielle(caller, tontine, tontineId, cycle);
    }

    return closeCycleRotative(caller, tontine, tontineId, cycle);
  }

  // ─── Clôture ROTATIVE ────────────────────────────────────────────────────

  private CycleResponse closeCycleRotative(
      User caller, Tontine tontine, UUID tontineId, CycleTontine cycle) throws CustomException {

    UUID cycleId = cycle.getId();

    BigDecimal jackpot =
        cotisationRepository
            .findByCycle_IdAndStatutAndDeletedAtIsNull(cycleId, CotisationStatut.VALIDE)
            .stream()
            .map(c -> c.getMontant() != null ? c.getMontant() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    List<TontineCommission> commissions =
        commissionRepository.findByTontine_IdAndDeletedAtIsNull(tontineId);

    BigDecimal totalCommission = BigDecimal.ZERO;
    for (TontineCommission commission : commissions) {
      BigDecimal part =
          switch (commission.getType()) {
            case POURCENTAGE_JACKPOT ->
                jackpot
                    .multiply(commission.getValeur())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FRAIS_FIXES_PAR_CYCLE -> commission.getValeur();
            case FRAIS_ADHESION -> BigDecimal.ZERO;
          };
      totalCommission = totalCommission.add(part);
    }

    cycle.setStatut(CycleStatut.TERMINE);
    cycle.setMontantJackpot(jackpot);
    cycle.setMontantCommission(totalCommission);
    cycle.setMontantNet(jackpot.subtract(totalCommission));
    cycle.setDateRemise(LocalDate.now());

    cycleRepository.save(cycle);
    auditService.log(
        caller,
        "cycles_tontine",
        cycleId,
        "statut",
        CycleStatut.EN_COURS.name(),
        CycleStatut.TERMINE.name());

    List<TontineMembre> gagnantsMembres = resolveGagnants(tontine, tontineId);
    List<CycleGagnant> savedGagnants = new ArrayList<>();

    if (!gagnantsMembres.isEmpty()) {
      BigDecimal montantParGagnant =
          cycle
              .getMontantNet()
              .divide(BigDecimal.valueOf(gagnantsMembres.size()), 2, RoundingMode.HALF_UP);

      for (TontineMembre membre : gagnantsMembres) {
        CycleGagnant g =
            CycleGagnant.builder()
                .cycle(cycle)
                .membre(membre)
                .montantRecu(montantParGagnant)
                .build();
        savedGagnants.add(cycleGagnantRepository.save(g));

        membre.setARecuJackpot(true);
        membre.setDateJackpot(LocalDateTime.now());
        membreRepository.save(membre);
      }

      cycle.setGagnants(savedGagnants);
      schedulerService.annoncerGagnants(cycle, savedGagnants);

      // Notification in-app jackpot : gagnant(s) + admin
      BigDecimal netParGagnant =
          cycle
              .getMontantNet()
              .divide(BigDecimal.valueOf(savedGagnants.size()), 2, RoundingMode.HALF_UP);
      for (CycleGagnant g : savedGagnants) {
        String winnerName =
            g.getMembre().getUser().getFirstName() + " " + g.getMembre().getUser().getLastName();
        String montantStr = netParGagnant.toPlainString() + " FCFA";
        notificationService.notify(
            g.getMembre().getUser().getId(),
            NotificationType.JACKPOT_DISTRIBUE,
            "Jackpot distribué 🎉",
            "Félicitations ! Vous recevez " + montantStr + " · " + tontine.getNom(),
            tontineId);
        notificationService.notify(
            tontine.getCreePar().getId(),
            NotificationType.JACKPOT_DISTRIBUE,
            "Jackpot distribué",
            winnerName + " a reçu " + montantStr + " · " + tontine.getNom(),
            tontineId);
      }
    }

    cotisationRepository
        .findByCycle_IdAndStatutAndDeletedAtIsNull(cycleId, CotisationStatut.EN_ATTENTE)
        .forEach(
            c -> {
              c.setStatut(CotisationStatut.EN_RETARD);
              cotisationRepository.save(c);
              // Notification in-app admin — paiement en retard
              notificationService.notify(
                  tontine.getCreePar().getId(),
                  NotificationType.PAIEMENT_EN_RETARD,
                  "Paiement en retard",
                  c.getMembre().getUser().getFirstName()
                      + " "
                      + c.getMembre().getUser().getLastName()
                      + " n'a pas cotisé · "
                      + tontine.getNom(),
                  tontineId);
            });

    if (tontine.getModeCycle() == ModeCycle.AUTOMATIQUE) {
      activateNextCycle(tontineId, cycle.getNumeroCycle() + 1);
    }

    log.info(
        "Cycle ROTATIVE clôturé – tontineId={} cycleId={} jackpot={} commission={} net={}",
        tontineId,
        cycleId,
        jackpot,
        totalCommission,
        jackpot.subtract(totalCommission));
    return CycleResponse.from(cycle);
  }

  // ─── Clôture EVENEMENTIELLE ──────────────────────────────────────────────

  private CycleResponse closeCycleEvenementielle(
      User caller, Tontine tontine, UUID tontineId, CycleTontine cycle) {

    UUID cycleId = cycle.getId();

    BigDecimal jackpotCycle =
        cotisationRepository
            .findByCycle_IdAndStatutAndDeletedAtIsNull(cycleId, CotisationStatut.VALIDE)
            .stream()
            .map(c -> c.getMontant() != null ? c.getMontant() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    cycle.setStatut(CycleStatut.TERMINE);
    cycle.setMontantJackpot(jackpotCycle);
    cycle.setMontantCommission(BigDecimal.ZERO);
    cycle.setMontantNet(jackpotCycle);
    cycle.setDateRemise(LocalDate.now());

    cycleRepository.save(cycle);
    auditService.log(
        caller,
        "cycles_tontine",
        cycleId,
        "statut",
        CycleStatut.EN_COURS.name(),
        CycleStatut.TERMINE.name());

    cotisationRepository
        .findByCycle_IdAndStatutAndDeletedAtIsNull(cycleId, CotisationStatut.EN_ATTENTE)
        .forEach(
            c -> {
              c.setStatut(CotisationStatut.EN_RETARD);
              cotisationRepository.save(c);
            });

    // Vérifier s'il reste des cycles EN_ATTENTE
    boolean isLastCycle =
        cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(tontineId).stream()
            .noneMatch(c -> c.getStatut() == CycleStatut.EN_ATTENTE);

    List<MembreDistributionInfo> distribution;

    if (isLastCycle) {
      // Clôture finale : calcul de la distribution totale par membre
      List<TontineCommission> commissions =
          commissionRepository.findByTontine_IdAndDeletedAtIsNull(tontineId);
      List<TontineMembre> membres =
          membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontineId, MembreStatut.ACTIF);
      distribution = computeFinalDistribution(tontineId, commissions, membres);

      if (!distribution.isEmpty()) {
        BigDecimal totalCagnotte =
            distribution.stream()
                .map(MembreDistributionInfo::getMontantCotise)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCommission =
            distribution.stream()
                .map(MembreDistributionInfo::getMontantCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Mettre à jour le dernier cycle avec les totaux de toute la collecte
        cycle.setMontantJackpot(totalCagnotte);
        cycle.setMontantCommission(totalCommission);
        cycle.setMontantNet(totalCagnotte.subtract(totalCommission));
        cycleRepository.save(cycle);
      }

      notifyFinalDistribution(tontine, distribution);

      // Notifications in-app distribution finale
      for (MembreDistributionInfo dist : distribution) {
        notificationService.notify(
            dist.getUserId(),
            NotificationType.DISTRIBUTION_FINALE,
            "Distribution finale 🎉",
            "Vous avez reçu "
                + dist.getMontantNet().toPlainString()
                + " FCFA · "
                + tontine.getNom(),
            tontineId);
      }
      BigDecimal cagnotteFinale =
          distribution.stream()
              .map(MembreDistributionInfo::getMontantCotise)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      notificationService.notify(
          tontine.getCreePar().getId(),
          NotificationType.DISTRIBUTION_FINALE,
          "Distribution finale terminée",
          "La tontine *"
              + tontine.getNom()
              + "* est clôturée. Cagnotte totale : "
              + cagnotteFinale.toPlainString()
              + " FCFA",
          tontineId);

      log.info(
          "Clôture finale EVENEMENTIELLE – tontineId={} membres={} cagnotte={}",
          tontineId,
          distribution.size(),
          distribution.stream()
              .map(MembreDistributionInfo::getMontantCotise)
              .reduce(BigDecimal.ZERO, BigDecimal::add));
    } else {
      activateNextCycle(tontineId, cycle.getNumeroCycle() + 1);
      distribution = null;
      log.info(
          "Sous-cycle EVENEMENTIELLE clôturé – tontineId={} cycleId={} num={}",
          tontineId,
          cycleId,
          cycle.getNumeroCycle());
    }

    return CycleResponse.from(cycle, distribution);
  }

  /**
   * Calcule la distribution finale par membre sur l'ensemble des cycles d'une tontine
   * événementielle.
   *
   * <p>Pour chaque membre : montantNet = somme cotisations VALIDEES - quote-part commission. La
   * commission POURCENTAGE_JACKPOT est distribuée proportionnellement. La FRAIS_FIXES_PAR_CYCLE est
   * répartie proportionnellement à la mise. FRAIS_ADHESION est exclue du calcul de jackpot.
   */
  private List<MembreDistributionInfo> computeFinalDistribution(
      UUID tontineId, List<TontineCommission> commissions, List<TontineMembre> membres) {

    List<Cotisation> allValid =
        cotisationRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
            tontineId, CotisationStatut.VALIDE);

    Map<UUID, BigDecimal> totalParMembre = new HashMap<>();
    for (Cotisation c : allValid) {
      totalParMembre.merge(c.getMembre().getId(), c.getMontant(), BigDecimal::add);
    }

    if (totalParMembre.isEmpty()) return List.of();

    BigDecimal totalCagnotte =
        totalParMembre.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

    // Taux de commission global (POURCENTAGE) et frais fixes totaux
    BigDecimal tauxPct = BigDecimal.ZERO;
    BigDecimal fraisFixesTotal = BigDecimal.ZERO;
    for (TontineCommission comm : commissions) {
      switch (comm.getType()) {
        case POURCENTAGE_JACKPOT -> tauxPct = tauxPct.add(comm.getValeur());
        case FRAIS_FIXES_PAR_CYCLE -> fraisFixesTotal = fraisFixesTotal.add(comm.getValeur());
        case FRAIS_ADHESION -> {
          /* ignoré */
        }
      }
    }

    final BigDecimal finalTauxPct = tauxPct;
    final BigDecimal finalFraisFixesTotal = fraisFixesTotal;
    final BigDecimal finalTotalCagnotte =
        totalCagnotte.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : totalCagnotte;

    Map<UUID, TontineMembre> membreMap =
        membres.stream().collect(Collectors.toMap(TontineMembre::getId, m -> m));

    List<MembreDistributionInfo> result = new ArrayList<>();
    for (Map.Entry<UUID, BigDecimal> entry : totalParMembre.entrySet()) {
      UUID membreId = entry.getKey();
      BigDecimal montantCotise = entry.getValue();
      TontineMembre m = membreMap.get(membreId);
      if (m == null) continue;

      BigDecimal commissionPct =
          montantCotise
              .multiply(finalTauxPct)
              .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

      BigDecimal commissionFixePart =
          finalFraisFixesTotal.compareTo(BigDecimal.ZERO) == 0
              ? BigDecimal.ZERO
              : finalFraisFixesTotal
                  .multiply(montantCotise)
                  .divide(finalTotalCagnotte, 2, RoundingMode.HALF_UP);

      BigDecimal commissionTotale = commissionPct.add(commissionFixePart);
      BigDecimal montantNet = montantCotise.subtract(commissionTotale);

      result.add(
          MembreDistributionInfo.builder()
              .membreId(membreId)
              .userId(m.getUser().getId())
              .firstName(m.getUser().getFirstName())
              .lastName(m.getUser().getLastName())
              .phone(m.getUser().getPhone())
              .montantCotise(montantCotise)
              .montantCommission(commissionTotale)
              .montantNet(montantNet)
              .build());
    }

    return result;
  }

  private void notifyFinalDistribution(Tontine tontine, List<MembreDistributionInfo> distribution) {
    for (MembreDistributionInfo dist : distribution) {
      try {
        whatsappService.send(
            dist.getPhone(),
            "🎉 *Dinthialma* – La tontine événementielle *"
                + tontine.getNom()
                + "* est clôturée !\n"
                + "Vous recevez *"
                + dist.getMontantNet().toPlainString()
                + " FCFA* (votre mise totale : "
                + dist.getMontantCotise().toPlainString()
                + " FCFA).");
      } catch (Exception e) {
        log.warn(
            "Notif WA distribution finale non envoyée pour {} : {}",
            dist.getPhone(),
            e.getMessage());
      }
    }
    // Notif admin : résumé de la clôture
    try {
      BigDecimal cagnotteTotal =
          distribution.stream()
              .map(MembreDistributionInfo::getMontantCotise)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      whatsappService.send(
          tontine.getCreePar().getPhone(),
          "✅ *Dinthialma* – Tontine *"
              + tontine.getNom()
              + "* clôturée avec succès.\n"
              + "Cagnotte totale : *"
              + cagnotteTotal.toPlainString()
              + " FCFA* distribuée entre "
              + distribution.size()
              + " membre(s).");
    } catch (Exception e) {
      log.warn("Notif WA admin (clôture finale) non envoyée : {}", e.getMessage());
    }
  }

  // ─── Sélection bénéficiaire jackpot ──────────────────────────────────────

  @Override
  public CycleResponse selectionnerBeneficiaire(
      String keycloakId, UUID tontineId, UUID cycleId, SelectionnerBeneficiaireRequest request)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    if (tontine.getTontineType() == TontineType.EVENEMENTIELLE) {
      throw new BadRequestException(
          "Les tontines événementielles n'ont pas de bénéficiaire unique – chaque membre récupère sa mise");
    }

    CycleTontine cycle = findCycleById(tontineId, cycleId);

    if (cycle.getStatut() != CycleStatut.TERMINE) {
      throw new BadRequestException(
          "La sélection du bénéficiaire n'est possible que sur un cycle TERMINE");
    }
    if (!"MANUEL".equals(tontine.getOrdreBeneficiaire())) {
      throw new BadRequestException(
          "La désignation manuelle des gagnants n'est disponible que pour les tontines"
              + " avec ordreBeneficiaire = MANUEL");
    }
    if (cycleGagnantRepository.existsByCycle_IdAndDeletedAtIsNull(cycleId)) {
      throw new BadRequestException("Les gagnants ont déjà été désignés pour ce cycle");
    }

    int nombreGagnants = tontine.getNombreGagnants();
    List<TontineMembre> eligibles =
        membreRepository
            .findByTontine_IdAndStatutAndDeletedAtIsNull(tontineId, MembreStatut.ACTIF)
            .stream()
            .filter(m -> !m.isARecuJackpot())
            .toList();

    if (eligibles.isEmpty()) {
      throw new BadRequestException(
          "Aucun membre éligible : tous les membres actifs ont déjà reçu un jackpot");
    }

    List<TontineMembre> choisis;

    if (request.getMembreIds() != null && !request.getMembreIds().isEmpty()) {
      choisis = new ArrayList<>();
      for (UUID membreId : request.getMembreIds()) {
        TontineMembre m =
            membreRepository
                .findByIdAndTontine_IdAndDeletedAtIsNull(membreId, tontineId)
                .orElseThrow(
                    () -> new NotFoundException(ResponseMessageConstants.MEMBER_NOT_FOUND));
        if (m.isARecuJackpot()) {
          throw new BadRequestException(
              m.getUser().getFirstName() + " a déjà reçu un jackpot dans cette tontine");
        }
        choisis.add(m);
      }
    } else {
      List<TontineMembre> shuffled = new ArrayList<>(eligibles);
      Collections.shuffle(shuffled);
      choisis = shuffled.stream().limit(nombreGagnants).toList();
    }

    BigDecimal montantParGagnant =
        cycle.getMontantNet().divide(BigDecimal.valueOf(choisis.size()), 2, RoundingMode.HALF_UP);

    List<CycleGagnant> savedGagnants = new ArrayList<>();
    for (TontineMembre membre : choisis) {
      CycleGagnant g =
          CycleGagnant.builder().cycle(cycle).membre(membre).montantRecu(montantParGagnant).build();
      savedGagnants.add(cycleGagnantRepository.save(g));

      membre.setARecuJackpot(true);
      membre.setDateJackpot(LocalDateTime.now());
      membreRepository.save(membre);

      auditService.log(
          caller, "cycles_tontine", cycleId, "gagnant_ajoute", null, membre.getId().toString());
    }

    cycle.setGagnants(savedGagnants);

    log.info(
        "Gagnants jackpot désignés (MANUEL) – cycleId={} nb={} tontineId={}",
        cycleId,
        choisis.size(),
        tontineId);

    schedulerService.annoncerGagnants(cycle, savedGagnants);

    try {
      String noms =
          choisis.stream()
              .map(m -> m.getUser().getFirstName() + " " + m.getUser().getLastName())
              .reduce((a, b) -> a + ", " + b)
              .orElse("");
      whatsappService.send(
          tontine.getCreePar().getPhone(),
          "✅ *Dinthialma* – Vous avez désigné *"
              + noms
              + "* comme gagnant(s) du jackpot (cycle "
              + cycle.getNumeroCycle()
              + ", tontine *"
              + tontine.getNom()
              + "*).");
    } catch (Exception e) {
      log.warn("Notif WA admin (jackpot manuel) non envoyée : {}", e.getMessage());
    }

    return CycleResponse.from(cycle);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  private List<TontineMembre> resolveGagnants(Tontine tontine, UUID tontineId) {
    List<TontineMembre> eligibles =
        membreRepository
            .findByTontine_IdAndStatutAndDeletedAtIsNull(tontineId, MembreStatut.ACTIF)
            .stream()
            .filter(m -> !m.isARecuJackpot())
            .toList();

    if (eligibles.isEmpty()) return List.of();

    int n = Math.min(tontine.getNombreGagnants(), eligibles.size());

    return switch (tontine.getOrdreBeneficiaire()) {
      case "ROTATION" ->
          eligibles.stream()
              .sorted(
                  Comparator.comparingInt(
                          (TontineMembre m) ->
                              m.getOrdreJackpot() != null ? m.getOrdreJackpot() : Integer.MAX_VALUE)
                      .thenComparing(TontineMembre::getCreatedAt))
              .limit(n)
              .toList();
      case "ALEATOIRE" -> {
        List<TontineMembre> shuffled = new ArrayList<>(eligibles);
        Collections.shuffle(shuffled);
        yield shuffled.stream().limit(n).toList();
      }
      default -> List.of(); // MANUEL
    };
  }

  private void activateNextCycle(UUID tontineId, int nextNum) {
    cycleRepository.findByTontine_IdAndDeletedAtIsNullOrderByNumeroCycleAsc(tontineId).stream()
        .filter(c -> c.getNumeroCycle() == nextNum && c.getStatut() == CycleStatut.EN_ATTENTE)
        .findFirst()
        .ifPresent(
            next -> {
              next.setStatut(CycleStatut.EN_COURS);
              cycleRepository.save(next);
              log.info("Cycle suivant activé – cycleId={} num={}", next.getId(), nextNum);
            });
  }

  private User findUserByKeycloakId(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }

  private Tontine findTontineById(UUID id) throws CustomException {
    return tontineRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.TONTINE_NOT_FOUND));
  }

  private CycleTontine findCycleById(UUID tontineId, UUID cycleId) throws CustomException {
    return cycleRepository
        .findByIdAndTontine_IdAndDeletedAtIsNull(cycleId, tontineId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.CYCLE_NOT_FOUND));
  }

  private void assertIsCreatorOrSuperAdmin(User caller, Tontine tontine) throws CustomException {
    if (isSuperAdmin(caller)) return;
    if (!tontine.getCreePar().getId().equals(caller.getId())) {
      throw new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED);
    }
  }

  private void assertCanAccess(User caller, Tontine tontine) throws CustomException {
    if (isSuperAdmin(caller)) return;
    if (tontine.getCreePar().getId().equals(caller.getId())) return;
    if (membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
        tontine.getId(), caller.getId())) return;
    throw new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED);
  }

  private boolean isSuperAdmin(User user) {
    return roleAssignmentRepository.existsByUserIdAndRole(user.getId(), UserRole.SUPER_ADMIN);
  }
}
