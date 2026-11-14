package com.africa.dinthialma_backend.tontine.calendrier;

import com.africa.dinthialma_backend.tontine.codeList.StatutEcheance;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.EcheanceCycle;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Règles de paiement par échéance d'un tour (B32, lot 2), sans accès à la base.
 *
 * <ul>
 *   <li>une échéance est <b>ouverte</b> au paiement à partir de son premier jour ; la première
 *       échéance d'un tour l'est dès l'ouverture du tour (une tontine à une échéance par tour se
 *       comporte comme avant B32) ;
 *   <li>l'échéance <b>courante</b> (celle payée par défaut) est la dernière échéance ouverte :
 *       l'échéance en cours, ou la dernière échue si le tour a dépassé sa fin sans être clôturé ;
 *   <li>une échéance ouverte autre que la courante ne se paie qu'en la désignant (régularisation).
 * </ul>
 */
public final class EcheancesTour {

  private EcheancesTour() {}

  /** Échéances non supprimées du tour, dans l'ordre. */
  public static List<EcheanceCycle> actives(CycleTontine cycle) {
    if (cycle == null || cycle.getEcheances() == null) {
      return List.of();
    }
    return cycle.getEcheances().stream()
        .filter(e -> e.getDeletedAt() == null)
        .sorted(Comparator.comparingInt(EcheanceCycle::getNumero))
        .toList();
  }

  /** Vrai si l'échéance accepte un paiement à l'instant {@code maintenant} (heure de Dakar). */
  public static boolean ouverte(EcheanceCycle echeance, LocalDateTime maintenant) {
    return echeance.getNumero() <= 1
        || StatutEcheance.de(echeance, maintenant) != StatutEcheance.A_VENIR;
  }

  /** Vrai si la limite de paiement de l'échéance est passée. */
  public static boolean echue(EcheanceCycle echeance, LocalDateTime maintenant) {
    return StatutEcheance.de(echeance, maintenant) == StatutEcheance.ECHUE;
  }

  /** Échéances ouvertes du tour (dues à ce jour), dans l'ordre. */
  public static List<EcheanceCycle> ouvertes(CycleTontine cycle, LocalDateTime maintenant) {
    return actives(cycle).stream().filter(e -> ouverte(e, maintenant)).toList();
  }

  /** Échéance payée par défaut : la dernière ouverte ; vide si le tour n'en a aucune. */
  public static Optional<EcheanceCycle> courante(CycleTontine cycle, LocalDateTime maintenant) {
    List<EcheanceCycle> ouvertes = ouvertes(cycle, maintenant);
    return ouvertes.isEmpty() ? Optional.empty() : Optional.of(ouvertes.get(ouvertes.size() - 1));
  }
}
