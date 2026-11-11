package com.africa.dinthialma_backend.common.util;

import java.util.Locale;

/** Normalisation des termes de recherche libre ({@code ?q=}) utilisés par les Specifications. */
public final class SearchUtils {

  private SearchUtils() {}

  /**
   * Motif {@code LIKE} insensible à la casse : {@code %terme%} en minuscules, « + » initial retiré
   * (les téléphones sont stockés sans). Les jokers SQL saisis par l'utilisateur sont échappés.
   *
   * @return null si le terme est vide (aucun filtre)
   */
  public static String motifLike(String q) {
    if (q == null || q.isBlank()) return null;
    String terme = q.trim();
    if (terme.startsWith("+")) terme = terme.substring(1);
    terme = terme.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return "%" + terme.toLowerCase(Locale.ROOT) + "%";
  }
}
