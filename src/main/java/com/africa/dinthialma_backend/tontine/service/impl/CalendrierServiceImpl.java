package com.africa.dinthialma_backend.tontine.service.impl;

import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.util.Fuseau;
import com.africa.dinthialma_backend.tontine.calendrier.CalendrierTours;
import com.africa.dinthialma_backend.tontine.calendrier.CalendrierTours.PeriodeTour;
import com.africa.dinthialma_backend.tontine.calendrier.Rythme;
import com.africa.dinthialma_backend.tontine.calendrier.RythmeJackpot;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.dto.SimulationCalendrierRequest;
import com.africa.dinthialma_backend.tontine.dto.SimulationCalendrierResponse;
import com.africa.dinthialma_backend.tontine.dto.SimulationCalendrierResponse.TourSimule;
import com.africa.dinthialma_backend.tontine.service.interfaces.CalendrierService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CalendrierServiceImpl implements CalendrierService {

  /** Nombre maximal d'échéances (et de tours) renvoyés par la simulation. */
  static final int LISTE_MAXIMUM = 400;

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
    TontineType type =
        request.getTontineType() != null ? request.getTontineType() : TontineType.ROTATIVE;
    int membres = request.getNombreMembres() != null ? request.getNombreMembres() : 0;
    int gagnants = request.getNombreGagnants() != null ? request.getNombreGagnants() : 1;
    int parTour =
        RythmeJackpot.cotisationsParTour(
            type,
            request.getCotisationsParTour(),
            request.getJackpotVise(),
            request.getMontant(),
            membres,
            gagnants);

    List<List<PeriodeTour>> tours =
        type == TontineType.EVENEMENTIELLE
            ? CalendrierTours.grouper(periodesEvenementielle(rythme, request), 1)
            : CalendrierTours.grouper(periodesRotative(rythme, request, parTour), parTour);
    List<PeriodeTour> echeances = tours.stream().flatMap(List::stream).toList();

    return SimulationCalendrierResponse.builder()
        .nombreTours(tours.size())
        .cotisationsParTour(parTour)
        .jackpotParTour(
            type == TontineType.ROTATIVE && request.getMontant() != null && membres > 0
                ? RythmeJackpot.jackpotParTour(request.getMontant(), membres, gagnants, parTour)
                : null)
        .premiereEcheance(Fuseau.versOffset(echeances.get(0).echeanceAt()))
        .derniereEcheance(Fuseau.versOffset(echeances.get(echeances.size() - 1).echeanceAt()))
        .rythme(rythme.libelle())
        .echeances(
            echeances.stream()
                .limit(LISTE_MAXIMUM)
                .map(p -> Fuseau.versOffset(p.echeanceAt()))
                .toList())
        .tours(toursSimules(tours))
        .build();
  }

  private static List<TourSimule> toursSimules(List<List<PeriodeTour>> tours) {
    List<TourSimule> resultat = new ArrayList<>();
    for (int i = 0; i < Math.min(tours.size(), LISTE_MAXIMUM); i++) {
      List<PeriodeTour> tour = tours.get(i);
      resultat.add(
          TourSimule.builder()
              .numero(i + 1)
              .dateDebut(tour.get(0).debut())
              .dateFin(tour.get(tour.size() - 1).fin())
              .echeances(tour.stream().map(p -> Fuseau.versOffset(p.echeanceAt())).toList())
              .build());
    }
    return resultat;
  }

  private static List<PeriodeTour> periodesRotative(
      Rythme rythme, SimulationCalendrierRequest request, int parTour) throws CustomException {
    if (request.getNombreMembres() == null) {
      throw new BadRequestException("nombreMembres est obligatoire pour une tontine rotative");
    }
    int gagnants = request.getNombreGagnants() != null ? request.getNombreGagnants() : 1;
    int tours = (int) Math.ceil((double) request.getNombreMembres() / gagnants);
    return CalendrierTours.generer(rythme, request.getDateDebut(), tours * parTour, null);
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
