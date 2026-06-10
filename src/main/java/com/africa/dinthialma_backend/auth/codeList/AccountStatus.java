package com.africa.dinthialma_backend.auth.codeList;

/**
 * Statut d'activation d'un compte utilisateur.
 *
 * <ul>
 *   <li>{@link #ACTIVE} – compte inscrit normalement, accès complet à l'application.
 *   <li>{@link #PRE_ENROLLED} – compte créé automatiquement par un gestionnaire de tontine via le
 *       numéro de téléphone. Pas de mot de passe ni de keycloakId. L'utilisateur doit s'inscrire
 *       sur l'application pour activer son compte et accéder à ses tontines.
 * </ul>
 */
public enum AccountStatus {
  ACTIVE,
  PRE_ENROLLED
}
