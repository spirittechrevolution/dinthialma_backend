package com.africa.dinthialma_backend.tontine.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Moment où la remise du jackpot d'un tour devient possible (B32, tontines rotatives). */
@Schema(
    description =
        "FIN_DU_TOUR = à l'échéance de la dernière cotisation du tour, même si des membres n'ont"
            + " pas payé ; TOUS_PAYES = dès que tous les membres actifs ont une cotisation validée"
            + " sur toutes les échéances du tour (même avant la dernière échéance)")
public enum DeclenchementJackpot {
  FIN_DU_TOUR,
  TOUS_PAYES
}
