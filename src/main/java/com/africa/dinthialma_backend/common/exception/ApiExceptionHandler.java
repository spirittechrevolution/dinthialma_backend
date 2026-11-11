package com.africa.dinthialma_backend.common.exception;

import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import jakarta.servlet.ServletException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Gestionnaire global des exceptions – toutes les erreurs sont normalisées en CustomResponse.
 *
 * <p>Chaque erreur porte un {@code code} stable ({@link ErrorCode}) : celui de l'exception métier
 * s'il est fourni, sinon un code générique déduit du statut HTTP.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<CustomResponse> handleCustomException(CustomException ex) {
    ErrorCode code = ex.getCode() != null ? ex.getCode() : ErrorCode.fromStatus(ex.getStatus());
    return error(ex.getStatus(), code, ex.getMessage(), ex.getDetails());
  }

  @ExceptionHandler(CustomRuntimeException.class)
  public ResponseEntity<CustomResponse> handleCustomRuntimeException(CustomRuntimeException ex) {
    return error(ex.getStatus(), ErrorCode.fromStatus(ex.getStatus()), ex.getMessage(), null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<CustomResponse> handleValidationException(
      MethodArgumentNotValidException ex) {
    String errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    return error(
        HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Données invalides : " + errors, null);
  }

  /** Paramètre de requête mal typé (ex : valeur d'enum inconnue, UUID invalide) → 400. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<CustomResponse> handleTypeMismatchException(
      MethodArgumentTypeMismatchException ex) {
    return error(
        HttpStatus.BAD_REQUEST,
        ErrorCode.INVALID_PARAMETER,
        "Paramètre invalide : " + ex.getName(),
        null);
  }

  /** Corps JSON illisible → 400 (au lieu de 500). */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<CustomResponse> handleNotReadableException(
      HttpMessageNotReadableException ex) {
    return error(
        HttpStatus.BAD_REQUEST,
        ErrorCode.MALFORMED_REQUEST,
        "Corps de requête illisible ou mal formé",
        null);
  }

  /** Paramètre de requête obligatoire absent → 400. */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<CustomResponse> handleMissingParameterException(
      MissingServletRequestParameterException ex) {
    return error(
        HttpStatus.BAD_REQUEST,
        ErrorCode.MALFORMED_REQUEST,
        "Paramètre obligatoire manquant : " + ex.getParameterName(),
        null);
  }

  /**
   * Erreurs Spring MVC portant leur propre statut (route inconnue → 404, méthode non supportée →
   * 405, type de contenu non supporté → 415…), qui tombaient auparavant en 500.
   */
  @ExceptionHandler({ServletException.class, ErrorResponseException.class})
  public ResponseEntity<CustomResponse> handleSpringErrorResponse(Exception ex) {
    if (!(ex instanceof ErrorResponse errorResponse)) {
      return handleGenericException(ex);
    }
    HttpStatusCode status = errorResponse.getStatusCode();
    String message =
        errorResponse.getBody().getDetail() != null
            ? errorResponse.getBody().getDetail()
            : "Requête refusée";
    return error(status, ErrorCode.fromStatus(status), message, null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<CustomResponse> handleGenericException(Exception ex) {
    log.error("Erreur non gérée", ex);
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.INTERNAL_ERROR,
        "Erreur interne du serveur",
        null);
  }

  private static ResponseEntity<CustomResponse> error(
      HttpStatusCode status, ErrorCode code, String message, Object data) {
    return ResponseEntity.status(status)
        .body(
            new CustomResponse(Constants.Message.ERROR_BODY, status.value(), code, message, data));
  }
}
