package com.africa.dinthialma_backend.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatusCode;

/**
 * Codes d'erreur stables exposés dans le champ {@code code} de {@code CustomResponse}.
 *
 * <p>Le front doit s'appuyer sur ce code, jamais sur le texte de {@code message} (qui peut être
 * reformulé). Toute erreur porte un code : un code métier précis quand il existe, sinon un code
 * générique déduit du statut HTTP ({@link #fromStatus(HttpStatusCode)}).
 *
 * <p>Règle d'évolution : on ajoute des valeurs, on ne renomme ni ne supprime jamais une valeur
 * existante.
 */
@Schema(description = "Code d'erreur stable (à utiliser côté front à la place du message)")
public enum ErrorCode {

  // ─── Génériques (déduits du statut HTTP) ────────────────────────────────
  BAD_REQUEST,
  /** Champs du corps de requête invalides (Bean Validation). */
  VALIDATION_ERROR,
  /** Paramètre de requête mal typé (enum inconnu, UUID invalide…). */
  INVALID_PARAMETER,
  /** Corps JSON illisible ou paramètre obligatoire manquant. */
  MALFORMED_REQUEST,
  UNAUTHORIZED,
  FORBIDDEN,
  NOT_FOUND,
  METHOD_NOT_ALLOWED,
  CONFLICT,
  TOO_MANY_REQUESTS,
  INTERNAL_ERROR,
  /** Service tiers indisponible (Keycloak…). */
  UPSTREAM_ERROR,

  // ─── Authentification / compte ──────────────────────────────────────────
  INVALID_CREDENTIALS,
  /** Compte créé par un gestionnaire, pas encore activé par inscription. */
  ACCOUNT_PRE_ENROLLED,
  ACCOUNT_DISABLED,
  /** Refresh token refusé : reconnexion complète requise. */
  SESSION_EXPIRED,
  PHONE_ALREADY_USED,
  EMAIL_ALREADY_USED,
  PHONE_UNCHANGED,
  /** Changement de mot de passe : l'ancien mot de passe saisi est incorrect. */
  CURRENT_PASSWORD_INVALID,
  /** Le nouveau mot de passe est identique à l'ancien. */
  PASSWORD_UNCHANGED,

  // ─── Code secret (PIN) ──────────────────────────────────────────────────
  PIN_INVALID,
  PIN_INVALID_FORMAT,
  PIN_TOO_SIMPLE,
  PIN_MISMATCH,
  PIN_NOT_CONFIGURED,
  PIN_EXPIRED,
  /** Verrouillé après trop d'échecs : {@code data.retryAfterSeconds} indique l'attente. */
  PIN_LOCKED,
  /** Aucune session active pour le PIN : connexion complète requise. */
  PIN_SESSION_EXPIRED,

  // ─── Codes SMS (OTP) ────────────────────────────────────────────────────
  OTP_INVALID,
  OTP_EXPIRED,
  /** Étape finale appelée sans OTP vérifié (ou déjà consommé) : refaire l'étape SMS. */
  OTP_NOT_VERIFIED,
  /** Réservé : aucune limitation d'envoi d'OTP n'existe encore côté backend. */
  OTP_RATE_LIMITED,

  // ─── Membres ────────────────────────────────────────────────────────────
  MEMBER_ALREADY_IN_TONTINE,
  JACKPOT_POSITION_TAKEN,
  /** Numéro inconnu : prénom et nom obligatoires pour créer le compte pré-inscrit. */
  MEMBER_NAME_REQUIRED,
  /** Réservé : le nombre maximal de membres n'est pas encore contrôlé à l'ajout. */
  TONTINE_FULL,

  // ─── Cotisations ────────────────────────────────────────────────────────
  /** Une cotisation existe déjà pour ce membre sur ce tour. */
  COTISATION_ALREADY_EXISTS,
  /** Le tour visé n'est pas EN_COURS. */
  NO_CYCLE_IN_PROGRESS,
  AMOUNT_BELOW_MINIMUM,
  /** Montant différent du montant fixe imposé. */
  AMOUNT_MISMATCH,
  /** Cotisation non modifiable (tour clôturé ou cotisation en retard). */
  COTISATION_NOT_EDITABLE,
  /** Action réservée à une cotisation EN_ATTENTE (validation, refus). */
  COTISATION_NOT_PENDING,

  // ─── Cycles ─────────────────────────────────────────────────────────────
  CYCLE_ALREADY_IN_PROGRESS;

  /** Code générique correspondant à un statut HTTP (repli quand aucun code métier n'est fourni). */
  public static ErrorCode fromStatus(HttpStatusCode status) {
    return switch (status.value()) {
      case 400 -> BAD_REQUEST;
      case 401 -> UNAUTHORIZED;
      case 403 -> FORBIDDEN;
      case 404 -> NOT_FOUND;
      case 405 -> METHOD_NOT_ALLOWED;
      case 409 -> CONFLICT;
      case 429 -> TOO_MANY_REQUESTS;
      case 502, 503, 504 -> UPSTREAM_ERROR;
      default -> status.is4xxClientError() ? BAD_REQUEST : INTERNAL_ERROR;
    };
  }
}
