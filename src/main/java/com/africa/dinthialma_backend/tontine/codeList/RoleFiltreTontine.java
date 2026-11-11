package com.africa.dinthialma_backend.tontine.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Filtre de la liste des tontines selon la relation de l'appelant. */
@Schema(
    description =
        "Filtre « Mes tontines »."
            + " GERE = tontines dont l'appelant est le créateur ;"
            + " COTISE = tontines où l'appelant est inscrit comme cotisant.")
public enum RoleFiltreTontine {
  GERE,
  COTISE
}
