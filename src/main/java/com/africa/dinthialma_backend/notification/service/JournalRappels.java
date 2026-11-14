package com.africa.dinthialma_backend.notification.service;

import com.africa.dinthialma_backend.notification.codeList.CanalRappel;
import com.africa.dinthialma_backend.notification.codeList.NatureRappel;
import com.africa.dinthialma_backend.notification.entity.RappelEnvoye;
import com.africa.dinthialma_backend.notification.repository.RappelEnvoyeRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Journal anti-doublon des rappels d'échéance (B26) : réserve un envoi AVANT de le faire. L'index
 * unique {@code uq_rappel_echeance_user_nature_canal} tranche entre plusieurs instances : seule la
 * première réservation réussit.
 */
@Component
@RequiredArgsConstructor
public class JournalRappels {

  private final RappelEnvoyeRepository repository;

  /**
   * Réserve l'envoi d'un rappel, dans sa propre transaction (un doublon n'annule rien d'autre).
   *
   * @return true si l'envoi est à faire, false s'il a déjà été fait (ou réservé ailleurs)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean reserver(UUID echeanceId, UUID userId, NatureRappel nature, CanalRappel canal) {
    try {
      repository.saveAndFlush(
          RappelEnvoye.builder()
              .echeanceId(echeanceId)
              .userId(userId)
              .nature(nature)
              .canal(canal)
              .build());
      return true;
    } catch (DataIntegrityViolationException dejaEnvoye) {
      return false;
    }
  }
}
