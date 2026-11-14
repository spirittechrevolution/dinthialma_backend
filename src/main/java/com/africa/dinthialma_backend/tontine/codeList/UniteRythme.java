package com.africa.dinthialma_backend.tontine.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Unité du rythme de cotisation (B26) : « tous les N jours / semaines / mois ». */
@Schema(description = "Unité du rythme : JOUR (1 à 90), SEMAINE (1 à 26), MOIS (1 à 12)")
public enum UniteRythme {
  JOUR(90),
  SEMAINE(26),
  MOIS(12);

  private final int intervalleMaximum;

  UniteRythme(int intervalleMaximum) {
    this.intervalleMaximum = intervalleMaximum;
  }

  /** Borne haute de l'intervalle pour cette unité (la borne basse est 1). */
  public int getIntervalleMaximum() {
    return intervalleMaximum;
  }
}
