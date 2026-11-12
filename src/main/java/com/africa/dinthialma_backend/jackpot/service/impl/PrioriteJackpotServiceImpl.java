package com.africa.dinthialma_backend.jackpot.service.impl;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.audit.AuditService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.security.AccessControl;
import com.africa.dinthialma_backend.jackpot.codeList.OriginePriorite;
import com.africa.dinthialma_backend.jackpot.codeList.StatutPriorite;
import com.africa.dinthialma_backend.jackpot.dto.PrioriteJackpotResponse;
import com.africa.dinthialma_backend.jackpot.dto.PrioriteRequests;
import com.africa.dinthialma_backend.jackpot.entity.PrioriteJackpot;
import com.africa.dinthialma_backend.jackpot.mapper.PrioriteJackpotMapper;
import com.africa.dinthialma_backend.jackpot.repository.PrioriteJackpotRepository;
import com.africa.dinthialma_backend.jackpot.service.interfaces.PrioriteJackpotService;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.dto.NotificationLiens;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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
public class PrioriteJackpotServiceImpl implements PrioriteJackpotService {

  private static final Set<StatutPriorite> ACTIVES =
      Set.of(StatutPriorite.EN_ATTENTE, StatutPriorite.ACCEPTEE);
  private static final String TABLE = "priorites_jackpot";

  private final PrioriteJackpotRepository prioriteRepository;
  private final PrioriteJackpotMapper prioriteMapper;
  private final TontineRepository tontineRepository;
  private final TontineMembreRepository membreRepository;
  private final AccessControl accessControl;
  private final NotificationService notificationService;
  private final AuditService auditService;

  // ─── Demande du membre ───────────────────────────────────────────────────

  @Override
  public PrioriteJackpotResponse demander(
      String keycloakId, UUID tontineId, PrioriteRequests.Demander request) throws CustomException {
    User caller = accessControl.utilisateur(keycloakId);
    Tontine tontine = tontineOuverteAuxPriorites(tontineId);
    TontineMembre membre =
        membreRepository
            .findByTontine_IdAndUser_IdAndDeletedAtIsNull(tontineId, caller.getId())
            .orElseThrow(
                () -> new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED));
    verifierNouvellePriorite(membre);

    PrioriteJackpot priorite =
        prioriteRepository.save(
            PrioriteJackpot.builder()
                .tontine(tontine)
                .membre(membre)
                .statut(StatutPriorite.EN_ATTENTE)
                .origine(OriginePriorite.DEMANDE_MEMBRE)
                .motif(texte(request != null ? request.getMotif() : null))
                .build());

    notificationService.notify(
        tontine.getCreePar().getId(),
        NotificationType.PRIORITE_JACKPOT_DEMANDEE,
        "Demande de priorité sur le jackpot",
        nomComplet(membre) + " demande à recevoir le prochain jackpot · " + tontine.getNom(),
        tontineId,
        liens(priorite));
    log.info("Priorité demandée – tontineId={} membreId={}", tontineId, membre.getId());
    return prioriteMapper.toResponse(priorite);
  }

  // ─── Décisions du gestionnaire ───────────────────────────────────────────

  @Override
  public PrioriteJackpotResponse attribuer(
      String keycloakId, UUID tontineId, PrioriteRequests.Attribuer request)
      throws CustomException {
    User caller = accessControl.utilisateur(keycloakId);
    Tontine tontine = tontineOuverteAuxPriorites(tontineId);
    assertGestionnaire(caller, tontine);
    TontineMembre membre =
        membreRepository
            .findByIdAndTontine_IdAndDeletedAtIsNull(request.getMembreId(), tontineId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.MEMBER_NOT_FOUND));
    verifierNouvellePriorite(membre);

    PrioriteJackpot priorite =
        PrioriteJackpot.builder()
            .tontine(tontine)
            .membre(membre)
            .origine(OriginePriorite.GESTIONNAIRE)
            .motif(texte(request.getMotif()))
            .build();
    return accorder(caller, priorite);
  }

  @Override
  public PrioriteJackpotResponse accepter(String keycloakId, UUID tontineId, UUID prioriteId)
      throws CustomException {
    User caller = accessControl.utilisateur(keycloakId);
    PrioriteJackpot priorite = prioriteEnAttente(caller, tontineId, prioriteId);
    tontineOuverteAuxPriorites(tontineId);
    verifierEligible(priorite.getMembre());
    return accorder(caller, priorite);
  }

  @Override
  public PrioriteJackpotResponse refuser(
      String keycloakId, UUID tontineId, UUID prioriteId, PrioriteRequests.Refuser request)
      throws CustomException {
    User caller = accessControl.utilisateur(keycloakId);
    PrioriteJackpot priorite = prioriteEnAttente(caller, tontineId, prioriteId);
    String motif = texte(request != null ? request.getMotif() : null);
    cloturer(caller, priorite, StatutPriorite.REFUSEE, motif);

    notificationService.notify(
        priorite.getMembre().getUser().getId(),
        NotificationType.PRIORITE_JACKPOT_REFUSEE,
        "Demande de priorité refusée",
        "Votre demande de priorité sur le jackpot de "
            + priorite.getTontine().getNom()
            + " a été refusée"
            + (motif != null ? " · Motif : " + motif : "")
            + ".",
        tontineId,
        liens(priorite));
    log.info("Priorité refusée – prioriteId={}", prioriteId);
    return prioriteMapper.toResponse(priorite);
  }

  @Override
  public PrioriteJackpotResponse annuler(String keycloakId, UUID tontineId, UUID prioriteId)
      throws CustomException {
    User caller = accessControl.utilisateur(keycloakId);
    PrioriteJackpot priorite = trouver(tontineId, prioriteId);
    boolean gestionnaire = estGestionnaire(caller, priorite.getTontine());
    boolean concerne = priorite.getMembre().getUser().getId().equals(caller.getId());
    if (!gestionnaire && !concerne) {
      throw new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED);
    }
    if (!priorite.getStatut().estActive()) {
      throw new BadRequestException(
          "Cette priorité n'est plus active (statut : " + priorite.getStatut() + ")",
          ErrorCode.PRIORITE_NOT_PENDING);
    }
    cloturer(caller, priorite, StatutPriorite.ANNULEE, null);

    if (!concerne) {
      notificationService.notify(
          priorite.getMembre().getUser().getId(),
          NotificationType.PRIORITE_JACKPOT_REFUSEE,
          "Priorité sur le jackpot annulée",
          "Le gestionnaire a annulé votre priorité sur le jackpot de "
              + priorite.getTontine().getNom()
              + ".",
          tontineId,
          liens(priorite));
    }
    log.info("Priorité annulée – prioriteId={} par userId={}", prioriteId, caller.getId());
    return prioriteMapper.toResponse(priorite);
  }

  // ─── Lecture ─────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<PrioriteJackpotResponse> lister(
      String keycloakId, UUID tontineId, StatutPriorite statut, Pageable pageable)
      throws CustomException {
    User caller = accessControl.utilisateur(keycloakId);
    Tontine tontine =
        tontineRepository
            .findByIdAndDeletedAtIsNull(tontineId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.TONTINE_NOT_FOUND));
    boolean gestionnaire = estGestionnaire(caller, tontine);
    if (!gestionnaire
        && !membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
            tontineId, caller.getId())) {
      throw new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED);
    }
    return prioriteRepository
        .findByTontine(tontineId, statut, pageable)
        .map(p -> vue(p, caller, gestionnaire));
  }

  // ─── Désignation des gagnants ────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<TontineMembre> prioritairesEligibles(UUID tontineId) {
    return prioriteRepository
        .findByTontine_IdAndStatutAndDeletedAtIsNullOrderByDateTraitementAscCreatedAtAsc(
            tontineId, StatutPriorite.ACCEPTEE)
        .stream()
        .map(PrioriteJackpot::getMembre)
        .filter(PrioriteJackpotServiceImpl::estEligible)
        .toList();
  }

  @Override
  public void marquerServies(CycleTontine cycle, Collection<TontineMembre> gagnants) {
    if (gagnants.isEmpty()) {
      return;
    }
    List<UUID> membreIds = gagnants.stream().map(TontineMembre::getId).toList();
    for (PrioriteJackpot p :
        prioriteRepository.findByMembre_IdInAndStatutInAndDeletedAtIsNull(membreIds, ACTIVES)) {
      p.setStatut(StatutPriorite.SERVIE);
      p.setCycle(cycle);
      prioriteRepository.save(p);
      log.info("Priorité servie – prioriteId={} cycleId={}", p.getId(), cycle.getId());
    }
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  /** Passe la priorité à ACCEPTEE et prévient le membre puis, par transparence, les autres. */
  private PrioriteJackpotResponse accorder(User caller, PrioriteJackpot priorite) {
    priorite.setStatut(StatutPriorite.ACCEPTEE);
    priorite.setTraiteePar(caller);
    priorite.setDateTraitement(LocalDateTime.now());
    PrioriteJackpot saved = prioriteRepository.save(priorite);
    auditService.log(caller, TABLE, saved.getId(), "statut", null, StatutPriorite.ACCEPTEE.name());

    Tontine tontine = saved.getTontine();
    TontineMembre beneficiaire = saved.getMembre();
    for (TontineMembre m :
        membreRepository.findByTontine_IdAndStatutAndDeletedAtIsNull(
            tontine.getId(), MembreStatut.ACTIF)) {
      boolean estBeneficiaire = m.getId().equals(beneficiaire.getId());
      notificationService.notify(
          m.getUser().getId(),
          NotificationType.PRIORITE_JACKPOT_ACCORDEE,
          estBeneficiaire ? "Priorité accordée 🙏" : "Priorité sur le prochain jackpot",
          estBeneficiaire
              ? "Vous recevrez le prochain jackpot de " + tontine.getNom() + "."
              : nomComplet(beneficiaire)
                  + " recevra en priorité le prochain jackpot de "
                  + tontine.getNom()
                  + ". L'ordre convenu reprend ensuite.",
          tontine.getId(),
          liens(saved));
    }
    log.info("Priorité accordée – tontineId={} membreId={}", tontine.getId(), beneficiaire.getId());
    return prioriteMapper.toResponse(saved);
  }

  private void cloturer(User caller, PrioriteJackpot p, StatutPriorite statut, String motif) {
    String ancien = p.getStatut().name();
    p.setStatut(statut);
    p.setMotifRefus(motif);
    p.setTraiteePar(caller);
    p.setDateTraitement(LocalDateTime.now());
    prioriteRepository.save(p);
    auditService.log(caller, TABLE, p.getId(), "statut", ancien, statut.name());
  }

  /** Tontine ROTATIVE ACTIVE ou SUSPENDUE (les tours sont en cours). */
  private Tontine tontineOuverteAuxPriorites(UUID tontineId) throws CustomException {
    Tontine tontine =
        tontineRepository
            .findByIdAndDeletedAtIsNull(tontineId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.TONTINE_NOT_FOUND));
    boolean enCours =
        tontine.getStatut() == TontineStatut.ACTIVE
            || tontine.getStatut() == TontineStatut.SUSPENDUE;
    if (tontine.getTontineType() != TontineType.ROTATIVE || !enCours) {
      throw new BadRequestException(
          "Les priorités de jackpot concernent les tontines rotatives en cours (ACTIVE ou"
              + " SUSPENDUE)",
          ErrorCode.PRIORITE_NON_DISPONIBLE);
    }
    return tontine;
  }

  /** Nouvelle priorité : membre éligible et sans autre priorité active. */
  private void verifierNouvellePriorite(TontineMembre membre) throws CustomException {
    verifierEligible(membre);
    if (prioriteRepository.existsByMembre_IdAndStatutInAndDeletedAtIsNull(
        membre.getId(), ACTIVES)) {
      throw new ConflictException(
          "Une priorité est déjà en attente ou accordée pour ce membre",
          ErrorCode.PRIORITE_DEJA_ACTIVE);
    }
  }

  /** Membre ACTIF n'ayant pas encore reçu de jackpot. */
  private static void verifierEligible(TontineMembre membre) throws CustomException {
    if (!estEligible(membre)) {
      throw new BadRequestException(
          membre.getUser().getFirstName()
              + " doit être un membre actif n'ayant pas encore reçu de jackpot",
          ErrorCode.MEMBRE_NON_ELIGIBLE_JACKPOT);
    }
  }

  private static boolean estEligible(TontineMembre membre) {
    return membre.getStatut() == MembreStatut.ACTIF && !membre.isARecuJackpot();
  }

  private PrioriteJackpot prioriteEnAttente(User caller, UUID tontineId, UUID prioriteId)
      throws CustomException {
    PrioriteJackpot priorite = trouver(tontineId, prioriteId);
    assertGestionnaire(caller, priorite.getTontine());
    if (priorite.getStatut() != StatutPriorite.EN_ATTENTE) {
      throw new BadRequestException(
          "Cette demande a déjà été traitée (statut : " + priorite.getStatut() + ")",
          ErrorCode.PRIORITE_NOT_PENDING);
    }
    return priorite;
  }

  private PrioriteJackpot trouver(UUID tontineId, UUID prioriteId) throws CustomException {
    return prioriteRepository
        .findByIdAndTontine_IdAndDeletedAtIsNull(prioriteId, tontineId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.PRIORITE_NOT_FOUND));
  }

  private boolean estGestionnaire(User caller, Tontine tontine) {
    return tontine.getCreePar().getId().equals(caller.getId())
        || accessControl.estSuperAdmin(caller);
  }

  private void assertGestionnaire(User caller, Tontine tontine) throws CustomException {
    if (!estGestionnaire(caller, tontine)) {
      throw new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED);
    }
  }

  private PrioriteJackpotResponse vue(PrioriteJackpot p, User caller, boolean gestionnaire) {
    boolean concerne = p.getMembre().getUser().getId().equals(caller.getId());
    return gestionnaire || concerne ? prioriteMapper.toResponse(p) : prioriteMapper.toVueMembre(p);
  }

  private static NotificationLiens liens(PrioriteJackpot p) {
    return new NotificationLiens(null, null, p.getMembre().getId(), p.getId());
  }

  private static String nomComplet(TontineMembre m) {
    return m.getUser().getFirstName() + " " + m.getUser().getLastName();
  }

  private static String texte(String valeur) {
    return valeur != null && !valeur.isBlank() ? valeur.trim() : null;
  }
}
