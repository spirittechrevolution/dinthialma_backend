package com.africa.dinthialma_backend.jackpot.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.jackpot.codeList.StatutPriorite;
import com.africa.dinthialma_backend.jackpot.dto.PrioriteJackpotResponse;
import com.africa.dinthialma_backend.jackpot.dto.PrioriteRequests;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Priorités de jackpot (tontines ROTATIVE) : un membre dans le besoin reçoit le prochain jackpot
 * avant la règle convenue, puis la règle reprend pour les autres.
 */
public interface PrioriteJackpotService {

  /** Le membre connecté demande la priorité ; le gestionnaire est notifié. */
  PrioriteJackpotResponse demander(
      String keycloakId, UUID tontineId, PrioriteRequests.Demander request) throws CustomException;

  /** Le gestionnaire attribue directement la priorité (accord pris avec le groupe). */
  PrioriteJackpotResponse attribuer(
      String keycloakId, UUID tontineId, PrioriteRequests.Attribuer request) throws CustomException;

  /** Priorités de la tontine (membres, gestionnaire, SUPER_ADMIN) ; motif masqué aux autres. */
  Page<PrioriteJackpotResponse> lister(
      String keycloakId, UUID tontineId, StatutPriorite statut, Pageable pageable)
      throws CustomException;

  /** Accepte une demande EN_ATTENTE. */
  PrioriteJackpotResponse accepter(String keycloakId, UUID tontineId, UUID prioriteId)
      throws CustomException;

  /** Refuse une demande EN_ATTENTE (motif optionnel) et notifie le membre. */
  PrioriteJackpotResponse refuser(
      String keycloakId, UUID tontineId, UUID prioriteId, PrioriteRequests.Refuser request)
      throws CustomException;

  /** Annule une priorité active : par le gestionnaire, ou par le membre concerné. */
  PrioriteJackpotResponse annuler(String keycloakId, UUID tontineId, UUID prioriteId)
      throws CustomException;

  /**
   * Membres à servir en premier, dans l'ordre d'acceptation : priorités ACCEPTEE dont le membre est
   * encore ACTIF et n'a pas reçu de jackpot.
   */
  List<TontineMembre> prioritairesEligibles(UUID tontineId);

  /**
   * Priorités actives des gagnants du tour → SERVIE (quelle que soit la façon dont ils ont gagné).
   */
  void marquerServies(CycleTontine cycle, Collection<TontineMembre> gagnants);
}
