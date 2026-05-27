package com.africa.dinthialma_backend.common.util;

import java.security.SecureRandom;

/** Utilitaire de génération d'OTP numérique à 6 chiffres. */
public final class OtpUtils {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int OTP_LENGTH = 6;

  private OtpUtils() {}

  /** Génère un OTP numérique de 6 chiffres. */
  public static String generateOtp() {
    int bound = (int) Math.pow(10, OTP_LENGTH);
    int otp = RANDOM.nextInt(bound);
    return String.format("%0" + OTP_LENGTH + "d", otp);
  }
}
