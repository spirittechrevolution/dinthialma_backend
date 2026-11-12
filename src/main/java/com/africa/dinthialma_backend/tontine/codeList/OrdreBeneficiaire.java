package com.africa.dinthialma_backend.tontine.codeList;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Règle de désignation des gagnants d'une tontine ROTATIVE (code list {@code ORDRE_BENEFICIAIRE},
 * stocké en texte dans {@code tontines.ordre_beneficiaire}).
 */
public enum OrdreBeneficiaire {
  /** Selon {@code ordreJackpot}, automatiquement à la clôture du tour. */
  ROTATION,
  /** Tirage au sort parmi les éligibles, automatiquement à la clôture du tour. */
  ALEATOIRE,
  /** Désignés par le gestionnaire après la clôture (liste vide = tirage). */
  MANUEL,
  /** À chaque tour, après la clôture, le gestionnaire choisit : tirage au sort ou choix manuel. */
  HYBRIDE;

  /** Valeurs acceptées, pour les messages d'erreur : « ROTATION, ALEATOIRE, MANUEL, HYBRIDE ». */
  public static final String VALEURS =
      Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));

  /** Code reconnu (casse exacte, comme en base). */
  public static boolean estValide(String code) {
    return code != null && Arrays.stream(values()).anyMatch(o -> o.name().equals(code));
  }

  /**
   * Gagnants désignés par le gestionnaire après la clôture (MANUEL, HYBRIDE) : aucune désignation
   * automatique, endpoint PATCH /beneficiaire autorisé.
   */
  public static boolean designationApresCloture(String code) {
    return MANUEL.name().equals(code) || HYBRIDE.name().equals(code);
  }
}
