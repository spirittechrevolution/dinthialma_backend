package com.africa.dinthialma_backend.adhesion.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/** Statut d'une demande d'adhésion. */
@Schema(
    description =
        "EN_ATTENTE = à traiter par le gestionnaire ; ACCEPTEE = le demandeur est devenu membre ;"
            + " REFUSEE = refusée (motif éventuel)")
public enum StatutDemande {
  EN_ATTENTE,
  ACCEPTEE,
  REFUSEE
}
