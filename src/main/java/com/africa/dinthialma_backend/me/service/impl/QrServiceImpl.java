package com.africa.dinthialma_backend.me.service.impl;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.contribution.repository.CotisationRepository;
import com.africa.dinthialma_backend.me.dto.MonQrResponse;
import com.africa.dinthialma_backend.me.dto.QrResolutionResponse;
import com.africa.dinthialma_backend.me.service.interfaces.QrService;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.member.repository.TontineMembreRepository;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import com.africa.dinthialma_backend.tontine.repository.CycleTontineRepository;
import com.africa.dinthialma_backend.tontine.repository.TontineRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QrServiceImpl implements QrService {

  /** Préfixe des QR membres. */
  static final String PREFIXE_MEMBRE = "m_";

  /** Préfixe des QR d'invitation de tontine (B16) : « t_<code> ». */
  static final String PREFIXE_TONTINE = "t_";

  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserRepository userRepository;
  private final TontineMembreRepository membreRepository;
  private final CycleTontineRepository cycleRepository;
  private final CotisationRepository cotisationRepository;
  private final TontineRepository tontineRepository;

  @Value("${dinthialma.app.url:https://www.dinthialma.com}")
  private String appUrl;

  @Override
  public MonQrResponse getMonQr(String keycloakId) throws CustomException {
    User user = findUserByKeycloakId(keycloakId);
    if (user.getQrToken() == null) {
      user.setQrToken(nouveauJeton());
      userRepository.save(user);
    }
    return toResponse(user.getQrToken());
  }

  @Override
  public MonQrResponse regenererMonQr(String keycloakId) throws CustomException {
    User user = findUserByKeycloakId(keycloakId);
    user.setQrToken(nouveauJeton());
    userRepository.save(user);
    log.info("QR membre régénéré – userId={}", user.getId());
    return toResponse(user.getQrToken());
  }

  @Override
  @Transactional(readOnly = true)
  public QrResolutionResponse resoudre(String keycloakId, String token) throws CustomException {
    User caller = findUserByKeycloakId(keycloakId);

    if (token != null && token.startsWith(PREFIXE_TONTINE)) {
      Tontine t =
          tontineRepository
              .findByCodeInvitationAndDeletedAtIsNull(token.substring(PREFIXE_TONTINE.length()))
              .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.QR_NOT_FOUND));
      return QrResolutionResponse.builder()
          .type("TONTINE")
          .invitationCode(t.getCodeInvitation())
          .dansMesTontines(List.of())
          .build();
    }
    if (token == null || !token.startsWith(PREFIXE_MEMBRE)) {
      throw new NotFoundException(ResponseMessageConstants.QR_NOT_FOUND);
    }
    User scanne =
        userRepository
            .findByQrTokenAndDeletedAtIsNull(token)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.QR_NOT_FOUND));

    List<QrResolutionResponse.MembreDansMaTontine> dansMesTontines =
        membreRepository
            .findByUser_IdAndDeletedAtIsNullAndTontine_DeletedAtIsNullOrderByCreatedAtDesc(
                scanne.getId())
            .stream()
            .filter(m -> m.getTontine().getCreePar().getId().equals(caller.getId()))
            .map(this::situation)
            .toList();

    return QrResolutionResponse.builder()
        .type("MEMBRE")
        .user(
            QrResolutionResponse.Personne.builder()
                .id(scanne.getId())
                .firstName(scanne.getFirstName())
                .lastName(scanne.getLastName())
                .accountStatus(scanne.getAccountStatus())
                .build())
        .dansMesTontines(dansMesTontines)
        .build();
  }

  private QrResolutionResponse.MembreDansMaTontine situation(TontineMembre m) {
    Tontine t = m.getTontine();
    CycleTontine enCours =
        cycleRepository
            .findByTontine_IdAndStatutAndDeletedAtIsNull(t.getId(), CycleStatut.EN_COURS)
            .orElse(null);
    return QrResolutionResponse.MembreDansMaTontine.builder()
        .tontineId(t.getId())
        .tontineNom(t.getNom())
        .membreId(m.getId())
        .statutMembre(m.getStatut())
        .cycleEnCoursId(enCours != null ? enCours.getId() : null)
        .aPayeCycleEnCours(
            enCours != null
                ? cotisationRepository.existsActiveByCycleAndMembre(enCours.getId(), m.getId())
                : null)
        .build();
  }

  /** Jeton aléatoire de 128 bits encodé en base64url, garanti unique. */
  private String nouveauJeton() {
    String jeton;
    do {
      byte[] octets = new byte[16];
      RANDOM.nextBytes(octets);
      jeton = PREFIXE_MEMBRE + Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    } while (userRepository.existsByQrToken(jeton));
    return jeton;
  }

  private MonQrResponse toResponse(String jeton) {
    String base = appUrl.endsWith("/") ? appUrl.substring(0, appUrl.length() - 1) : appUrl;
    return MonQrResponse.builder().qrToken(jeton).url(base + "/q/" + jeton).build();
  }

  private User findUserByKeycloakId(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.AUTH_USER_NOT_FOUND));
  }
}
