package com.africa.dinthialma_backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Version RuntimeException de CustomException (non-checked). */
@Getter
public class CustomRuntimeException extends RuntimeException {

  private final HttpStatus status;
  private final String message;

  public CustomRuntimeException(HttpStatus status, String message) {
    super(message);
    this.status = status;
    this.message = message;
  }
}
