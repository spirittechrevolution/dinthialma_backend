package com.africa.dinthialma_backend.common.constants;

import java.util.Set;

public final class Constants {

  private Constants() {}

  public static final class Message {
    public static final String SUCCESS_BODY = "success";
    public static final String ERROR_BODY = "error";

    private Message() {}
  }

  public static final class Status {
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int NO_CONTENT = 204;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int TOO_MANY_REQUESTS = 429;
    public static final int INTERNAL_ERROR = 500;

    private Status() {}
  }

  /** Objectifs d'un OTP – détermine le flux et le service qui le consomme. */
  public static final class OtpPurpose {
    public static final String REGISTRATION = "REGISTRATION";
    public static final String RESET_PASSWORD = "RESET_PASSWORD";
    public static final String RESET_PIN = "RESET_PIN";
    public static final String PHONE_CHANGE = "PHONE_CHANGE";

    private OtpPurpose() {}
  }

  /** Paramètres du code PIN (connexion rapide). */
  public static final class Pin {
    /** Longueur exacte requise : 6 chiffres. */
    public static final int LENGTH = 6;

    /** Nombre maximal de tentatives incorrectes avant verrouillage. */
    public static final int MAX_ATTEMPTS = 5;

    /** Durée du verrouillage en minutes après MAX_ATTEMPTS échecs. */
    public static final int LOCK_DURATION_MINUTES = 30;

    /**
     * Durée de validité du PIN en jours. Passé ce délai, l'utilisateur doit reconfigurer son PIN.
     */
    public static final long EXPIRY_DAYS = 90;

    /**
     * PINs triviaux interdits (suites, répétitions). Le service valide que le PIN fourni n'est pas
     * dans cette liste.
     */
    public static final Set<String> FORBIDDEN_PINS =
        Set.of(
            "123456", "654321", "000000", "111111", "222222", "333333", "444444", "555555",
            "666666", "777777", "888888", "999999", "112233", "121212", "123123");

    private Pin() {}
  }

  /** TTL des sessions PIN selon le type de client. */
  public static final class Session {
    /** Durée de vie d'une session WEB en heures. */
    public static final long WEB_TTL_HOURS = 24;

    /** Durée de vie d'une session MOBILE en jours. */
    public static final long MOBILE_TTL_DAYS = 30;

    private Session() {}
  }

  /** Paramètres des OTP. */
  public static final class Otp {
    /** Durée de validité d'un OTP en minutes. */
    public static final long EXPIRY_MINUTES = 10;

    private Otp() {}
  }
}
