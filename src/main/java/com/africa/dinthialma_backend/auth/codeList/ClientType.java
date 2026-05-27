package com.africa.dinthialma_backend.auth.codeList;

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
public enum ClientType {

  /** Navigateur web (Chrome, Firefox, Safari…). */
  WEB,

  /** Application mobile iOS ou Android. */
  MOBILE
}
