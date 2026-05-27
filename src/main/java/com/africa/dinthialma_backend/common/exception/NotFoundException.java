package com.africa.dinthialma_backend.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends CustomException {

  public NotFoundException(String message) {
    super(HttpStatus.NOT_FOUND, message);
  }
}
