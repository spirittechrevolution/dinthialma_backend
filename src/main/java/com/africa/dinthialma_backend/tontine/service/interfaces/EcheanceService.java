package com.africa.dinthialma_backend.tontine.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.tontine.dto.ModifierEcheanceRequest;
import com.africa.dinthialma_backend.tontine.dto.TontineResponse;
import java.util.UUID;

/** Échéances d'une tontine modifiées en cours de route (B26). */
public interface EcheanceService {

  /**
   * Modifie le jour et l'heure d'échéance et les rappels d'une tontine non terminée.
   *
   * <p>Le tour en cours et les tours terminés ne bougent pas ; les tours pas encore commencés sont
   * replanifiés à partir de la fin du dernier tour commencé (événementielle : jusqu'à la date de
   * l'événement, quitte à ajouter ou retirer des tours à venir). Un changement d'échéance est
   * audité et notifié aux membres actifs ({@code REGLES_MODIFIEES}). Réservé au créateur et au
   * SUPER_ADMIN.
   *
   * @throws CustomException {@code ECHEANCE_NON_MODIFIABLE} (tontine TERMINEE), {@code
   *     JOUR_ECHEANCE_INVALIDE}, {@code HEURE_ECHEANCE_INVALIDE}, 403, 404
   */
  TontineResponse modifierEcheance(
      String keycloakId, UUID tontineId, ModifierEcheanceRequest request) throws CustomException;
}
