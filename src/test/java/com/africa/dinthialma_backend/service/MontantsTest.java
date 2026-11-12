package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.africa.dinthialma_backend.common.util.Montants;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Montants (francs entiers, partages exacts)")
class MontantsTest {

  private static BigDecimal f(String v) {
    return new BigDecimal(v);
  }

  private static List<BigDecimal> francs(String... valeurs) {
    return Arrays.stream(valeurs).map(BigDecimal::new).toList();
  }

  private static void sommeExacte(List<BigDecimal> parts, String total) {
    assertThat(parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(total);
  }

  @Test
  @DisplayName("arrondi au franc le plus proche, 0,5 vers le haut ; pourcentage arrondi")
  void arrondis() {
    assertThat(Montants.arrondir(f("1500.49"))).isEqualByComparingTo("1500");
    assertThat(Montants.arrondir(f("1500.50"))).isEqualByComparingTo("1501");
    assertThat(Montants.pourcentage(f("3333"), f("10"))).isEqualByComparingTo("333");
    assertThat(Montants.pourcentage(f("3335"), f("10"))).isEqualByComparingTo("334");
    assertThat(Montants.pourcentage(f("2000"), f("2.5"))).isEqualByComparingTo("50");
  }

  @Test
  @DisplayName("partage égal : 1 000 / 3 -> 334, 333, 333 (somme exacte)")
  void repartir() {
    List<BigDecimal> parts = Montants.repartir(f("1000"), 3);
    assertThat(parts)
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactlyElementsOf(francs("334", "333", "333"));
    sommeExacte(parts, "1000");

    assertThat(Montants.repartir(f("3002"), 3))
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactlyElementsOf(francs("1001", "1001", "1000"));
    assertThat(Montants.repartir(f("900"), 3))
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactlyElementsOf(francs("300", "300", "300"));
  }

  @Test
  @DisplayName("total décimal : le reliquat va à la première part ; total négatif exact")
  void casParticuliers() {
    List<BigDecimal> parts = Montants.repartir(f("1000.50"), 3);
    assertThat(parts)
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactlyElementsOf(francs("334.50", "333", "333"));
    sommeExacte(parts, "1000.50");
    sommeExacte(Montants.repartir(f("-100"), 3), "-100");
    assertThat(Montants.repartir(f("7"), 1)).containsExactly(f("7"));
    assertThatThrownBy(() -> Montants.repartir(f("10"), 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("prorata : plus forts restes, somme exacte ; poids nuls -> partage égal")
  void prorata() {
    // 100 F de commission sur des mises 1 000 / 1 000 / 1 000 -> 34, 33, 33
    List<BigDecimal> egal = Montants.repartirAuProrata(f("100"), francs("1000", "1000", "1000"));
    assertThat(egal)
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactlyElementsOf(francs("34", "33", "33"));

    // 100 F sur 1 000 / 2 000 / 3 000 -> exacts 16,67 / 33,33 / 50 -> 17, 33, 50
    List<BigDecimal> parts = Montants.repartirAuProrata(f("100"), francs("1000", "2000", "3000"));
    assertThat(parts)
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactlyElementsOf(francs("17", "33", "50"));
    sommeExacte(parts, "100");

    sommeExacte(Montants.repartirAuProrata(f("0"), francs("500", "700")), "0");
    assertThat(Montants.repartirAuProrata(f("10"), francs("0", "0")))
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactlyElementsOf(francs("5", "5"));
    assertThatThrownBy(() -> Montants.repartirAuProrata(f("10"), List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
