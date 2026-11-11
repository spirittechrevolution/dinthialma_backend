package com.africa.dinthialma_backend.common.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Exception 429 – trop de tentatives (verrouillage PIN, rate limiting). */
public class TooManyRequestsException extends CustomException {

  public TooManyRequestsException(String message) {
    super(HttpStatus.TOO_MANY_REQUESTS, message);
  }

  /** Variante avec code stable et délai d'attente renvoyé dans {@code data.retryAfterSeconds}. */
  public TooManyRequestsException(String message, ErrorCode code, long retryAfterSeconds) {
    super(
        HttpStatus.TOO_MANY_REQUESTS,
        message,
        code,
        Map.of("retryAfterSeconds", retryAfterSeconds));
  }
}
