package com.africa.dinthialma_backend.common.util;

/**
 * Utilitaires de normalisation des numéros de téléphone.
 *
 * <p>Convention Dinthialma : les numéros sont stockés <strong>sans le préfixe {@code +}</strong>
 * (ex. {@code 221783703310} et non {@code +221783703310}). Cette normalisation est appliquée
 * systématiquement à l'entrée du système (inscription, bootstrap, login).
 */
public final class PhoneUtils {

  private PhoneUtils() {}

  /**
   * Normalise un numéro de téléphone en supprimant le {@code +} initial s'il est présent.
   *
   * <pre>
   * normalize("+221783703310") → "221783703310"
   * normalize("221783703310")  → "221783703310"
   * normalize("  +221 783 703 310  ") → "221 783 703 310"  (trim + strip +)
   * </pre>
   *
   * @param phone numéro brut (peut être {@code null})
   * @return numéro normalisé, ou {@code null} si l'entrée est {@code null}
   */
  public static String normalize(String phone) {
    if (phone == null) return null;
    String trimmed = phone.trim();
    return trimmed.startsWith("+") ? trimmed.substring(1) : trimmed;
  }
}
