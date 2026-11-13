package com.africa.dinthialma_backend.tontine.service.interfaces;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.tontine.dto.SimulationCalendrierRequest;
import com.africa.dinthialma_backend.tontine.dto.SimulationCalendrierResponse;

/** Calendrier des tours (B26). */
public interface CalendrierService {

  /**
   * Simule le calendrier d'une tontine avant sa création, avec les mêmes règles que la génération
   * des tours à l'activation. Aucun effet en base.
   *
   * @throws CustomException rythme invalide ({@code RYTHME_INVALIDE}, {@code
   *     JOUR_ECHEANCE_INVALIDE}, {@code HEURE_ECHEANCE_INVALIDE}), nombre de membres ou date de
   *     l'événement manquants
   */
  SimulationCalendrierResponse simuler(SimulationCalendrierRequest request) throws CustomException;
}
