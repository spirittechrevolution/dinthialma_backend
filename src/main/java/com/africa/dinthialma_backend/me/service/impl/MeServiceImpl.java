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
import com.africa.dinthialma_backend.me.service.interfaces.MeService;
import java.util.List;
import java.util.UUID;
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
