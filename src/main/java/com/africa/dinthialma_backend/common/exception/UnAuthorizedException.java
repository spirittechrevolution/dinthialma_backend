package com.africa.dinthialma_backend.common.exception;

import org.springframework.http.HttpStatus;

public class UnAuthorizedException extends CustomException {

  public UnAuthorizedException(String message) {
    super(HttpStatus.UNAUTHORIZED, message);
  }

  public UnAuthorizedException(String message, ErrorCode code) {
    super(HttpStatus.UNAUTHORIZED, message, code);
  }
}
