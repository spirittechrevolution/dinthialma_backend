package com.africa.dinthialma_backend.notification.dto;

import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import java.util.UUID;

/**
 * Identifiants facultatifs attachés à une notification pour ouvrir l'écran exact côté front (reçu,
 * tour, fiche membre, demande d'adhésion). Chaque champ peut être null.
 */
public record NotificationLiens(UUID cycleId, UUID cotisationId, UUID membreId, UUID demandeId) {

  /** Liens sans demande d'adhésion. */
  public NotificationLiens(UUID cycleId, UUID cotisationId, UUID membreId) {
    this(cycleId, cotisationId, membreId, null);
  }

  /** Aucun lien précis (la notification ne pointe que vers la tontine, ou vers rien). */
  public static final NotificationLiens AUCUN = new NotificationLiens(null, null, null);

  /** Lien vers une cotisation, son cycle et son membre. */
  public static NotificationLiens cotisation(Cotisation c) {
    return new NotificationLiens(
        c.getCycle() != null ? c.getCycle().getId() : null,
        c.getId(),
        c.getMembre() != null ? c.getMembre().getId() : null);
  }

  /** Lien vers un cycle (et éventuellement le membre concerné). */
  public static NotificationLiens cycle(UUID cycleId, UUID membreId) {
    return new NotificationLiens(cycleId, null, membreId);
  }

  /** Lien vers un membership (fiche membre). */
  public static NotificationLiens membre(UUID membreId) {
    return new NotificationLiens(null, null, membreId);
  }

  /** Lien vers une demande d'adhésion. */
  public static NotificationLiens demande(UUID demandeId) {
    return new NotificationLiens(null, null, null, demandeId);
  }
}
