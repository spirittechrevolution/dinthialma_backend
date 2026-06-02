package com.africa.dinthialma_backend.contribution.service.impl;

import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.dto.AdminRecordCotisationRequest;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.RecordCotisationRequest;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.contribution.service.interfaces.CotisationService;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implémentation du service de gestion des cotisations. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CotisationServiceImpl implements CotisationService {

  private final CotisationRepository cotisationRepository;
  private final TontineRepository tontineRepository;
  private final CycleTontineRepository cycleRepository;
  private final TontineMembreRepository membreRepository;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository roleAssignmentRepository;
  private final AuditService auditService;
  private final WhatsappService whatsappService;

  // ─── Enregistrement ──────────────────────────────────────────────────────

  @Override
  public CotisationResponse recordCotisation(
      String keycloakId, UUID tontineId, RecordCotisationRequest request) throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);

    // L'appelant doit être cotisant dans cette tontine
    TontineMembre membre =
        membreRepository
            .findByTontine_IdAndUser_IdAndDeletedAtIsNull(tontineId, caller.getId())
            .orElseThrow(
                () -> new ForbiddenException("Vous n'êtes pas cotisant dans cette tontine"));

    CycleTontine cycle = findCycleById(tontineId, request.getCycleId());

    if (cycle.getStatut() != CycleStatut.EN_COURS) {
      throw new BadRequestException("Les cotisations ne sont acceptées que pour un cycle EN_COURS");
    }

    // Unicité cycle/membre
    if (cotisationRepository.existsByCycle_IdAndMembre_IdAndDeletedAtIsNull(
        cycle.getId(), membre.getId())) {
      throw new ConflictException("Vous avez déjà enregistré une cotisation pour ce cycle");
    }

    Cotisation cotisation =
        Cotisation.builder()
            .tontine(tontine)
            .membre(membre)
            .cycle(cycle)
            .montant(request.getMontant())
            .methodePaiement(request.getMethodePaiement())
            .referenceTransaction(request.getReferenceTransaction())
            .note(request.getNote())
            .enregistrePar(caller)
            .statut(CotisationStatut.EN_ATTENTE)
            .build();

    Cotisation saved = cotisationRepository.save(cotisation);
    auditService.logCreate(caller, "cotisations", saved.getId());
    log.info(
        "Cotisation enregistrée – tontineId={} cycleId={} membreId={} id={}",
        tontineId,
        cycle.getId(),
        membre.getId(),
        saved.getId());
    return CotisationResponse.from(saved);
  }

  // ─── Enregistrement admin (cash / PRE_ENROLLED) ──────────────────────────

  @Override
  public CotisationResponse adminRecordCotisation(
      String keycloakId, UUID tontineId, AdminRecordCotisationRequest request)
      throws CustomException {

    User admin = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(admin, tontine);

    TontineMembre membre =
        membreRepository
            .findByIdAndTontine_IdAndDeletedAtIsNull(request.getMembreId(), tontineId)
            .orElseThrow(() -> new NotFoundException("Membre introuvable dans cette tontine"));

    CycleTontine cycle = findCycleById(tontineId, request.getCycleId());

    if (cycle.getStatut() != CycleStatut.EN_COURS) {
      throw new BadRequestException("Les cotisations ne sont acceptées que pour un cycle EN_COURS");
    }

    if (cotisationRepository.existsByCycle_IdAndMembre_IdAndDeletedAtIsNull(
        cycle.getId(), membre.getId())) {
      throw new ConflictException("Une cotisation existe déjà pour ce membre sur ce cycle");
    }

    Cotisation cotisation =
        Cotisation.builder()
            .tontine(tontine)
            .membre(membre)
            .cycle(cycle)
            .montant(request.getMontant())
            .methodePaiement(request.getMethodePaiement())
            .referenceTransaction(request.getReferenceTransaction())
            .note(request.getNote())
            .enregistrePar(admin)
            .statut(CotisationStatut.VALIDE)
            .validePar(admin)
            .dateValidation(LocalDateTime.now())
            .build();

    Cotisation saved = cotisationRepository.save(cotisation);
    auditService.logCreate(admin, "cotisations", saved.getId());
    log.info(
        "Cotisation admin enregistrée+validée – tontineId={} cycleId={} membreId={} id={}",
        tontineId,
        cycle.getId(),
        membre.getId(),
        saved.getId());

    // Notification WhatsApp au membre si son compte est ACTIVE
    try {
      String phone = membre.getUser().getPhone();
      String montantStr = request.getMontant().toPlainString() + " FCFA";
      String ref = request.getReferenceTransaction();
      String msg =
          ref != null && !ref.isBlank()
              ? "✅ *Dinthialma* – Votre cotisation de "
                  + montantStr
                  + " pour la tontine *"
                  + tontine.getNom()
                  + "* a été enregistrée"
                  + " et validée.\nRéférence : "
                  + ref
              : "✅ *Dinthialma* – Votre cotisation de "
                  + montantStr
                  + " pour la tontine *"
                  + tontine.getNom()
                  + "* a été enregistrée et validée.";
      whatsappService.send(phone, msg);
    } catch (Exception e) {
      log.warn("Notification WhatsApp non envoyée (non bloquant) : {}", e.getMessage());
    }

    return CotisationResponse.from(saved);
  }

  // ─── Validation ──────────────────────────────────────────────────────────

  @Override
  public CotisationResponse validateCotisation(String keycloakId, UUID tontineId, UUID cotisationId)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    Cotisation cotisation =
        cotisationRepository
            .findByIdAndDeletedAtIsNull(cotisationId)
            .orElseThrow(
                () -> new NotFoundException(ResponseMessageConstants.CONTRIBUTION_NOT_FOUND));

    if (!cotisation.getTontine().getId().equals(tontineId)) {
      throw new NotFoundException(ResponseMessageConstants.CONTRIBUTION_NOT_FOUND);
    }

    if (cotisation.getStatut() != CotisationStatut.EN_ATTENTE) {
      throw new BadRequestException(
          "Seule une cotisation EN_ATTENTE peut être validée (statut actuel : "
              + cotisation.getStatut()
              + ")");
    }

    cotisation.setStatut(CotisationStatut.VALIDE);
    cotisation.setValidePar(caller);
    cotisation.setDateValidation(LocalDateTime.now());

    Cotisation saved = cotisationRepository.save(cotisation);
    auditService.log(
        caller,
        "cotisations",
        cotisationId,
        "statut",
        CotisationStatut.EN_ATTENTE.name(),
        CotisationStatut.VALIDE.name());
    log.info("Cotisation validée – cotisationId={} par userId={}", cotisationId, caller.getId());

    // Notification WhatsApp au membre
    try {
      String phone = saved.getMembre().getUser().getPhone();
      String montantStr = saved.getMontant().toPlainString() + " FCFA";
      String tontineName = saved.getTontine().getNom();
      String ref = saved.getReferenceTransaction();
      String msg =
          ref != null && !ref.isBlank()
              ? "✅ *Dinthialma* – Votre cotisation de "
                  + montantStr
                  + " pour la tontine *"
                  + tontineName
                  + "* a été validée.\nRéférence : "
                  + ref
              : "✅ *Dinthialma* – Votre cotisation de "
                  + montantStr
                  + " pour la tontine *"
                  + tontineName
                  + "* a été validée.";
      whatsappService.send(phone, msg);
    } catch (Exception e) {
      log.warn("Notification WhatsApp non envoyée (non bloquant) : {}", e.getMessage());
    }

    return CotisationResponse.from(saved);
  }

  // ─── Liste ───────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<CotisationResponse> listCotisations(
      String keycloakId, UUID tontineId, UUID cycleId, Pageable pageable) throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);

    boolean isAdminOrSuper =
        isSuperAdmin(caller) || tontine.getCreePar().getId().equals(caller.getId());

    if (isAdminOrSuper) {
      if (cycleId != null) {
        return cotisationRepository
            .findByCycle_IdAndDeletedAtIsNull(cycleId, pageable)
            .map(CotisationResponse::from);
      }
      return cotisationRepository
          .findByTontine_IdAndDeletedAtIsNull(tontineId, pageable)
          .map(CotisationResponse::from);
    }

    // Un membre ne voit que ses propres cotisations (filtre en base)
    TontineMembre membreCaller =
        membreRepository
            .findByTontine_IdAndUser_IdAndDeletedAtIsNull(tontineId, caller.getId())
            .orElseThrow(
                () -> new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED));

    if (cycleId != null) {
      return cotisationRepository
          .findByCycle_IdAndMembre_IdAndDeletedAtIsNull(cycleId, membreCaller.getId(), pageable)
          .map(CotisationResponse::from);
    }
    return cotisationRepository
        .findByTontine_IdAndMembre_IdAndDeletedAtIsNull(tontineId, membreCaller.getId(), pageable)
        .map(CotisationResponse::from);
  }

  // ─── Récupération unique ──────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public CotisationResponse getCotisation(String keycloakId, UUID tontineId, UUID cotisationId)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);

    Cotisation cotisation =
        cotisationRepository
            .findByIdAndDeletedAtIsNull(cotisationId)
            .orElseThrow(
                () -> new NotFoundException(ResponseMessageConstants.CONTRIBUTION_NOT_FOUND));

    if (!cotisation.getTontine().getId().equals(tontineId)) {
      throw new NotFoundException(ResponseMessageConstants.CONTRIBUTION_NOT_FOUND);
    }

    boolean isAdminOrSuper =
        isSuperAdmin(caller) || tontine.getCreePar().getId().equals(caller.getId());

    if (!isAdminOrSuper) {
      // Le membre ne voit que sa propre cotisation
      TontineMembre membreCaller =
          membreRepository
              .findByTontine_IdAndUser_IdAndDeletedAtIsNull(tontineId, caller.getId())
              .orElseThrow(
                  () -> new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED));

      if (!cotisation.getMembre().getId().equals(membreCaller.getId())) {
        throw new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED);
      }
    }

    return CotisationResponse.from(cotisation);
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

  private boolean isSuperAdmin(User user) {
    return roleAssignmentRepository.existsByUserIdAndRole(user.getId(), UserRole.SUPER_ADMIN);
  }
}
