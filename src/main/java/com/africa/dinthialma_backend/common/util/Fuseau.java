package com.africa.dinthialma_backend.common.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Fuseau horaire de référence de la plateforme : {@code Africa/Dakar} (UTC+0, sans heure d'été).
 * Toutes les échéances, rappels et détections sont exprimés dans ce fuseau.
 */
public final class Fuseau {

  public static final String NOM = "Africa/Dakar";
  public static final ZoneId DAKAR = ZoneId.of(NOM);

  private Fuseau() {}

  /** Date-heure de Dakar → date-heure ISO avec décalage (ex. {@code 2026-11-22T23:59:00Z}). */
  public static OffsetDateTime versOffset(LocalDateTime dateHeure) {
    return dateHeure == null ? null : dateHeure.atZone(DAKAR).toOffsetDateTime();
  }

  /** Instant présent, heure de Dakar. */
  public static LocalDateTime maintenant() {
    return LocalDateTime.now(DAKAR);
  }
}
