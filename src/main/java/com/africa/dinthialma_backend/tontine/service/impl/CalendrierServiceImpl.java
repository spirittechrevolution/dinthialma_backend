package com.africa.dinthialma_backend.tontine.service.impl;

import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.util.Fuseau;
import com.africa.dinthialma_backend.tontine.calendrier.CalendrierTours;
import com.africa.dinthialma_backend.tontine.calendrier.CalendrierTours.PeriodeTour;
import com.africa.dinthialma_backend.tontine.calendrier.Rythme;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.dto.SimulationCalendrierRequest;
import com.africa.dinthialma_backend.tontine.dto.SimulationCalendrierResponse;
import com.africa.dinthialma_backend.tontine.service.interfaces.CalendrierService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CalendrierServiceImpl implements CalendrierService {

  /** Nombre maximal d'échéances renvoyées par la simulation. */
  static final int ECHEANCES_LISTEES_MAXIMUM = 400;

  @Override
  public SimulationCalendrierResponse simuler(SimulationCalendrierRequest request)
      throws CustomException {
    Rythme rythme =
        Rythme.resoudre(
            request.getRythmeIntervalle(),
            request.getRythmeUnite(),
            request.getJourEcheance(),
            request.getHeureEcheance(),
            request.getFrequence());
    List<PeriodeTour> periodes =
        request.getTontineType() == TontineType.EVENEMENTIELLE
            ? periodesEvenementielle(rythme, request)
            : periodesRotative(rythme, request);

    return SimulationCalendrierResponse.builder()
        .nombreTours(periodes.size())
        .premiereEcheance(Fuseau.versOffset(periodes.get(0).echeanceAt()))
        .derniereEcheance(Fuseau.versOffset(periodes.get(periodes.size() - 1).echeanceAt()))
        .rythme(rythme.libelle())
        .echeances(
            periodes.stream()
                .limit(ECHEANCES_LISTEES_MAXIMUM)
                .map(p -> Fuseau.versOffset(p.echeanceAt()))
                .toList())
        .build();
  }

  private static List<PeriodeTour> periodesRotative(
      Rythme rythme, SimulationCalendrierRequest request) throws CustomException {
    if (request.getNombreMembres() == null) {
      throw new BadRequestException("nombreMembres est obligatoire pour une tontine rotative");
    }
    int gagnants = request.getNombreGagnants() != null ? request.getNombreGagnants() : 1;
    int tours = (int) Math.ceil((double) request.getNombreMembres() / gagnants);
    return CalendrierTours.generer(rythme, request.getDateDebut(), tours, null);
  }

  private static List<PeriodeTour> periodesEvenementielle(
      Rythme rythme, SimulationCalendrierRequest request) throws CustomException {
    if (request.getDateEcheance() == null
        || !request.getDateEcheance().isAfter(request.getDateDebut())) {
      throw new BadRequestException(
          "dateEcheance est obligatoire et doit être postérieure à dateDebut");
    }
    return CalendrierTours.generer(
        rythme, request.getDateDebut(), Integer.MAX_VALUE, request.getDateEcheance());
  }
}
