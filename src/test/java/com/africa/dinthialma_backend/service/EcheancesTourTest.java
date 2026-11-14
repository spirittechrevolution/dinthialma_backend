package com.africa.dinthialma_backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.africa.dinthialma_backend.support.Fixtures;
import com.africa.dinthialma_backend.tontine.calendrier.EcheancesTour;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.EcheanceCycle;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Règles de paiement par échéance (B32, lot 2). */
@DisplayName("EcheancesTour")
class EcheancesTourTest {

  private static final LocalDate LUNDI = LocalDate.of(2026, 11, 16);
  private CycleTontine cycle;

  private EcheanceCycle ajouter(int numero, LocalDate debut, LocalDate fin) {
    EcheanceCycle e =
        EcheanceCycle.builder()
            .cycle(cycle)
            .numero(numero)
            .dateDebut(debut)
            .echeanceAt(fin.atTime(20, 0))
            .build();
    cycle.getEcheances().add(e);
    return e;
  }

  @BeforeEach
  void setUp() {
    cycle = Fixtures.cycle(Fixtures.tontine(Fixtures.user("G")), 1, CycleStatut.EN_COURS);
  }

  @Test
  @DisplayName("actives : sans les supprimées, dans l'ordre ; tour sans échéance -> vide")
  void actives() {
    assertThat(EcheancesTour.actives(null)).isEmpty();
    EcheanceCycle deux = ajouter(2, LUNDI.plusDays(7), LUNDI.plusDays(13));
    EcheanceCycle un = ajouter(1, LUNDI, LUNDI.plusDays(6));
    ajouter(3, LUNDI.plusDays(14), LUNDI.plusDays(20)).setDeletedAt(LocalDateTime.now());
    assertThat(EcheancesTour.actives(cycle)).containsExactly(un, deux);
    assertThat(EcheancesTour.courante(cycle, LUNDI.minusDays(3).atStartOfDay())).contains(un);
  }

  @Test
  @DisplayName("1re échéance ouverte dès le tour ; suivantes à leur 1er jour ; courante = dernière")
  void ouverturesEtCourante() {
    EcheanceCycle un = ajouter(1, LUNDI, LUNDI.plusDays(6));
    EcheanceCycle deux = ajouter(2, LUNDI.plusDays(7), LUNDI.plusDays(13));
    EcheanceCycle trois = ajouter(3, LUNDI.plusDays(14), LUNDI.plusDays(20));

    // avant le début du tour : seule la 1re est ouverte (paiement d'avance, comme avant B32)
    LocalDateTime avant = LUNDI.minusDays(2).atTime(9, 0);
    assertThat(EcheancesTour.ouverte(un, avant)).isTrue();
    assertThat(EcheancesTour.ouverte(deux, avant)).isFalse();

    // 2e semaine : 1 échue, 2 en cours, 3 à venir
    LocalDateTime semaine2 = LUNDI.plusDays(9).atTime(9, 0);
    assertThat(EcheancesTour.ouvertes(cycle, semaine2)).containsExactly(un, deux);
    assertThat(EcheancesTour.courante(cycle, semaine2)).contains(deux);
    assertThat(EcheancesTour.echue(un, semaine2)).isTrue();
    assertThat(EcheancesTour.echue(deux, semaine2)).isFalse();

    // tour dépassé sans clôture : la dernière échue reste la courante
    assertThat(EcheancesTour.courante(cycle, LUNDI.plusDays(30).atStartOfDay())).contains(trois);
  }

  @Test
  @DisplayName("tour sans échéance : aucune courante")
  void sansEcheance() {
    assertThat(EcheancesTour.courante(cycle, LocalDateTime.now())).isEmpty();
  }
}
