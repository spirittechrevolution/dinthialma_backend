package com.africa.dinthialma_backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Exception de base du projet – toutes les exceptions métier étendent celle-ci. */
@Getter
public class CustomException extends Exception {

  private final HttpStatus status;
  private final String message;

  public CustomException(HttpStatus status, String message) {
    super(message);
    this.status = status;
    this.message = message;
  }
}
