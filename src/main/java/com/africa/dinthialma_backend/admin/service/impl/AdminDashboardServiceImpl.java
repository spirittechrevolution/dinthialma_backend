package com.africa.dinthialma_backend.admin.service.impl;

import com.africa.dinthialma_backend.adhesion.codeList.StatutDemande;
import com.africa.dinthialma_backend.adhesion.repository.DemandeAdhesionRepository;
import com.africa.dinthialma_backend.admin.dto.AdminUserResponse;
import com.africa.dinthialma_backend.admin.dto.GlobalDashboardResponse;
import com.africa.dinthialma_backend.admin.dto.MyDashboardResponse;
import com.africa.dinthialma_backend.admin.dto.UpdateUserRolesRequest;
import com.africa.dinthialma_backend.admin.service.interfaces.AdminDashboardService;
import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserRoleAssignment;
import com.africa.dinthialma_backend.auth.mapper.UserMapper;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.auth.repository.UserSpecifications;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.util.Fuseau;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.contribution.mapper.CotisationMapper;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.jackpot.codeList.StatutPriorite;
import com.africa.dinthialma_backend.jackpot.repository.PrioriteJackpotRepository;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.tontine.calendrier.Rythme;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.OrdreBeneficiaire;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.mapper.TontineMapper;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineSpecifications;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implémentation du service d'administration et de dashboard. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminDashboardServiceImpl implements AdminDashboardService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final CotisationMapper cotisationMapper;
  private final TontineMapper tontineMapper;
  private final UserRoleAssignmentRepository roleRepository;
  private final TontineRepository tontineRepository;
  private final CycleTontineRepository cycleRepository;
  private final TontineMembreRepository membreRepository;
  private final CotisationRepository cotisationRepository;
  private final KeycloakAuthService keycloakAuthService;
  private final DemandeAdhesionRepository demandeRepository;
  private final PrioriteJackpotRepository prioriteRepository;

  // ─── Dashboard global SUPER_ADMIN ─────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public GlobalDashboardResponse getGlobalDashboard(String keycloakId) throws CustomException {
    assertSuperAdmin(keycloakId);

    LocalDateTime debutMois =
        LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime hier = LocalDateTime.now().minusHours(24);

    // ─── Users ────────────────────────────────────────────────────────────
    long totalUsers = userRepository.countByDeletedAtIsNull();
    long actifsUsers = userRepository.countByActiveTrueAndDeletedAtIsNull();
    long desactivesUsers = userRepository.countByActiveFalseAndDeletedAtIsNull();
    long nouveauxCeMois =
        userRepository.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(debutMois);

    // ─── Tontines ─────────────────────────────────────────────────────────
    long totalTontines = tontineRepository.countByDeletedAtIsNull();
    long brouillon = tontineRepository.countByStatutAndDeletedAtIsNull(TontineStatut.BROUILLON);
    long actives = tontineRepository.countByStatutAndDeletedAtIsNull(TontineStatut.ACTIVE);
    long suspendues = tontineRepository.countByStatutAndDeletedAtIsNull(TontineStatut.SUSPENDUE);
    long terminees = tontineRepository.countByStatutAndDeletedAtIsNull(TontineStatut.TERMINEE);

    // ─── Finances ─────────────────────────────────────────────────────────
    long enAttente =
        cotisationRepository.countByStatutAndDeletedAtIsNull(CotisationStatut.EN_ATTENTE);
    long enRetard =
        cotisationRepository.countByStatutAndDeletedAtIsNull(CotisationStatut.EN_RETARD);
    var montantMois = cotisationRepository.sumMontantValideSince(debutMois);

    // ─── Activité 24h ─────────────────────────────────────────────────────
    long nouveauxInscrits24h =
        userRepository.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(hier);
    long cotisations24h =
        cotisationRepository.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(hier);

    return GlobalDashboardResponse.builder()
        .users(
            GlobalDashboardResponse.UsersStats.builder()
                .total(totalUsers)
                .actifs(actifsUsers)
                .desactives(desactivesUsers)
                .nouveauxCeMois(nouveauxCeMois)
                .build())
        .tontines(
            GlobalDashboardResponse.TontinesStats.builder()
                .total(totalTontines)
                .brouillon(brouillon)
                .actives(actives)
                .suspendues(suspendues)
                .terminees(terminees)
                .build())
        .finances(
            GlobalDashboardResponse.FinancesStats.builder()
                .cotisationsEnAttente(enAttente)
                .cotisationsEnRetard(enRetard)
                .montantCotisationsCeMois(montantMois)
                .build())
        .activite24h(
            GlobalDashboardResponse.ActiviteRecente.builder()
                .nouveauxInscrits(nouveauxInscrits24h)
                .cotisationsEnregistrees(cotisations24h)
                .build())
        .build();
  }

  // ─── Gestion des utilisateurs SUPER_ADMIN ─────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<AdminUserResponse> listUsers(
      String keycloakId, AccountStatus accountStatus, UserRole role, String q, Pageable pageable)
      throws CustomException {
    assertSuperAdmin(keycloakId);

    return userRepository
        .findAll(
            Specification.allOf(
                UserSpecifications.nonSupprime(),
                UserSpecifications.accountStatus(accountStatus),
                UserSpecifications.role(role),
                UserSpecifications.recherche(q)),
            pageable)
        .map(userMapper::toAdmin);
  }

  @Override
  @Transactional(readOnly = true)
  public AdminUserResponse getUserDetail(String keycloakId, UUID userId) throws CustomException {
    assertSuperAdmin(keycloakId);
    User user = findUserById(userId);
    return userMapper.toAdmin(user);
  }

  @Override
  public AdminUserResponse disableUser(String keycloakId, UUID userId) throws CustomException {
    User caller = assertSuperAdmin(keycloakId);
    User target = findUserById(userId);

    if (caller.getId().equals(target.getId())) {
      throw new BadRequestException("Un SUPER_ADMIN ne peut pas désactiver son propre compte");
    }
    if (!target.isActive()) {
      throw new BadRequestException("Ce compte est déjà désactivé");
    }

    target.setActive(false);
    userRepository.save(target);

    try {
      keycloakAuthService.disableUser(target.getKeycloakId());
    } catch (Exception e) {
      log.warn("Keycloak indisponible lors de la désactivation de {} : {}", userId, e.getMessage());
    }

    log.info("Compte désactivé – userId={} par superAdmin={}", userId, caller.getId());
    return userMapper.toAdmin(target);
  }

  @Override
  public AdminUserResponse enableUser(String keycloakId, UUID userId) throws CustomException {
    assertSuperAdmin(keycloakId);
    User target = findUserById(userId);

    if (target.isActive()) {
      throw new BadRequestException("Ce compte est déjà actif");
    }

    target.setActive(true);
    userRepository.save(target);

    try {
      keycloakAuthService.enableUser(target.getKeycloakId());
    } catch (Exception e) {
      log.warn("Keycloak indisponible lors de la réactivation de {} : {}", userId, e.getMessage());
    }

    log.info("Compte réactivé – userId={}", userId);
    return userMapper.toAdmin(target);
  }

  @Override
  public AdminUserResponse updateUserRoles(
      String keycloakId, UUID userId, UpdateUserRolesRequest request) throws CustomException {

    assertSuperAdmin(keycloakId);
    User target = findUserById(userId);

    // USER est le rôle de base – il ne peut jamais être retiré
    Set<UserRole> newRoles = request.getRoles();
    newRoles.add(UserRole.USER);

    List<UserRoleAssignment> currentAssignments = roleRepository.findAllByUserId(target.getId());
    newRoles.stream()
        .filter(role -> !roleRepository.existsByUserIdAndRole(target.getId(), role))
        .forEach(role -> ajouterRole(target, role));
    currentAssignments.stream()
        .filter(a -> a.getRole() != UserRole.USER && !newRoles.contains(a.getRole()))
        .forEach(a -> retirerRole(target, a));

    // Recharger l'entité pour avoir les rôles à jour
    User refreshed =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
    return userMapper.toAdmin(refreshed);
  }

  /** Ajoute le rôle en base ; l'échec Keycloak est toléré (rôle marqué non synchronisé). */
  private void ajouterRole(User target, UserRole role) {
    boolean synced = true;
    try {
      keycloakAuthService.assignRole(target.getKeycloakId(), role.getKeycloakRole());
    } catch (Exception e) {
      log.warn("Keycloak indisponible, rôle {} non synchronisé : {}", role, e.getMessage());
      synced = false;
    }
    roleRepository.save(
        UserRoleAssignment.builder().user(target).role(role).syncedToKeycloak(synced).build());
    log.info("Rôle {} ajouté à userId={}", role, target.getId());
  }

  /** Retire le rôle en base ; l'échec Keycloak est toléré. */
  private void retirerRole(User target, UserRoleAssignment assignment) {
    UserRole role = assignment.getRole();
    try {
      keycloakAuthService.removeRole(target.getKeycloakId(), role.getKeycloakRole());
    } catch (Exception e) {
      log.warn("Keycloak indisponible, rôle {} non révoqué : {}", role, e.getMessage());
    }
    roleRepository.delete(assignment);
    log.info("Rôle {} retiré de userId={}", role, target.getId());
  }

  // ─── Listes plateforme SUPER_ADMIN ────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<TontineResponse> listAllTontines(
      String keycloakId, TontineStatut statut, String q, Pageable pageable) throws CustomException {
    assertSuperAdmin(keycloakId);

    Page<Tontine> tontines =
        tontineRepository.findAll(
            Specification.allOf(
                TontineSpecifications.nonSupprimee(),
                TontineSpecifications.statut(statut),
                TontineSpecifications.recherche(q)),
            pageable);

    return tontines.map(
        t ->
            tontineMapper.toResponse(
                t, (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(t.getId())));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CotisationResponse> listAllContributions(
      String keycloakId, CotisationStatut statut, UUID tontineId, Pageable pageable)
      throws CustomException {
    assertSuperAdmin(keycloakId);

    Page<Cotisation> cotisations;
    if (tontineId != null && statut != null) {
      cotisations =
          cotisationRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
              tontineId, statut, pageable);
    } else if (tontineId != null) {
      cotisations = cotisationRepository.findByTontine_IdAndDeletedAtIsNull(tontineId, pageable);
    } else if (statut != null) {
      cotisations = cotisationRepository.findByStatutAndDeletedAtIsNull(statut, pageable);
    } else {
      cotisations = cotisationRepository.findByDeletedAtIsNull(pageable);
    }

    return cotisations.map(cotisationMapper::toResponse);
  }

  // ─── Dashboard ADMIN (créateur tontines) ─────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public MyDashboardResponse getMyDashboard(String keycloakId, Pageable pageable)
      throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    UUID uid = caller.getId();

    Page<MyDashboardResponse.TontineStats> statsPage =
        tontineRepository
            .findByCreePar_IdAndDeletedAtIsNull(uid, pageable)
            .map(this::buildTontineStats);

    // Agrégats globaux : calculés en base sur toutes les tontines, indépendamment de la page
    return MyDashboardResponse.builder()
        .nombreTontinesGerees(statsPage.getTotalElements())
        .montantTotalValide(cotisationRepository.sumMontantValideByCreateur(uid))
        .totalMembres(
            membreRepository.countByTontine_CreePar_IdAndTontine_DeletedAtIsNullAndDeletedAtIsNull(
                uid))
        .totalCotisationsEnAttente(countCotisationsCreateur(uid, CotisationStatut.EN_ATTENTE))
        .totalCotisationsEnRetard(countCotisationsCreateur(uid, CotisationStatut.EN_RETARD))
        .totalDemandesEnAttente(
            demandeRepository
                .countByTontine_CreePar_IdAndTontine_DeletedAtIsNullAndStatutAndDeletedAtIsNull(
                    uid, StatutDemande.EN_ATTENTE))
        .totalCollecteToursEnCours(cotisationRepository.sumMontantValideToursEnCoursByCreateur(uid))
        .totalAttenduToursEnCours(computeTotalAttenduToursEnCours(uid))
        .tontines(new PagedModel<>(statsPage))
        .build();
  }

  /** Somme, sur les tontines gérées ayant un tour EN_COURS, de (montant par membre × ACTIFS). */
  private BigDecimal computeTotalAttenduToursEnCours(UUID userId) {
    BigDecimal total = BigDecimal.ZERO;
    for (Object[] row : membreRepository.findParametresToursEnCoursByCreateur(userId)) {
      BigDecimal parMembre =
          Tontine.montantAttenduParMembre(
              Boolean.TRUE.equals(row[0]), (BigDecimal) row[1], (BigDecimal) row[2]);
      if (parMembre != null) {
        total = total.add(parMembre.multiply(BigDecimal.valueOf((Long) row[3])));
      }
    }
    return total;
  }

  private long countCotisationsCreateur(UUID userId, CotisationStatut statut) {
    return cotisationRepository
        .countByTontine_CreePar_IdAndTontine_DeletedAtIsNullAndStatutAndDeletedAtIsNull(
            userId, statut);
  }

  /** Calcule les métriques d'une tontine pour le dashboard admin. */
  private MyDashboardResponse.TontineStats buildTontineStats(Tontine tontine) {
    UUID tid = tontine.getId();

    int nombreMembres = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(tid);
    long enAttente =
        cotisationRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(
            tid, CotisationStatut.EN_ATTENTE);
    long enRetard =
        cotisationRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(
            tid, CotisationStatut.EN_RETARD);
    var montantValide = cotisationRepository.sumMontantValideByTontine(tid);

    MyDashboardResponse.CycleEnCoursInfo cycleInfo =
        cycleRepository
            .findByTontine_IdAndStatutAndDeletedAtIsNull(tid, CycleStatut.EN_COURS)
            .map(cycle -> cycleEnCoursInfo(tontine, cycle))
            .orElse(null);

    boolean evenementielle = tontine.getTontineType() == TontineType.EVENEMENTIELLE;
    boolean gagnantsADesigner =
        tontine.getTontineType() == TontineType.ROTATIVE
            && OrdreBeneficiaire.designationApresCloture(tontine.getOrdreBeneficiaire())
            && cycleRepository.existsTermineSansGagnant(tid);

    return MyDashboardResponse.TontineStats.builder()
        .tontineId(tid)
        .nom(tontine.getNom())
        .statut(tontine.getStatut())
        .tontineType(tontine.getTontineType())
        .montant(tontine.getMontant())
        .frequence(tontine.getFrequence())
        .rythmeIntervalle(tontine.getRythme().intervalle())
        .rythmeUnite(tontine.getRythme().unite())
        .jourEcheance(tontine.getRythme().jourEcheance())
        .heureEcheance(Rythme.formaterHeure(tontine.getRythme().heureEcheance()))
        .cotisationsParTour(tontine.getCotisationsParTourEffectif())
        .declenchementJackpot(tontine.getDeclenchementJackpot())
        .dateDebut(tontine.getDateDebut())
        .dateEcheance(evenementielle ? tontine.getDateEcheance() : null)
        .nomEvenement(evenementielle ? tontine.getNomEvenement() : null)
        .montantLibre(evenementielle ? tontine.isMontantLibre() : null)
        .nombreMembresCotise(
            evenementielle ? cotisationRepository.countMembresAyantCotiseValide(tid) : null)
        .nombreMembres(nombreMembres)
        .cotisationsEnAttente(enAttente)
        .cotisationsEnRetard(enRetard)
        .montantTotalValide(montantValide)
        .cycleEnCours(cycleInfo)
        .gagnantsADesigner(gagnantsADesigner)
        .demandesEnAttente(
            demandeRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(
                tid, StatutDemande.EN_ATTENTE))
        .prioritesEnAttente(
            prioriteRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(
                tid, StatutPriorite.EN_ATTENTE))
        .build();
  }

  /** Tour EN_COURS : bénéficiaire éventuel et collecte (une requête groupée par statut). */
  private MyDashboardResponse.CycleEnCoursInfo cycleEnCoursInfo(
      Tontine tontine, CycleTontine cycle) {
    long payes = 0;
    long enAttenteTour = 0;
    BigDecimal collecte = BigDecimal.ZERO;
    BigDecimal montantEnAttente = BigDecimal.ZERO;
    for (Object[] row : cotisationRepository.countAndSumByStatutForCycle(cycle.getId())) {
      CotisationStatut statut = (CotisationStatut) row[0];
      if (statut == CotisationStatut.VALIDE) {
        payes = (Long) row[1];
        collecte = (BigDecimal) row[2];
      } else if (statut == CotisationStatut.EN_ATTENTE) {
        enAttenteTour = (Long) row[1];
        montantEnAttente = (BigDecimal) row[2];
      }
    }
    long actifs =
        membreRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(
            tontine.getId(), MembreStatut.ACTIF);
    BigDecimal parMembre = tontine.getMontantAttenduParMembre();
    User beneficiaire = premierBeneficiaire(cycle);

    return MyDashboardResponse.CycleEnCoursInfo.builder()
        .cycleId(cycle.getId())
        .numeroCycle(cycle.getNumeroCycle())
        .dateDebut(cycle.getDateDebut())
        .dateFin(cycle.getDateFin())
        .echeanceAt(Fuseau.versOffset(cycle.getEcheanceAt()))
        .beneficiaireNom(
            beneficiaire != null
                ? beneficiaire.getFirstName() + " " + beneficiaire.getLastName()
                : null)
        .beneficiaireUserId(beneficiaire != null ? beneficiaire.getId() : null)
        .montantCollecte(collecte)
        .montantEnAttente(montantEnAttente)
        .montantAttendu(parMembre != null ? parMembre.multiply(BigDecimal.valueOf(actifs)) : null)
        .membresAyantPaye(payes)
        .membresEnAttente(enAttenteTour)
        .membresActifs(actifs)
        .build();
  }

  /** Utilisateur du premier gagnant actif du cycle, ou null. */
  private static User premierBeneficiaire(CycleTontine cycle) {
    return cycle.getGagnants().stream()
        .filter(g -> g.getDeletedAt() == null)
        .findFirst()
        .map(g -> g.getMembre().getUser())
        .orElse(null);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  /**
   * Vérifie que l'appelant est SUPER_ADMIN et retourne l'entité User correspondante.
   *
   * @throws ForbiddenException si l'utilisateur n'est pas SUPER_ADMIN
   */
  private User assertSuperAdmin(String keycloakId) throws CustomException {
    User user = findUserByKeycloakId(keycloakId);
    if (!roleRepository.existsByUserIdAndRole(user.getId(), UserRole.SUPER_ADMIN)) {
      throw new ForbiddenException(ResponseMessageConstants.ADMIN_ACCESS_DENIED);
    }
    return user;
  }

  private User findUserByKeycloakId(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }

  private User findUserById(UUID userId) throws CustomException {
    return userRepository
        .findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }
}
