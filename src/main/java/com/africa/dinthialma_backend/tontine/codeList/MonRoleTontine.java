package com.africa.dinthialma_backend.tontine.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Relation de l'utilisateur appelant avec une tontine donnée. */
@Schema(
    description =
        "Rôle de l'appelant dans la tontine."
            + " GESTIONNAIRE = créateur de la tontine ;"
            + " MEMBRE = cotisant de la tontine ;"
            + " GESTIONNAIRE_ET_MEMBRE = créateur inscrit aussi comme cotisant ;"
            + " LECTURE = aucune relation directe (SUPER_ADMIN qui consulte).")
public enum MonRoleTontine {
  GESTIONNAIRE,
  MEMBRE,
  GESTIONNAIRE_ET_MEMBRE,
  LECTURE;

  /** Déduit le rôle à partir des deux relations possibles. */
  public static MonRoleTontine of(boolean createur, boolean membre) {
    if (createur && membre) return GESTIONNAIRE_ET_MEMBRE;
    if (createur) return GESTIONNAIRE;
    if (membre) return MEMBRE;
    return LECTURE;
  }
}
