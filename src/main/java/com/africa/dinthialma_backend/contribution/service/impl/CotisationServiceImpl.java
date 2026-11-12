package com.africa.dinthialma_backend.contribution.service.impl;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.util.CsvExport;
import com.africa.dinthialma_backend.common.util.CsvWriter;
import com.africa.dinthialma_backend.common.util.FichierExport;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
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
import com.africa.dinthialma_backend.contribution.repository.CotisationSpecifications;
import com.africa.dinthialma_backend.contribution.service.interfaces.CotisationService;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.dto.NotificationLiens;
import com.africa.dinthialma_backend.notification.service.WhatsappService;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implémentation du service de gestion des cotisations. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CotisationServiceImpl implements CotisationService {

  private final CotisationRepository cotisationRepository;
  private final CotisationMapper cotisationMapper;
  private final TontineRepository tontineRepository;
  private final CycleTontineRepository cycleRepository;
  private final TontineMembreRepository membreRepository;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository roleAssignmentRepository;
  private final AuditService auditService;
  private final WhatsappService whatsappService;
  private final NotificationService notificationService;

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
      throw new BadRequestException(
          "Les cotisations ne sont acceptées que pour un cycle EN_COURS",
          ErrorCode.NO_CYCLE_IN_PROGRESS);
    }

    // Unicité cycle/membre
    if (cotisationRepository.existsActiveByCycleAndMembre(cycle.getId(), membre.getId())) {
      throw new ConflictException(
          "Vous avez déjà enregistré une cotisation pour ce cycle",
          ErrorCode.COTISATION_ALREADY_EXISTS);
    }

    validateMontantCotisation(tontine, request.getMontant());

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

    String montantStr = request.getMontant().toPlainString() + " FCFA";
    String tontineName = tontine.getNom();
    int numCycle = cycle.getNumeroCycle();

    // Notif membre — confirmation de soumission
    try {
      whatsappService.send(
          caller.getPhone(),
          "📝 *Dinthialma* – Votre cotisation de "
              + montantStr
              + " pour la tontine *"
              + tontineName
              + "* (cycle "
              + numCycle
              + ")"
              + " a bien été soumise. En attente de validation par votre gestionnaire.");
    } catch (Exception e) {
      log.warn("Notif WA membre (soumission) non envoyée : {}", e.getMessage());
    }

    // Notification in-app membre — confirmation de soumission
    notificationService.notify(
        caller.getId(),
        NotificationType.COTISATION_SOUMISE,
        "Cotisation soumise",
        "Votre paiement de " + montantStr + " est en attente de validation · " + tontineName,
        tontineId,
        NotificationLiens.cotisation(saved));

    // Notification in-app admin — paiement reçu
    notificationService.notify(
        tontine.getCreePar().getId(),
        NotificationType.PAIEMENT_RECU,
        "Paiement reçu",
        caller.getFirstName()
            + " "
            + caller.getLastName()
            + " a payé "
            + montantStr
            + " · "
            + tontineName,
        tontineId,
        NotificationLiens.cotisation(saved));

    // Notif admin — nouvelle cotisation à valider
    try {
      String adminPhone = tontine.getCreePar().getPhone();
      String membreNom = caller.getFirstName() + " " + caller.getLastName();
      whatsappService.send(
          adminPhone,
          "💰 *Dinthialma* – "
              + membreNom
              + " ("
              + caller.getPhone()
              + ")"
              + " vient de déclarer une cotisation de "
              + montantStr
              + " pour la tontine *"
              + tontineName
              + "* (cycle "
              + numCycle
              + ")."
              + " Veuillez valider.");
    } catch (Exception e) {
      log.warn("Notif WA admin (nouvelle cotisation) non envoyée : {}", e.getMessage());
    }

    return cotisationMapper.toResponse(saved);
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
      throw new BadRequestException(
          "Les cotisations ne sont acceptées que pour un cycle EN_COURS",
          ErrorCode.NO_CYCLE_IN_PROGRESS);
    }

    if (cotisationRepository.existsActiveByCycleAndMembre(cycle.getId(), membre.getId())) {
      throw new ConflictException(
          "Une cotisation existe déjà pour ce membre sur ce cycle",
          ErrorCode.COTISATION_ALREADY_EXISTS);
    }

    validateMontantCotisation(tontine, request.getMontant());

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

    String membrePhone = membre.getUser().getPhone();
    String membreNom = membre.getUser().getFirstName() + " " + membre.getUser().getLastName();
    String montantStr = request.getMontant().toPlainString() + " FCFA";
    String tontineName = tontine.getNom();
    String ref = request.getReferenceTransaction();
    boolean isPreEnrolled = AccountStatus.PRE_ENROLLED == membre.getUser().getAccountStatus();
    int numCycle = cycle.getNumeroCycle();

    // Notification in-app membre — cotisation enregistrée et validée (sauf PRE_ENROLLED)
    if (!isPreEnrolled) {
      notificationService.notify(
          membre.getUser().getId(),
          NotificationType.COTISATION_VALIDEE,
          "Cotisation validée",
          "Votre cotisation de " + montantStr + " a été enregistrée · " + tontineName,
          tontineId,
          NotificationLiens.cotisation(saved));
    }

    // Notif membre — message adapté selon le statut du compte
    try {
      String membreMsg =
          isPreEnrolled
              ? "✅ *Dinthialma* – Votre gestionnaire a enregistré votre cotisation de "
                  + montantStr
                  + " pour la tontine *"
                  + tontineName
                  + "* (cycle "
                  + numCycle
                  + ")."
                  + "\nInscrivez-vous sur Dinthialma pour suivre vos cotisations en temps réel."
              : ref != null && !ref.isBlank()
                  ? "✅ *Dinthialma* – Votre cotisation de "
                      + montantStr
                      + " pour la tontine *"
                      + tontineName
                      + "* (cycle "
                      + numCycle
                      + ")"
                      + " a été enregistrée et validée.\nRéférence : "
                      + ref
                  : "✅ *Dinthialma* – Votre cotisation de "
                      + montantStr
                      + " pour la tontine *"
                      + tontineName
                      + "* (cycle "
                      + numCycle
                      + ")"
                      + " a été enregistrée et validée.";
      whatsappService.send(membrePhone, membreMsg);
    } catch (Exception e) {
      log.warn("Notif WA membre (admin-record) non envoyée : {}", e.getMessage());
    }

    // Notif admin — confirmation de l'enregistrement
    try {
      whatsappService.send(
          tontine.getCreePar().getPhone(),
          "✅ *Dinthialma* – Vous avez enregistré la cotisation de "
              + montantStr
              + " de "
              + membreNom
              + " pour la tontine *"
              + tontineName
              + "* (cycle "
              + numCycle
              + ").");
    } catch (Exception e) {
      log.warn("Notif WA admin (admin-record) non envoyée : {}", e.getMessage());
    }

    return cotisationMapper.toResponse(saved);
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
              + ")",
          ErrorCode.COTISATION_NOT_PENDING);
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

    // Notification in-app membre — cotisation validée
    notificationService.notify(
        saved.getMembre().getUser().getId(),
        NotificationType.COTISATION_VALIDEE,
        "Cotisation validée",
        "Votre paiement de "
            + saved.getMontant().toPlainString()
            + " FCFA a été validé · "
            + saved.getTontine().getNom(),
        tontineId,
        NotificationLiens.cotisation(saved));

    String memberPhone = saved.getMembre().getUser().getPhone();
    String memberNom =
        saved.getMembre().getUser().getFirstName()
            + " "
            + saved.getMembre().getUser().getLastName();
    String montantStr = saved.getMontant().toPlainString() + " FCFA";
    String tontineName = saved.getTontine().getNom();
    String ref = saved.getReferenceTransaction();
    int numCycle = saved.getCycle().getNumeroCycle();

    // Notif membre — cotisation validée
    try {
      String msg =
          ref != null && !ref.isBlank()
              ? "✅ *Dinthialma* – Votre cotisation de "
                  + montantStr
                  + " pour la tontine *"
                  + tontineName
                  + "* (cycle "
                  + numCycle
                  + ")"
                  + " a été validée.\nRéférence : "
                  + ref
              : "✅ *Dinthialma* – Votre cotisation de "
                  + montantStr
                  + " pour la tontine *"
                  + tontineName
                  + "* (cycle "
                  + numCycle
                  + ")"
                  + " a été validée.";
      whatsappService.send(memberPhone, msg);
    } catch (Exception e) {
      log.warn("Notif WA membre (validation) non envoyée : {}", e.getMessage());
    }

    // Notif admin — confirmation de validation
    try {
      whatsappService.send(
          saved.getTontine().getCreePar().getPhone(),
          "✅ *Dinthialma* – Vous avez validé la cotisation de "
              + montantStr
              + " de "
              + memberNom
              + " pour la tontine *"
              + tontineName
              + "* (cycle "
              + numCycle
              + ").");
    } catch (Exception e) {
      log.warn("Notif WA admin (validation) non envoyée : {}", e.getMessage());
    }

    return cotisationMapper.toResponse(saved);
  }

  // ─── Refus ───────────────────────────────────────────────────────────────

  @Override
  public CotisationResponse refuseCotisation(
      String keycloakId, UUID tontineId, UUID cotisationId, RefuseCotisationRequest request)
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
          "Seule une cotisation EN_ATTENTE peut être refusée (statut actuel : "
              + cotisation.getStatut()
              + ")",
          ErrorCode.COTISATION_NOT_PENDING);
    }

    String motif = request.getMotif().trim();
    cotisation.setStatut(CotisationStatut.REFUSE);
    cotisation.setMotifRefus(motif);
    cotisation.setRefusePar(caller);
    cotisation.setDateRefus(LocalDateTime.now());

    Cotisation saved = cotisationRepository.save(cotisation);
    auditService.log(
        caller,
        "cotisations",
        cotisationId,
        "statut",
        CotisationStatut.EN_ATTENTE.name(),
        CotisationStatut.REFUSE.name());
    log.info("Cotisation refusée – cotisationId={} par userId={}", cotisationId, caller.getId());

    String montantStr = saved.getMontant().toPlainString() + " FCFA";
    String tontineName = saved.getTontine().getNom();
    int numCycle = saved.getCycle().getNumeroCycle();

    // Notification in-app membre — cotisation refusée (non bloquant)
    notificationService.notify(
        saved.getMembre().getUser().getId(),
        NotificationType.COTISATION_REFUSEE,
        "Cotisation refusée",
        "Votre paiement de "
            + montantStr
            + " a été refusé · "
            + tontineName
            + " · Motif : "
            + motif
            + ". Vous pouvez déclarer un nouveau paiement.",
        tontineId,
        NotificationLiens.cotisation(saved));

    // WhatsApp membre (non bloquant ; un pré-inscrit n'a pas forcément l'application)
    try {
      whatsappService.send(
          saved.getMembre().getUser().getPhone(),
          "❌ *Dinthialma* – Votre cotisation de "
              + montantStr
              + " pour la tontine *"
              + tontineName
              + "* (cycle "
              + numCycle
              + ") a été refusée.\nMotif : "
              + motif
              + "\nVous pouvez déclarer un nouveau paiement.");
    } catch (Exception e) {
      log.warn("Notif WA membre (refus) non envoyée : {}", e.getMessage());
    }

    return cotisationMapper.toResponse(saved);
  }

  // ─── Liste ───────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<CotisationResponse> listCotisations(
      String keycloakId,
      UUID tontineId,
      UUID cycleId,
      UUID membreId,
      List<CotisationStatut> statuts,
      String q,
      Pageable pageable)
      throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);

    boolean isAdminOrSuper =
        isSuperAdmin(caller) || tontine.getCreePar().getId().equals(caller.getId());

    // Un membre ne voit que ses propres cotisations : le filtre membreId est forcé sur lui
    UUID membreFiltre = membreId;
    if (!isAdminOrSuper) {
      TontineMembre membreCaller =
          membreRepository
              .findByTontine_IdAndUser_IdAndDeletedAtIsNull(tontineId, caller.getId())
              .orElseThrow(
                  () -> new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED));
      membreFiltre = membreCaller.getId();
    }

    // Le filtre tontine est toujours appliqué : un cycleId d'une autre tontine ne renvoie rien
    Specification<Cotisation> spec =
        Specification.allOf(
            CotisationSpecifications.nonSupprimee(),
            CotisationSpecifications.deTontine(tontineId),
            CotisationSpecifications.deCycle(cycleId),
            CotisationSpecifications.deMembre(membreFiltre),
            CotisationSpecifications.statutParmi(statuts),
            CotisationSpecifications.recherche(q));

    return cotisationRepository.findAll(spec, pageable).map(cotisationMapper::toResponse);
  }

  // ─── Export CSV (B24) ─────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public FichierExport exportCotisations(
      String keycloakId,
      UUID tontineId,
      String format,
      UUID cycleId,
      List<CotisationStatut> statuts)
      throws CustomException {
    CsvExport.verifierFormat(format);
    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    Specification<Cotisation> spec =
        Specification.allOf(
            CotisationSpecifications.nonSupprimee(),
            CotisationSpecifications.deTontine(tontineId),
            CotisationSpecifications.deCycle(cycleId),
            CotisationSpecifications.statutParmi(statuts));
    List<Cotisation> cotisations =
        cotisationRepository
            .findAll(
                spec,
                PageRequest.of(
                    0, CsvWriter.MAX_LIGNES, Sort.by("cycle.numeroCycle", "createdAt").ascending()))
            .getContent();

    CsvWriter csv =
        new CsvWriter()
            .ligne(
                "Tour",
                "Membre",
                "Téléphone",
                "Montant (FCFA)",
                "Statut",
                "Méthode",
                "Référence",
                "Déclarée le",
                "Validée le",
                "Motif refus",
                "Note");
    for (Cotisation c : cotisations) {
      var u = c.getMembre().getUser();
      csv.ligne(
          c.getCycle().getNumeroCycle(),
          (u.getFirstName() + " " + u.getLastName()).trim(),
          u.getPhone(),
          c.getMontant() != null ? c.getMontant().toPlainString() : null,
          c.getStatut(),
          c.getMethodePaiement(),
          c.getReferenceTransaction(),
          c.getCreatedAt(),
          c.getDateValidation(),
          c.getMotifRefus(),
          c.getNote());
    }
    return FichierExport.csv("cotisations-" + tontineId + ".csv", csv.toBytes());
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

    return cotisationMapper.toResponse(cotisation);
  }

  // ─── Modification ────────────────────────────────────────────────────────

  @Override
  public CotisationResponse updateCotisation(
      String keycloakId, UUID tontineId, UUID cotisationId, UpdateCotisationRequest request)
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

    // EN_RETARD → cycle fermé ; REFUSE → statut final : modification interdite
    if (cotisation.getStatut() == CotisationStatut.EN_RETARD
        || cotisation.getStatut() == CotisationStatut.REFUSE) {
      throw new BadRequestException(
          ResponseMessageConstants.CONTRIBUTION_CANNOT_EDIT, ErrorCode.COTISATION_NOT_EDITABLE);
    }

    // VALIDE + cycle TERMINE → jackpot déjà calculé, modification interdite
    if (cotisation.getStatut() == CotisationStatut.VALIDE
        && cotisation.getCycle().getStatut() == CycleStatut.TERMINE) {
      throw new BadRequestException(
          ResponseMessageConstants.CONTRIBUTION_CANNOT_EDIT, ErrorCode.COTISATION_NOT_EDITABLE);
    }

    if (request.getMontant() != null
        && request.getMontant().compareTo(cotisation.getMontant()) != 0) {
      validateMontantCotisation(tontine, request.getMontant());
      auditService.log(
          caller,
          "cotisations",
          cotisationId,
          "montant",
          cotisation.getMontant().toPlainString(),
          request.getMontant().toPlainString());
      cotisation.setMontant(request.getMontant());
    }

    if (request.getMethodePaiement() != null) {
      auditService.log(
          caller,
          "cotisations",
          cotisationId,
          "methode_paiement",
          cotisation.getMethodePaiement(),
          request.getMethodePaiement());
      cotisation.setMethodePaiement(request.getMethodePaiement());
    }

    if (request.getReferenceTransaction() != null) {
      auditService.log(
          caller,
          "cotisations",
          cotisationId,
          "reference_transaction",
          cotisation.getReferenceTransaction(),
          request.getReferenceTransaction());
      cotisation.setReferenceTransaction(request.getReferenceTransaction());
    }

    if (request.getNote() != null) {
      cotisation.setNote(request.getNote());
    }

    Cotisation saved = cotisationRepository.save(cotisation);
    log.info(
        "Cotisation modifiée par admin – cotisationId={} par userId={}",
        cotisationId,
        caller.getId());

    return cotisationMapper.toResponse(saved);
  }

  // ─── Récapitulatif par membre ─────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<CycleRecapCotisationResponse> getCycleRecap(
      String keycloakId, UUID tontineId, UUID cycleId, Pageable pageable) throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    // Vérifie que le cycle appartient bien à la tontine
    findCycleById(tontineId, cycleId);

    Page<TontineMembre> membres =
        membreRepository.findByTontine_IdAndStatutNotAndDeletedAtIsNull(
            tontineId, MembreStatut.SORTI, pageable);
    if (membres.isEmpty()) {
      return membres.map(m -> cotisationMapper.toRecap(m, null));
    }

    List<UUID> membreIds = membres.getContent().stream().map(TontineMembre::getId).toList();
    Map<UUID, Cotisation> cotisationByMembre =
        cotisationRepository
            .findByCycle_IdAndMembre_IdInAndDeletedAtIsNull(cycleId, membreIds)
            .stream()
            .collect(
                Collectors.toMap(c -> c.getMembre().getId(), c -> c, Cotisation::plusPertinente));

    return membres.map(m -> cotisationMapper.toRecap(m, cotisationByMembre.get(m.getId())));
  }

  // ─── Récapitulatif total par membre ───────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<MembreTotalCotisationResponse> getTontineRecapTotal(
      String keycloakId, UUID tontineId, Pageable pageable) throws CustomException {

    User caller = findUserByKeycloakId(keycloakId);
    Tontine tontine = findTontineById(tontineId);
    assertIsCreatorOrSuperAdmin(caller, tontine);

    Page<TontineMembre> membres =
        membreRepository.findByTontine_IdAndStatutNotAndDeletedAtIsNull(
            tontineId, MembreStatut.SORTI, pageable);
    if (membres.isEmpty()) {
      return membres.map(m -> cotisationMapper.toTotal(m, BigDecimal.ZERO, 0L));
    }

    List<UUID> membreIds = membres.getContent().stream().map(TontineMembre::getId).toList();
    Map<UUID, Object[]> totauxByMembre =
        cotisationRepository.sumAndCountValideGroupByMembreIn(tontineId, membreIds).stream()
            .collect(Collectors.toMap(row -> (UUID) row[0], row -> row));

    return membres.map(
        m -> {
          Object[] row = totauxByMembre.get(m.getId());
          BigDecimal total = row != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
          long nombre = row != null ? (Long) row[2] : 0L;
          return cotisationMapper.toTotal(m, total, nombre);
        });
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

  /**
   * Valide le montant d'une cotisation selon le type de tontine.
   *
   * <ul>
   *   <li>EVENEMENTIELLE + montantLibre=false → montant doit être exactement égal au montant fixe.
   *   <li>EVENEMENTIELLE + montantLibre=true → montant >= montantMinimum si défini.
   *   <li>ROTATIVE → aucune validation ici (montant libre en pratique).
   * </ul>
   */
  private void validateMontantCotisation(Tontine tontine, BigDecimal montant)
      throws CustomException {
    if (tontine.getTontineType() != TontineType.EVENEMENTIELLE) return;

    if (!tontine.isMontantLibre()) {
      if (tontine.getMontant() != null && montant.compareTo(tontine.getMontant()) != 0) {
        throw new BadRequestException(
            "Le montant doit être exactement " + tontine.getMontant().toPlainString() + " FCFA",
            ErrorCode.AMOUNT_MISMATCH);
      }
    } else if (tontine.getMontantMinimum() != null
        && montant.compareTo(tontine.getMontantMinimum()) < 0) {
      throw new BadRequestException(
          "Le montant minimum de cotisation est "
              + tontine.getMontantMinimum().toPlainString()
              + " FCFA",
          ErrorCode.AMOUNT_BELOW_MINIMUM);
    }
  }
}
