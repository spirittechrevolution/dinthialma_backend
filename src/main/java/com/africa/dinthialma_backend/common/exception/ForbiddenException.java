package com.africa.dinthialma_backend.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends CustomException {

  public ForbiddenException(String message) {
    super(HttpStatus.FORBIDDEN, message);
  }

  public ForbiddenException(String message, ErrorCode code) {
    super(HttpStatus.FORBIDDEN, message, code);
  }
}
