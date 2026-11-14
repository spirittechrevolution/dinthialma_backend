package com.africa.dinthialma_backend.tontine.codeList;

import com.africa.dinthialma_backend.tontine.entity.EcheanceCycle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Statut d'une échéance (B32), calculé à partir de l'heure (non enregistré) : A_VENIR avant son
 * premier jour, EN_COURS jusqu'à la fin de sa minute limite, ECHUE ensuite.
 */
@Schema(description = "A_VENIR = pas encore ouverte ; EN_COURS = ouverte ; ECHUE = limite passée")
public enum StatutEcheance {
  A_VENIR,
  EN_COURS,
  ECHUE;

  public static StatutEcheance de(EcheanceCycle echeance, LocalDateTime maintenant) {
    if (maintenant.toLocalDate().isBefore(echeance.getDateDebut())) {
      return A_VENIR;
    }
    LocalDateTime finMinute =
        echeance.getEcheanceAt().truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
    return maintenant.isBefore(finMinute) ? EN_COURS : ECHUE;
  }
}
