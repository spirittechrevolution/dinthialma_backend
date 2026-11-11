package com.africa.dinthialma_backend.common.exception;

import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Exception de base du projet – toutes les exceptions métier étendent celle-ci. */
@Getter
public class CustomException extends Exception {

  private final HttpStatus status;
  private final String message;

  /** Code stable renvoyé au front – null = code générique déduit du statut HTTP. */
  private final ErrorCode code;

  /** Informations complémentaires renvoyées dans {@code data} (ex : retryAfterSeconds). */
  private final Map<String, Object> details;

  public CustomException(HttpStatus status, String message) {
    this(status, message, null, null);
  }

  public CustomException(HttpStatus status, String message, ErrorCode code) {
    this(status, message, code, null);
  }

  public CustomException(
      HttpStatus status, String message, ErrorCode code, Map<String, Object> details) {
    super(message);
    this.status = status;
    this.message = message;
    this.code = code;
    this.details = details;
  }
}
