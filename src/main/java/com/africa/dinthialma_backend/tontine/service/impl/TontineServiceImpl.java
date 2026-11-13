package com.africa.dinthialma_backend.tontine.service.impl;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserRoleAssignment;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.OrdreBeneficiaire;
import com.africa.dinthialma_backend.tontine.codeList.RoleFiltreTontine;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.dto.ChangerOrdreBeneficiaireRequest;
import com.africa.dinthialma_backend.tontine.dto.CreateTontineRequest;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import com.africa.dinthialma_backend.tontine.dto.UpdateTontineRequest;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.mapper.TontineMapper;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineSpecifications;
import com.africa.dinthialma_backend.tontine.service.interfaces.CommissionService;
import com.africa.dinthialma_backend.tontine.service.interfaces.TontineService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TontineServiceImpl implements TontineService {

  private final TontineRepository tontineRepository;
  private final TontineMapper tontineMapper;
  private final CycleTontineRepository cycleRepository;
  private final TontineMembreRepository membreRepository;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository roleAssignmentRepository;
  private final KeycloakAuthService keycloakAuthService;
  private final AuditService auditService;
  private final NotificationService notificationService;
  private final CommissionService commissionService;

  // ─── Création ────────────────────────────────────────────────────────────

  @Override
  public TontineResponse createTontine(String keycloakId, CreateTontineRequest request)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    TontineType type =
        request.getTontineType() != null ? request.getTontineType() : TontineType.ROTATIVE;

    validateCreateRequest(request, type);
    commissionService.verifierCommissionsInitiales(request.getCommissions());

    BigDecimal montantEffectif = resolveMontant(request, type);

    Tontine tontine =
        Tontine.builder()
            .tontineType(type)
            .nom(request.getNom().trim())
            .description(request.getDescription())
            .montant(montantEffectif)
            .frequence(request.getFrequence())
            .ordreBeneficiaire(
                type == TontineType.EVENEMENTIELLE ? null : request.getOrdreBeneficiaire())
            .modeCycle(
                type == TontineType.EVENEMENTIELLE ? ModeCycle.AUTOMATIQUE : request.getModeCycle())
            .dateDebut(request.getDateDebut())
            .nombreMembres(request.getNombreMembres() != null ? request.getNombreMembres() : 0)
            .nombreGagnants(request.getNombreGagnants())
            .statut(TontineStatut.BROUILLON)
            .creePar(caller)
            .dateEcheance(type == TontineType.EVENEMENTIELLE ? request.getDateEcheance() : null)
            .nomEvenement(type == TontineType.EVENEMENTIELLE ? request.getNomEvenement() : null)
            .montantLibre(type == TontineType.EVENEMENTIELLE && request.isMontantLibre())
            .montantMinimum(type == TontineType.EVENEMENTIELLE ? request.getMontantMinimum() : null)
            .build();

    Tontine saved = tontineRepository.save(tontine);
    commissionService.enregistrerCommissionsInitiales(caller, saved, request.getCommissions());
    assignRoleIfAbsent(caller, UserRole.ADMIN);
    auditService.logCreate(caller, "tontines", saved.getId());
    log.info("Tontine créée – id={} type={} par userId={}", saved.getId(), type, caller.getId());
    return tontineMapper.toResponse(saved, 0, caller.getId(), null);
  }

  // ─── Lecture ─────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public TontineResponse getTontine(String keycloakId, UUID id) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(id);

    assertCanAccess(caller, tontine);

    return toResponse(tontine, caller);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<TontineResponse> listTontines(
      String keycloakId, RoleFiltreTontine role, TontineStatut statut, String q, Pageable pageable)
      throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    UUID uid = caller.getId();

    Specification<Tontine> perimetre;
    if (role == RoleFiltreTontine.GERE) {
      perimetre = TontineSpecifications.creeePar(uid);
    } else if (role == RoleFiltreTontine.COTISE) {
      perimetre = TontineSpecifications.cotiseePar(uid);
    } else if (isSuperAdmin(caller)) {
      perimetre = null;
    } else {
      perimetre = TontineSpecifications.visiblePar(uid);
    }

    Page<Tontine> tontines =
        tontineRepository.findAll(
            Specification.allOf(
                TontineSpecifications.nonSupprimee(),
                perimetre,
                TontineSpecifications.statut(statut),
                TontineSpecifications.recherche(q)),
            pageable);

    // Memberships de l'appelant sur la page courante : une seule requête
    List<UUID> tontineIds = tontines.getContent().stream().map(Tontine::getId).toList();
    Map<UUID, TontineMembre> mesMemberships =
        tontineIds.isEmpty()
            ? Map.of()
            : membreRepository
                .findByUser_IdAndTontine_IdInAndDeletedAtIsNull(uid, tontineIds)
                .stream()
                .collect(Collectors.toMap(m -> m.getTontine().getId(), m -> m, (a, b) -> a));

    return tontines.map(
        t -> {
          int count = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(t.getId());
          return tontineMapper.toResponse(t, count, uid, mesMemberships.get(t.getId()));
        });
  }

  // ─── Mise à jour ─────────────────────────────────────────────────────────

  @Override
  public TontineResponse updateTontine(String keycloakId, UUID id, UpdateTontineRequest request)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(id);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    if (tontine.getStatut() != TontineStatut.BROUILLON) {
      throw new BadRequestException("La tontine ne peut être modifiée qu'en statut BROUILLON");
    }

    appliquerChampsCommuns(caller, tontine, request);
    if (tontine.getTontineType() == TontineType.EVENEMENTIELLE) {
      appliquerChampsEvenementielle(caller, tontine, request);
    }

    Tontine saved = tontineRepository.save(tontine);
    return toResponse(saved, caller);
  }

  private void appliquerChampsCommuns(User caller, Tontine tontine, UpdateTontineRequest request)
      throws CustomException {
    String nom = request.getNom() != null ? request.getNom().trim() : null;
    modifier(caller, tontine, "nom", tontine.getNom(), nom, String::valueOf, tontine::setNom);
    modifier(
        caller,
        tontine,
        "description",
        tontine.getDescription(),
        request.getDescription(),
        String::valueOf,
        tontine::setDescription);
    modifier(
        caller,
        tontine,
        "montant",
        tontine.getMontant(),
        request.getMontant(),
        BigDecimal::toPlainString,
        tontine::setMontant);
    modifier(
        caller,
        tontine,
        "frequence",
        tontine.getFrequence(),
        request.getFrequence(),
        String::valueOf,
        tontine::setFrequence);
    if (request.getOrdreBeneficiaire() != null) {
      verifierOrdreBeneficiaire(request.getOrdreBeneficiaire());
    }
    modifier(
        caller,
        tontine,
        "ordreBeneficiaire",
        tontine.getOrdreBeneficiaire(),
        request.getOrdreBeneficiaire(),
        String::valueOf,
        tontine::setOrdreBeneficiaire);
    modifier(
        caller,
        tontine,
        "dateDebut",
        tontine.getDateDebut(),
        request.getDateDebut(),
        LocalDate::toString,
        tontine::setDateDebut);
    modifier(
        caller,
        tontine,
        "nombreMembres",
        tontine.getNombreMembres(),
        request.getNombreMembres(),
        String::valueOf,
        tontine::setNombreMembres);
    modifier(
        caller,
        tontine,
        "nombreGagnants",
        tontine.getNombreGagnants(),
        request.getNombreGagnants(),
        String::valueOf,
        tontine::setNombreGagnants);
  }

  private void appliquerChampsEvenementielle(
      User caller, Tontine tontine, UpdateTontineRequest request) throws CustomException {
    LocalDate echeance = request.getDateEcheance();
    if (echeance != null && !echeance.isAfter(tontine.getDateDebut())) {
      throw new BadRequestException("La date d'échéance doit être postérieure à la date de début");
    }
    modifier(
        caller,
        tontine,
        "dateEcheance",
        tontine.getDateEcheance(),
        echeance,
        LocalDate::toString,
        tontine::setDateEcheance);
    if (request.getNomEvenement() != null) {
      tontine.setNomEvenement(request.getNomEvenement());
    }
    if (request.getMontantMinimum() != null) {
      tontine.setMontantMinimum(request.getMontantMinimum());
    }
    if (request.getMontantLibre() != null) {
      tontine.setMontantLibre(request.getMontantLibre());
    }
  }

  /** Champ non null dans la requête : trace l'ancienne et la nouvelle valeur, puis l'affecte. */
  private <T> void modifier(
      User caller,
      Tontine tontine,
      String champ,
      T ancienne,
      T nouvelle,
      Function<T, String> texte,
      Consumer<T> affecter) {
    if (nouvelle == null) {
      return;
    }
    String ancienTexte = ancienne != null ? texte.apply(ancienne) : null;
    auditService.log(
        caller, "tontines", tontine.getId(), champ, ancienTexte, texte.apply(nouvelle));
    affecter.accept(nouvelle);
  }

  // ─── Ordre des bénéficiaires en cours de tontine ─────────────────────────

  @Override
  public TontineResponse changerOrdreBeneficiaire(
      String keycloakId, UUID id, ChangerOrdreBeneficiaireRequest request) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(id);
    assertIsCreatorOrSuperAdmin(caller, tontine);
    String nouvel = request.getOrdreBeneficiaire().trim();
    verifierOrdreBeneficiaire(nouvel);
    verifierOrdreModifiable(tontine, nouvel);

    String ancien = tontine.getOrdreBeneficiaire();
    if (nouvel.equals(ancien)) {
      return toResponse(tontine, caller);
    }
    tontine.setOrdreBeneficiaire(nouvel);
    Tontine saved = tontineRepository.save(tontine);
    auditService.log(caller, "tontines", id, "ordreBeneficiaire", ancien, nouvel);
    notifierChangementOrdre(saved);
    log.info("Ordre des bénéficiaires modifié – tontineId={} {} → {}", id, ancien, nouvel);
    return toResponse(saved, caller);
  }

  private void verifierOrdreModifiable(Tontine tontine, String nouvel) throws CustomException {
    boolean modifiable =
        tontine.getStatut() == TontineStatut.BROUILLON
            || tontine.getStatut() == TontineStatut.ACTIVE
            || tontine.getStatut() == TontineStatut.SUSPENDUE;
    if (tontine.getTontineType() != TontineType.ROTATIVE || !modifiable) {
      throw new BadRequestException(
          "L'ordre des bénéficiaires ne se modifie que sur une tontine rotative non terminée");
    }
    if (!OrdreBeneficiaire.designationApresCloture(nouvel)
        && cycleRepository.existsTermineSansGagnant(tontine.getId())) {
      throw new ConflictException(
          "Un tour clôturé attend encore ses gagnants : désignez-les avant de passer à un ordre"
              + " automatique",
          ErrorCode.DESIGNATION_EN_ATTENTE);
    }
  }

  private void notifierChangementOrdre(Tontine tontine) {
    String libelle = libelleOrdre(tontine.getOrdreBeneficiaire());
    membreRepository
        .findByTontine_IdAndStatutAndDeletedAtIsNull(tontine.getId(), MembreStatut.ACTIF)
        .forEach(
            m ->
                notificationService.notify(
                    m.getUser().getId(),
                    NotificationType.ORDRE_BENEFICIAIRE_MODIFIE,
                    "Règle du jackpot modifiée",
                    "Les gagnants de "
                        + tontine.getNom()
                        + " seront désormais désignés "
                        + libelle
                        + ", à partir de la prochaine clôture de tour.",
                    tontine.getId()));
  }

  private static String libelleOrdre(String ordre) {
    return switch (ordre) {
      case "ROTATION" -> "selon l'ordre de passage";
      case "ALEATOIRE" -> "par tirage au sort automatique";
      case "HYBRIDE" -> "par le gestionnaire à chaque tour (tirage ou choix)";
      default -> "par le choix du gestionnaire";
    };
  }

  // ─── Suppression ─────────────────────────────────────────────────────────

  @Override
  public void deleteTontine(String keycloakId, UUID id) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(id);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    if (tontine.getStatut() != TontineStatut.BROUILLON) {
      throw new BadRequestException("Seule une tontine en BROUILLON peut être supprimée");
    }

    tontine.setDeletedAt(LocalDateTime.now());
    tontineRepository.save(tontine);
    auditService.logDelete(caller, "tontines", id);
    log.info("Tontine supprimée (soft delete) – id={}", id);
  }

  // ─── Activation ──────────────────────────────────────────────────────────

  @Override
  public TontineResponse activateTontine(String keycloakId, UUID id) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(id);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    if (tontine.getStatut() != TontineStatut.BROUILLON
        && tontine.getStatut() != TontineStatut.SUSPENDUE) {
      throw new BadRequestException(
          "La tontine doit être en BROUILLON ou SUSPENDUE pour être activée");
    }

    String oldStatut = tontine.getStatut().name();
    tontine.setStatut(TontineStatut.ACTIVE);

    if (tontine.getTontineType() == TontineType.EVENEMENTIELLE) {
      if (!cycleRepository.existsByTontine_IdAndDeletedAtIsNull(id)) {
        generateEvenementielleSubCycles(tontine);
      }
    } else if (tontine.getModeCycle() == ModeCycle.AUTOMATIQUE
        && !cycleRepository.existsByTontine_IdAndDeletedAtIsNull(id)) {
      generateCycles(tontine);
    }

    Tontine saved = tontineRepository.save(tontine);
    auditService.log(caller, "tontines", id, "statut", oldStatut, TontineStatut.ACTIVE.name());
    log.info("Tontine activée – id={} type={}", id, tontine.getTontineType());
    return toResponse(saved, caller);
  }

  // ─── Suspension ──────────────────────────────────────────────────────────

  @Override
  public TontineResponse suspendTontine(String keycloakId, UUID id) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(id);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    if (tontine.getStatut() != TontineStatut.ACTIVE) {
      throw new BadRequestException("Seule une tontine ACTIVE peut être suspendue");
    }

    tontine.setStatut(TontineStatut.SUSPENDUE);
    Tontine saved = tontineRepository.save(tontine);
    auditService.log(
        caller,
        "tontines",
        id,
        "statut",
        TontineStatut.ACTIVE.name(),
        TontineStatut.SUSPENDUE.name());
    log.info("Tontine suspendue – id={}", id);
    return toResponse(saved, caller);
  }

  // ─── Génération automatique des cycles (ROTATIVE) ────────────────────────

  private void generateCycles(Tontine tontine) {
    List<TontineMembre> membres =
        new ArrayList<>(
            membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
                tontine.getId(), MembreStatut.ACTIF));

    if ("ALEATOIRE".equals(tontine.getOrdreBeneficiaire())) {
      Collections.shuffle(membres);
      for (int i = 0; i < membres.size(); i++) {
        membres.get(i).setOrdreJackpot(i + 1);
      }
      membreRepository.saveAll(membres);
    } else {
      membres.sort(
          Comparator.comparingInt(
              m -> (m.getOrdreJackpot() != null ? m.getOrdreJackpot() : Integer.MAX_VALUE)));
    }

    int n = tontine.getNombreGagnants();
    int nombreCycles = (int) Math.ceil((double) membres.size() / n);
    LocalDate current = tontine.getDateDebut();

    for (int i = 0; i < nombreCycles; i++) {
      LocalDate fin = calculateEndDate(current, tontine.getFrequence());

      CycleTontine cycle =
          CycleTontine.builder()
              .tontine(tontine)
              .numeroCycle(i + 1)
              .dateDebut(current)
              .dateFin(fin)
              .statut(i == 0 ? CycleStatut.EN_COURS : CycleStatut.EN_ATTENTE)
              .build();

      cycleRepository.save(cycle);
      current = fin.plusDays(1);
    }

    log.info(
        "Cycles ROTATIVE générés – tontineId={} membres={} gagnants/cycle={} cycles={}",
        tontine.getId(),
        membres.size(),
        n,
        nombreCycles);
  }

  // ─── Génération des sous-cycles (EVENEMENTIELLE) ─────────────────────────

  /**
   * Génère les sous-cycles informatifs d'une tontine événementielle.
   *
   * <p>Chaque sous-cycle couvre une période selon la fréquence. Le dernier se termine exactement
   * sur {@code dateEcheance}. La fréquence est utilisée pour planifier les rappels WhatsApp.
   */
  private void generateEvenementielleSubCycles(Tontine tontine) {
    LocalDate current = tontine.getDateDebut();
    LocalDate echeance = tontine.getDateEcheance();
    int cycleNum = 1;

    while (!current.isAfter(echeance)) {
      LocalDate normalEnd = calculateEndDate(current, tontine.getFrequence());
      LocalDate actualEnd = normalEnd.isAfter(echeance) ? echeance : normalEnd;

      CycleTontine cycle =
          CycleTontine.builder()
              .tontine(tontine)
              .numeroCycle(cycleNum)
              .dateDebut(current)
              .dateFin(actualEnd)
              .statut(cycleNum == 1 ? CycleStatut.EN_COURS : CycleStatut.EN_ATTENTE)
              .build();

      cycleRepository.save(cycle);
      current = actualEnd.plusDays(1);
      cycleNum++;
    }

    log.info(
        "Sous-cycles EVENEMENTIELLE générés – tontineId={} nbCycles={} echéance={}",
        tontine.getId(),
        cycleNum - 1,
        echeance);
  }

  private LocalDate calculateEndDate(LocalDate start, String frequence) {
    return switch (frequence.toUpperCase()) {
      case "JOURNALIERE" -> start;
      case "HEBDOMADAIRE" -> start.plusDays(6);
      case "BIMENSUEL" -> start.plusDays(13);
      case "TRIMESTRIEL" -> start.plusMonths(3).minusDays(1);
      default -> start.plusMonths(1).minusDays(1); // MENSUEL par défaut
    };
  }

  // ─── Validation création ─────────────────────────────────────────────────

  private void validateCreateRequest(CreateTontineRequest request, TontineType type)
      throws CustomException {
    if (type == TontineType.EVENEMENTIELLE) {
      validerCreationEvenementielle(request);
    } else {
      validerCreationRotative(request);
    }
  }

  private static void validerCreationEvenementielle(CreateTontineRequest request)
      throws CustomException {
    if (request.getDateEcheance() == null) {
      throw new BadRequestException(
          "La date d'échéance est obligatoire pour une tontine événementielle");
    }
    if (!request.getDateEcheance().isAfter(request.getDateDebut())) {
      throw new BadRequestException("La date d'échéance doit être postérieure à la date de début");
    }
    if (!request.isMontantLibre() && !estStrictementPositif(request.getMontant())) {
      throw new BadRequestException(
          "Le montant fixe de cotisation est obligatoire quand montantLibre=false");
    }
  }

  private static void validerCreationRotative(CreateTontineRequest request) throws CustomException {
    if (request.getOrdreBeneficiaire() == null || request.getOrdreBeneficiaire().isBlank()) {
      throw new BadRequestException(
          "L'ordre des bénéficiaires est obligatoire pour une tontine rotative");
    }
    verifierOrdreBeneficiaire(request.getOrdreBeneficiaire());
    if (!estStrictementPositif(request.getMontant())) {
      throw new BadRequestException(
          "Le montant de cotisation est obligatoire pour une tontine rotative");
    }
    if (request.getNombreMembres() == null || request.getNombreMembres() < 2) {
      throw new BadRequestException(
          "Le nombre de membres doit être au minimum 2 pour une tontine rotative");
    }
    if (request.getModeCycle() == null) {
      throw new BadRequestException("Le mode de cycle est obligatoire pour une tontine rotative");
    }
  }

  private static boolean estStrictementPositif(BigDecimal montant) {
    return montant != null && montant.compareTo(BigDecimal.ZERO) > 0;
  }

  private static void verifierOrdreBeneficiaire(String ordre) throws CustomException {
    if (!OrdreBeneficiaire.estValide(ordre)) {
      throw new BadRequestException(
          "Ordre des bénéficiaires inconnu : « "
              + ordre
              + " ». Valeurs acceptées : "
              + OrdreBeneficiaire.VALEURS,
          ErrorCode.INVALID_PARAMETER);
    }
  }

  private BigDecimal resolveMontant(CreateTontineRequest request, TontineType type) {
    if (type == TontineType.EVENEMENTIELLE && request.isMontantLibre()) {
      return request.getMontantMinimum() != null ? request.getMontantMinimum() : BigDecimal.ZERO;
    }
    return request.getMontant();
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

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

  /** Réponse enrichie du rôle de l'appelant (détail et mutations unitaires). */
  private TontineResponse toResponse(Tontine tontine, User caller) {
    int nombreMembres = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(tontine.getId());
    TontineMembre monMembership =
        membreRepository
            .findByTontine_IdAndUser_IdAndDeletedAtIsNull(tontine.getId(), caller.getId())
            .orElse(null);
    return tontineMapper.toResponse(tontine, nombreMembres, caller.getId(), monMembership);
  }

  private boolean isSuperAdmin(User user) {
    return roleAssignmentRepository.existsByUserIdAndRole(user.getId(), UserRole.SUPER_ADMIN);
  }

  private void assignRoleIfAbsent(User user, UserRole role) {
    if (roleAssignmentRepository.existsByUserIdAndRole(user.getId(), role)) return;

    boolean synced = true;
    try {
      keycloakAuthService.assignRole(user.getKeycloakId(), role.getKeycloakRole());
    } catch (Exception e) {
      log.warn(
          "Impossible d'assigner le rôle {} à user {} dans Keycloak : {}",
          role,
          user.getId(),
          e.getMessage());
      synced = false;
    }

    UserRoleAssignment assignment =
        UserRoleAssignment.builder().user(user).role(role).syncedToKeycloak(synced).build();
    roleAssignmentRepository.save(assignment);
  }
}
