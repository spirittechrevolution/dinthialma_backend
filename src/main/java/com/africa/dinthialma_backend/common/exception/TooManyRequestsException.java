package com.africa.dinthialma_backend.common.exception;

import org.springframework.http.HttpStatus;

/** Exception 429 – trop de tentatives (verrouillage PIN, rate limiting). */
public class TooManyRequestsException extends CustomException {

  public TooManyRequestsException(String message) {
    super(HttpStatus.TOO_MANY_REQUESTS, message);
  }
}
