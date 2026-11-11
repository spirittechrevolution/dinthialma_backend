package com.africa.dinthialma_backend.common.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Génération CSV pour les exports (B24) : séparateur « ; » et BOM UTF-8 pour une ouverture directe
 * dans Excel en français, échappement RFC 4180 et neutralisation de l'injection de formules.
 */
public final class CsvWriter {

  /** Nombre maximal de lignes par export. */
  public static final int MAX_LIGNES = 10_000;

  private static final char SEP = ';';
  private static final String BOM = "﻿";

  private final StringBuilder sb = new StringBuilder(BOM);

  public CsvWriter ligne(Object... cellules) {
    for (int i = 0; i < cellules.length; i++) {
      if (i > 0) sb.append(SEP);
      sb.append(cellule(cellules[i]));
    }
    sb.append("\r\n");
    return this;
  }

  public CsvWriter ligne(List<?> cellules) {
    return ligne(cellules.toArray());
  }

  public byte[] toBytes() {
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static String cellule(Object valeur) {
    if (valeur == null) return "";
    String s = valeur.toString();
    // Injection de formules (=, +, -, @) : préfixe apostrophe, sauf pour les nombres
    if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0 && !estNombre(s)) {
      s = "'" + s;
    }
    if (s.indexOf(SEP) >= 0
        || s.indexOf('"') >= 0
        || s.indexOf('\n') >= 0
        || s.indexOf('\r') >= 0) {
      s = "\"" + s.replace("\"", "\"\"") + "\"";
    }
    return s;
  }

  private static boolean estNombre(String s) {
    try {
      Double.parseDouble(s);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
