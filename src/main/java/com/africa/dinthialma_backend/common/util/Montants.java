package com.africa.dinthialma_backend.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Calculs de montants en francs CFA entiers (S02) : arrondi au franc le plus proche et partages
 * dont la somme est <b>exactement</b> le total (aucun franc ni centime perdu).
 */
public final class Montants {

  private static final BigDecimal CENT = BigDecimal.valueOf(100);

  private Montants() {}

  /** Arrondi au franc le plus proche (0,5 vers le haut). */
  public static BigDecimal arrondir(BigDecimal montant) {
    return montant.setScale(0, RoundingMode.HALF_UP);
  }

  /** {@code pourcentage} % de {@code base}, arrondi au franc. */
  public static BigDecimal pourcentage(BigDecimal base, BigDecimal pourcentage) {
    return arrondir(base.multiply(pourcentage).divide(CENT, 10, RoundingMode.HALF_UP));
  }

  /**
   * Partage égal de {@code total} en {@code n} parts en francs entiers ; le reste va, un franc
   * chacun, aux premières parts (ordre de désignation) : 1 000 / 3 → 334, 333, 333. Si le total a
   * des décimales, elles vont à la première part. Somme des parts = total.
   */
  public static List<BigDecimal> repartir(BigDecimal total, int n) {
    if (n <= 0) {
      throw new IllegalArgumentException("Nombre de parts invalide : " + n);
    }
    BigDecimal base = total.divide(BigDecimal.valueOf(n), 0, RoundingMode.FLOOR);
    BigDecimal reste = total.subtract(base.multiply(BigDecimal.valueOf(n)));
    List<BigDecimal> parts = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      parts.add(base);
    }
    return distribuerReste(parts, reste, IntStream.range(0, n).boxed().toList());
  }

  /**
   * Partage de {@code total} au prorata des {@code poids}, en francs entiers, par la méthode des
   * plus forts restes (à égalité : ordre de la liste). Poids tous nuls : partage égal. Somme =
   * total.
   */
  public static List<BigDecimal> repartirAuProrata(BigDecimal total, List<BigDecimal> poids) {
    if (poids.isEmpty()) {
      throw new IllegalArgumentException("Aucun poids pour la répartition");
    }
    BigDecimal somme = poids.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    if (somme.signum() == 0) {
      return repartir(total, poids.size());
    }
    List<BigDecimal> parts = new ArrayList<>(poids.size());
    List<BigDecimal> fractions = new ArrayList<>(poids.size());
    for (BigDecimal p : poids) {
      BigDecimal exacte = total.multiply(p).divide(somme, 10, RoundingMode.HALF_UP);
      BigDecimal entiere = exacte.setScale(0, RoundingMode.FLOOR);
      parts.add(entiere);
      fractions.add(exacte.subtract(entiere));
    }
    BigDecimal reste = total.subtract(parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    List<Integer> ordre =
        IntStream.range(0, poids.size())
            .boxed()
            .sorted(Comparator.comparing((Integer i) -> fractions.get(i)).reversed())
            .toList();
    return distribuerReste(parts, reste, ordre);
  }

  /** Un franc par part dans l'ordre donné, puis l'éventuel reliquat décimal à la première. */
  private static List<BigDecimal> distribuerReste(
      List<BigDecimal> parts, BigDecimal reste, List<Integer> ordre) {
    int francs = reste.setScale(0, RoundingMode.FLOOR).intValueExact();
    for (int k = 0; k < francs; k++) {
      int i = ordre.get(k % ordre.size());
      parts.set(i, parts.get(i).add(BigDecimal.ONE));
    }
    BigDecimal reliquat = reste.subtract(BigDecimal.valueOf(francs));
    if (reliquat.signum() != 0) {
      int i = ordre.get(0);
      parts.set(i, parts.get(i).add(reliquat));
    }
    return parts;
  }
}
