package com.africa.dinthialma_backend.auth.codeList;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Type de client à l'origine d'une session PIN.
 *
 * <p>Détermine les politiques appliquées par le service de session :
 *
 * <ul>
 *   <li>{@link #WEB} → TTL court (24 h), user-agent navigateur
 *   <li>{@link #MOBILE} → TTL long (30 jours), device_id applicatif
 * </ul>
 */
@Schema(
    description =
        "Type de client pour les sessions PIN. WEB = navigateur (TTL 24 h),"
            + " MOBILE = application iOS/Android (TTL 30 jours).")
public enum ClientType {

  /** Navigateur web (Chrome, Firefox, Safari…). */
  WEB,

  /** Application mobile iOS ou Android. */
  MOBILE
}
