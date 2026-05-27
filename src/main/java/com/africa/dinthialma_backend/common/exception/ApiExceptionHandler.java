package com.africa.dinthialma_backend.common.exception;

import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Gestionnaire global des exceptions – toutes les erreurs sont normalisées en CustomResponse. */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(TooManyRequestsException.class)
  public ResponseEntity<CustomResponse> handleTooManyRequestsException(
      TooManyRequestsException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(
            new CustomResponse(
                Constants.Message.ERROR_BODY, ex.getStatus().value(), ex.getMessage(), null));
  }

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<CustomResponse> handleCustomException(CustomException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(
            new CustomResponse(
                Constants.Message.ERROR_BODY, ex.getStatus().value(), ex.getMessage(), null));
  }

  @ExceptionHandler(CustomRuntimeException.class)
  public ResponseEntity<CustomResponse> handleCustomRuntimeException(CustomRuntimeException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(
            new CustomResponse(
                Constants.Message.ERROR_BODY, ex.getStatus().value(), ex.getMessage(), null));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<CustomResponse> handleValidationException(
      MethodArgumentNotValidException ex) {
    String errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new CustomResponse(
                Constants.Message.ERROR_BODY,
                HttpStatus.BAD_REQUEST.value(),
                "Données invalides : " + errors,
                null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<CustomResponse> handleGenericException(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new CustomResponse(
                Constants.Message.ERROR_BODY,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erreur interne du serveur",
                null));
  }
}
