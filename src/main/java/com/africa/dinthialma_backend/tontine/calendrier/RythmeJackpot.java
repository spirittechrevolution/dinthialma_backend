package com.africa.dinthialma_backend.tontine.calendrier;

import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.africa.dinthialma_backend.common.util.Montants;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rythme du jackpot (B32) : nombre de cotisations par tour, saisi ou déduit d'un jackpot visé
 * (brut, avant commission). Ex. 10 membres × 5 000 F par semaine = 50 000 F par échéance ; 500 000
 * F visés → 10 cotisations par tour.
 */
public final class RythmeJackpot {

  private RythmeJackpot() {}

  /**
   * Cotisations par tour retenues : {@code demande} si fourni, sinon déduit de {@code jackpotVise}
   * (ROTATIVE), sinon 1.
   *
   * @throws CustomException {@code COTISATIONS_PAR_TOUR_INVALIDE} (valeur < 1, ou ≠ 1 hors
   *     ROTATIVE, ou jackpot visé sans montant ni nombre de membres)
   */
  public static int cotisationsParTour(
      TontineType type,
      Integer demande,
      BigDecimal jackpotVise,
      BigDecimal montant,
      int membres,
      int gagnants)
      throws CustomException {
    int n = 1;
    if (demande != null) {
      n = demande;
    } else if (jackpotVise != null && type == TontineType.ROTATIVE) {
      n = deduire(jackpotVise, montant, membres, gagnants);
    }
    if (n < 1) {
      throw invalide("Le nombre de cotisations par tour doit être au moins 1");
    }
    if (type != TontineType.ROTATIVE && n != 1) {
      throw invalide("Plusieurs cotisations par tour : réservé aux tontines rotatives");
    }
    return n;
  }

  /** Nombre de cotisations par tour le plus proche du jackpot visé (au moins 1). */
  public static int deduire(BigDecimal jackpotVise, BigDecimal montant, int membres, int gagnants)
      throws CustomException {
    BigDecimal parEcheance = collecteParEcheance(montant, membres, gagnants);
    if (parEcheance.signum() <= 0) {
      throw invalide(
          "Jackpot visé : le montant de cotisation et le nombre de membres sont nécessaires");
    }
    int n = jackpotVise.divide(parEcheance, 0, RoundingMode.HALF_UP).intValue();
    return Math.max(1, n);
  }

  /**
   * Jackpot brut prévu d'un tour : montant × membres × échéances ÷ gagnants, en francs entiers
   * (part de chaque gagnant).
   */
  public static BigDecimal jackpotParTour(
      BigDecimal montant, int membres, int gagnants, int echeances) {
    return Montants.arrondir(
        collecteParEcheance(montant, membres, gagnants).multiply(BigDecimal.valueOf(echeances)));
  }

  private static BigDecimal collecteParEcheance(BigDecimal montant, int membres, int gagnants) {
    if (montant == null || membres <= 0) {
      return BigDecimal.ZERO;
    }
    return montant
        .multiply(BigDecimal.valueOf(membres))
        .divide(BigDecimal.valueOf(Math.max(1, gagnants)), 2, RoundingMode.HALF_UP);
  }

  private static BadRequestException invalide(String message) {
    return new BadRequestException(message, ErrorCode.COTISATIONS_PAR_TOUR_INVALIDE);
  }
}
