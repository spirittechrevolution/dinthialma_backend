package com.africa.dinthialma_backend.tontine.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Type de tontine."
            + " ROTATIVE = jackpot tournant distribué à un ou plusieurs membres à chaque cycle ;"
            + " EVENEMENTIELLE = épargne collective vers un événement, chaque membre récupère"
            + " sa mise nette à la clôture finale.")
public enum TontineType {
  ROTATIVE,
  EVENEMENTIELLE
}
