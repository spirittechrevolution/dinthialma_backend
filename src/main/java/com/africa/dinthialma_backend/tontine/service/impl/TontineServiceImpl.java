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
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.dto.CreateTontineRequest;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import com.africa.dinthialma_backend.tontine.dto.UpdateTontineRequest;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import com.africa.dinthialma_backend.tontine.service.interfaces.TontineService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
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
public class TontineServiceImpl implements TontineService {

  private final TontineRepository tontineRepository;
  private final CycleTontineRepository cycleRepository;
  private final TontineMembreRepository membreRepository;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository roleAssignmentRepository;
  private final KeycloakAuthService keycloakAuthService;
  private final AuditService auditService;

  // ─── Création ────────────────────────────────────────────────────────────

  @Override
  public TontineResponse createTontine(String keycloakId, CreateTontineRequest request)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    TontineType type =
        request.getTontineType() != null ? request.getTontineType() : TontineType.ROTATIVE;

    validateCreateRequest(request, type);

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
    assignRoleIfAbsent(caller, UserRole.ADMIN);
    auditService.logCreate(caller, "tontines", saved.getId());
    log.info("Tontine créée – id={} type={} par userId={}", saved.getId(), type, caller.getId());
    return TontineResponse.from(saved, 0);
  }

  // ─── Lecture ─────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public TontineResponse getTontine(String keycloakId, UUID id) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(id);

    assertCanAccess(caller, tontine);

    int nombreMembres = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(id);
    return TontineResponse.from(tontine, nombreMembres);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<TontineResponse> listTontines(String keycloakId, Pageable pageable)
      throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);

    Page<Tontine> tontines;
    if (isSuperAdmin(caller)) {
      tontines = tontineRepository.findByDeletedAtIsNull(pageable);
    } else {
      tontines = tontineRepository.findVisibleByUserId(caller.getId(), pageable);
    }

    return tontines.map(
        t -> {
          int count = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(t.getId());
          return TontineResponse.from(t, count);
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

    if (request.getNom() != null) {
      auditService.log(caller, "tontines", id, "nom", tontine.getNom(), request.getNom().trim());
      tontine.setNom(request.getNom().trim());
    }
    if (request.getDescription() != null) {
      auditService.log(
          caller,
          "tontines",
          id,
          "description",
          tontine.getDescription(),
          request.getDescription());
      tontine.setDescription(request.getDescription());
    }
    if (request.getMontant() != null) {
      auditService.log(
          caller,
          "tontines",
          id,
          "montant",
          tontine.getMontant().toPlainString(),
          request.getMontant().toPlainString());
      tontine.setMontant(request.getMontant());
    }
    if (request.getFrequence() != null) {
      auditService.log(
          caller, "tontines", id, "frequence", tontine.getFrequence(), request.getFrequence());
      tontine.setFrequence(request.getFrequence());
    }
    if (request.getOrdreBeneficiaire() != null) {
      auditService.log(
          caller,
          "tontines",
          id,
          "ordreBeneficiaire",
          tontine.getOrdreBeneficiaire(),
          request.getOrdreBeneficiaire());
      tontine.setOrdreBeneficiaire(request.getOrdreBeneficiaire());
    }
    if (request.getDateDebut() != null) {
      auditService.log(
          caller,
          "tontines",
          id,
          "dateDebut",
          tontine.getDateDebut().toString(),
          request.getDateDebut().toString());
      tontine.setDateDebut(request.getDateDebut());
    }
    if (request.getNombreMembres() != null) {
      auditService.log(
          caller,
          "tontines",
          id,
          "nombreMembres",
          String.valueOf(tontine.getNombreMembres()),
          String.valueOf(request.getNombreMembres()));
      tontine.setNombreMembres(request.getNombreMembres());
    }
    if (request.getNombreGagnants() != null) {
      auditService.log(
          caller,
          "tontines",
          id,
          "nombreGagnants",
          String.valueOf(tontine.getNombreGagnants()),
          String.valueOf(request.getNombreGagnants()));
      tontine.setNombreGagnants(request.getNombreGagnants());
    }

    // Champs EVENEMENTIELLE
    if (tontine.getTontineType() == TontineType.EVENEMENTIELLE) {
      if (request.getDateEcheance() != null) {
        if (!request.getDateEcheance().isAfter(tontine.getDateDebut())) {
          throw new BadRequestException(
              "La date d'échéance doit être postérieure à la date de début");
        }
        auditService.log(
            caller,
            "tontines",
            id,
            "dateEcheance",
            tontine.getDateEcheance() != null ? tontine.getDateEcheance().toString() : null,
            request.getDateEcheance().toString());
        tontine.setDateEcheance(request.getDateEcheance());
      }
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

    Tontine saved = tontineRepository.save(tontine);
    int nombreMembres = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(id);
    return TontineResponse.from(saved, nombreMembres);
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
    int nombreMembres = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(id);
    auditService.log(caller, "tontines", id, "statut", oldStatut, TontineStatut.ACTIVE.name());
    log.info("Tontine activée – id={} type={}", id, tontine.getTontineType());
    return TontineResponse.from(saved, nombreMembres);
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
    int nombreMembres = (int) membreRepository.countByTontine_IdAndDeletedAtIsNull(id);
    auditService.log(
        caller,
        "tontines",
        id,
        "statut",
        TontineStatut.ACTIVE.name(),
        TontineStatut.SUSPENDUE.name());
    log.info("Tontine suspendue – id={}", id);
    return TontineResponse.from(saved, nombreMembres);
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
      if (request.getDateEcheance() == null) {
        throw new BadRequestException(
            "La date d'échéance est obligatoire pour une tontine événementielle");
      }
      if (!request.getDateEcheance().isAfter(request.getDateDebut())) {
        throw new BadRequestException(
            "La date d'échéance doit être postérieure à la date de début");
      }
      if (!request.isMontantLibre()
          && (request.getMontant() == null
              || request.getMontant().compareTo(BigDecimal.ZERO) <= 0)) {
        throw new BadRequestException(
            "Le montant fixe de cotisation est obligatoire quand montantLibre=false");
      }
    } else {
      // ROTATIVE
      if (request.getOrdreBeneficiaire() == null || request.getOrdreBeneficiaire().isBlank()) {
        throw new BadRequestException(
            "L'ordre des bénéficiaires est obligatoire pour une tontine rotative");
      }
      if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
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
