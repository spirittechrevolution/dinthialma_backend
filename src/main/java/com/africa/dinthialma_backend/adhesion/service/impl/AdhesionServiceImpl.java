package com.africa.dinthialma_backend.adhesion.service.impl;

import com.africa.dinthialma_backend.adhesion.codeList.StatutDemande;
import com.africa.dinthialma_backend.adhesion.dto.DemandeAdhesionResponse;
import com.africa.dinthialma_backend.adhesion.dto.DemandeRequests;
import com.africa.dinthialma_backend.adhesion.dto.InvitationApercuResponse;
import com.africa.dinthialma_backend.adhesion.dto.InvitationResponse;
import com.africa.dinthialma_backend.adhesion.entity.DemandeAdhesion;
import com.africa.dinthialma_backend.adhesion.mapper.DemandeAdhesionMapper;
import com.africa.dinthialma_backend.adhesion.repository.DemandeAdhesionRepository;
import com.africa.dinthialma_backend.adhesion.service.interfaces.AdhesionService;
import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.repository.UserRoleAssignmentRepository;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.ConflictException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.exception.ForbiddenException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.dto.AddMembreRequest;
import com.africa.dinthialma_backend.member.dto.MembreResponse;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.member.service.interfaces.MembreService;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.dto.NotificationLiens;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import com.africa.dinthialma_backend.tontine.calendrier.Rythme;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdhesionServiceImpl implements AdhesionService {

  /** Alphabet sans caractères ambigus (ni 0/O, ni 1/I/L). */
  private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

  private static final SecureRandom RANDOM = new SecureRandom();

  private final TontineRepository tontineRepository;
  private final DemandeAdhesionMapper demandeAdhesionMapper;
  private final TontineMembreRepository membreRepository;
  private final DemandeAdhesionRepository demandeRepository;
  private final UserRepository userRepository;
  private final UserRoleAssignmentRepository roleRepository;
  private final MembreService membreService;
  private final NotificationService notificationService;

  @Value("${dinthialma.app.url:https://www.dinthialma.com}")
  private String appUrl;

  // ─── B16 : invitation ────────────────────────────────────────────────────

  @Override
  public InvitationResponse obtenirInvitation(String keycloakId, UUID tontineId, boolean regenerer)
      throws CustomException {
    User caller = findUser(keycloakId);
    Tontine tontine = findTontine(tontineId);
    assertCreateurOuSuperAdmin(caller, tontine);
    assertOuverte(tontine);

    if (regenerer || tontine.getCodeInvitation() == null) {
      tontine.setCodeInvitation(nouveauCode());
      tontineRepository.save(tontine);
      log.info("Code d'invitation généré – tontineId={}", tontineId);
    }
    return toInvitation(tontine.getCodeInvitation());
  }

  @Override
  public void revoquerInvitation(String keycloakId, UUID tontineId) throws CustomException {
    User caller = findUser(keycloakId);
    Tontine tontine = findTontine(tontineId);
    assertCreateurOuSuperAdmin(caller, tontine);
    tontine.setCodeInvitation(null);
    tontineRepository.save(tontine);
    log.info("Invitation révoquée – tontineId={}", tontineId);
  }

  @Override
  @Transactional(readOnly = true)
  public InvitationApercuResponse apercu(String code, String keycloakId) throws CustomException {
    Tontine t = findByCode(code);
    Integer places = placesRestantes(t);
    InvitationApercuResponse.InvitationApercuResponseBuilder apercu =
        InvitationApercuResponse.builder()
            .code(t.getCodeInvitation())
            .nom(t.getNom())
            .tontineType(t.getTontineType())
            .montant(t.isMontantLibre() ? null : t.getMontant())
            .frequence(t.getFrequence())
            .rythmeIntervalle(t.getRythme().intervalle())
            .rythmeUnite(t.getRythme().unite())
            .jourEcheance(t.getRythme().jourEcheance())
            .heureEcheance(Rythme.formaterHeure(t.getRythme().heureEcheance()))
            .montantLibre(t.isMontantLibre())
            .montantMinimum(t.getMontantMinimum())
            .nomEvenement(t.getNomEvenement())
            .dateEcheance(t.getDateEcheance())
            .dateDebut(t.getDateDebut())
            .placesRestantes(places)
            .gestionnairePrenom(t.getCreePar() != null ? t.getCreePar().getFirstName() : null)
            .ouverte(estOuverte(t) && (places == null || places > 0));
    if (keycloakId != null) {
      userRepository
          .findByKeycloakId(keycloakId)
          .ifPresent(caller -> relationAppelant(apercu, t, caller));
    }
    return apercu.build();
  }

  /** Relation de l'appelant identifié : membre, gestionnaire, demande en attente. */
  private void relationAppelant(
      InvitationApercuResponse.InvitationApercuResponseBuilder apercu, Tontine t, User caller) {
    boolean membre =
        membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(t.getId(), caller.getId());
    boolean gestionnaire = t.getCreePar().getId().equals(caller.getId());
    apercu
        .dejaMembre(membre)
        .estGestionnaire(gestionnaire)
        .demandeEnAttente(
            demandeRepository.existsByTontine_IdAndUser_IdAndStatutAndDeletedAtIsNull(
                t.getId(), caller.getId(), StatutDemande.EN_ATTENTE))
        .tontineId(membre || gestionnaire ? t.getId() : null);
  }

  // ─── B17 : demandes ──────────────────────────────────────────────────────

  @Override
  public DemandeAdhesionResponse creerDemande(
      String keycloakId, String code, DemandeRequests.Creer request) throws CustomException {
    User caller = findUser(keycloakId);
    Tontine tontine = findByCode(code);
    assertOuverte(tontine);

    if (tontine.getCreePar().getId().equals(caller.getId())) {
      throw new BadRequestException(
          "Vous gérez cette tontine : ajoutez-vous directement comme membre");
    }
    if (membreRepository.existsByTontine_IdAndUser_IdAndDeletedAtIsNull(
        tontine.getId(), caller.getId())) {
      throw new ConflictException(
          ResponseMessageConstants.MEMBER_ALREADY_EXISTS, ErrorCode.MEMBER_ALREADY_IN_TONTINE);
    }
    if (demandeRepository.existsByTontine_IdAndUser_IdAndStatutAndDeletedAtIsNull(
        tontine.getId(), caller.getId(), StatutDemande.EN_ATTENTE)) {
      throw new ConflictException(
          "Vous avez déjà une demande en attente pour cette tontine",
          ErrorCode.DEMANDE_DEJA_EN_ATTENTE);
    }
    assertNonComplete(tontine);

    DemandeAdhesion demande =
        demandeRepository.save(
            DemandeAdhesion.builder()
                .tontine(tontine)
                .user(caller)
                .statut(StatutDemande.EN_ATTENTE)
                .message(
                    request != null && request.getMessage() != null
                        ? request.getMessage().trim()
                        : null)
                .build());

    notificationService.notify(
        tontine.getCreePar().getId(),
        NotificationType.DEMANDE_ADHESION,
        "Nouvelle demande d'adhésion",
        caller.getFirstName()
            + " "
            + caller.getLastName()
            + " demande à rejoindre "
            + tontine.getNom(),
        tontine.getId(),
        NotificationLiens.demande(demande.getId()));
    log.info("Demande d'adhésion – tontineId={} userId={}", tontine.getId(), caller.getId());
    return demandeAdhesionMapper.toVueDemandeur(demande);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<DemandeAdhesionResponse> demandesRecues(
      String keycloakId, StatutDemande statut, UUID tontineId, Pageable pageable)
      throws CustomException {
    User caller = findUser(keycloakId);
    return demandeRepository
        .findRecues(caller.getId(), statut, tontineId, pageable)
        .map(d -> demandeAdhesionMapper.toVueGestionnaire(d));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<DemandeAdhesionResponse> mesDemandes(String keycloakId, Pageable pageable)
      throws CustomException {
    User caller = findUser(keycloakId);
    return demandeRepository
        .findByUser_IdAndDeletedAtIsNull(caller.getId(), pageable)
        .map(d -> demandeAdhesionMapper.toVueDemandeur(d));
  }

  @Override
  public DemandeAdhesionResponse accepter(
      String keycloakId, UUID tontineId, UUID demandeId, DemandeRequests.Accepter request)
      throws CustomException {
    User caller = findUser(keycloakId);
    DemandeAdhesion demande = findDemandeEnAttente(caller, tontineId, demandeId);
    Tontine tontine = demande.getTontine();
    assertOuverte(tontine);
    assertNonComplete(tontine);

    // Mêmes règles et notifications que l'ajout manuel (unicité, position, rôle MEMBER, notif)
    AddMembreRequest ajout = new AddMembreRequest();
    ajout.setPhone(demande.getUser().getPhone());
    ajout.setOrdreJackpot(request != null ? request.getOrdreJackpot() : null);
    MembreResponse membre = membreService.addMembre(keycloakId, tontineId, ajout);

    demande.setStatut(StatutDemande.ACCEPTEE);
    demande.setTraiteePar(caller);
    demande.setDateTraitement(LocalDateTime.now());
    demande.setMembre(membreRepository.getReferenceById(membre.getId()));
    log.info("Demande acceptée – demandeId={} membreId={}", demandeId, membre.getId());
    return demandeAdhesionMapper.toVueGestionnaire(demandeRepository.save(demande));
  }

  @Override
  public DemandeAdhesionResponse refuser(
      String keycloakId, UUID tontineId, UUID demandeId, DemandeRequests.Refuser request)
      throws CustomException {
    User caller = findUser(keycloakId);
    DemandeAdhesion demande = findDemandeEnAttente(caller, tontineId, demandeId);
    String motif =
        request != null && request.getMotif() != null && !request.getMotif().isBlank()
            ? request.getMotif().trim()
            : null;

    demande.setStatut(StatutDemande.REFUSEE);
    demande.setMotifRefus(motif);
    demande.setTraiteePar(caller);
    demande.setDateTraitement(LocalDateTime.now());
    demandeRepository.save(demande);

    notificationService.notify(
        demande.getUser().getId(),
        NotificationType.ADHESION_REFUSEE,
        "Demande d'adhésion refusée",
        "Votre demande pour rejoindre "
            + demande.getTontine().getNom()
            + " a été refusée"
            + (motif != null ? " · Motif : " + motif : "")
            + ".",
        tontineId,
        NotificationLiens.demande(demande.getId()));
    log.info("Demande refusée – demandeId={}", demandeId);
    return demandeAdhesionMapper.toVueGestionnaire(demande);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  private DemandeAdhesion findDemandeEnAttente(User caller, UUID tontineId, UUID demandeId)
      throws CustomException {
    DemandeAdhesion demande =
        demandeRepository
            .findByIdAndTontine_IdAndDeletedAtIsNull(demandeId, tontineId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.DEMANDE_NOT_FOUND));
    assertCreateurOuSuperAdmin(caller, demande.getTontine());
    if (demande.getStatut() != StatutDemande.EN_ATTENTE) {
      throw new BadRequestException(
          "Cette demande a déjà été traitée (statut : " + demande.getStatut() + ")",
          ErrorCode.DEMANDE_NOT_PENDING);
    }
    return demande;
  }

  private static boolean estOuverte(Tontine t) {
    return t.getStatut() == TontineStatut.BROUILLON || t.getStatut() == TontineStatut.ACTIVE;
  }

  private static void assertOuverte(Tontine t) throws CustomException {
    if (!estOuverte(t)) {
      throw new BadRequestException(
          "Cette tontine n'accepte plus de nouveaux membres (statut : " + t.getStatut() + ")",
          ErrorCode.TONTINE_NOT_OPEN);
    }
  }

  /** Places restantes : ROTATIVE à effectif fixé uniquement (null = sans limite). */
  private Integer placesRestantes(Tontine t) {
    if (t.getTontineType() != TontineType.ROTATIVE || t.getNombreMembres() <= 0) return null;
    long actifs =
        membreRepository.countByTontine_IdAndStatutAndDeletedAtIsNull(
            t.getId(), MembreStatut.ACTIF);
    return (int) Math.max(0, t.getNombreMembres() - actifs);
  }

  private void assertNonComplete(Tontine t) throws CustomException {
    Integer places = placesRestantes(t);
    if (places != null && places <= 0) {
      throw new ConflictException("Cette tontine est complète", ErrorCode.TONTINE_FULL);
    }
  }

  private Tontine findByCode(String code) throws CustomException {
    String normalise = normaliser(code);
    return tontineRepository
        .findByCodeInvitationAndDeletedAtIsNull(normalise)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.INVITATION_NOT_FOUND));
  }

  /** Majuscules, espaces retirés ; « ABCD2345 » accepté pour « ABCD-2345 ». */
  static String normaliser(String code) {
    if (code == null) return "";
    String c = code.replace(" ", "").trim().toUpperCase(Locale.ROOT);
    if (c.length() == 8 && c.indexOf('-') < 0) c = c.substring(0, 4) + "-" + c.substring(4);
    return c;
  }

  private String nouveauCode() {
    String code;
    do {
      StringBuilder sb = new StringBuilder(9);
      for (int i = 0; i < 8; i++) {
        if (i == 4) sb.append('-');
        sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
      }
      code = sb.toString();
    } while (tontineRepository.existsByCodeInvitation(code));
    return code;
  }

  private InvitationResponse toInvitation(String code) {
    String base = appUrl.endsWith("/") ? appUrl.substring(0, appUrl.length() - 1) : appUrl;
    return InvitationResponse.builder()
        .code(code)
        .url(base + "/q/t/" + code)
        .qrToken("t_" + code)
        .build();
  }

  private void assertCreateurOuSuperAdmin(User caller, Tontine tontine) throws CustomException {
    if (tontine.getCreePar().getId().equals(caller.getId())) return;
    if (roleRepository.existsByUserIdAndRole(caller.getId(), UserRole.SUPER_ADMIN)) return;
    throw new ForbiddenException(ResponseMessageConstants.TONTINE_ACCESS_DENIED);
  }

  private Tontine findTontine(UUID id) throws CustomException {
    return tontineRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.TONTINE_NOT_FOUND));
  }

  private User findUser(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }
}
