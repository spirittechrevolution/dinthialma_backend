package com.africa.dinthialma_backend.common.response;

import com.africa.dinthialma_backend.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Réponse API standardisée pour tous les endpoints. */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"status", "statusCode", "code", "message", "data"})
public class CustomResponse {

  /** "success" ou "error" */
  private String status;

  /** Code HTTP (ex : 200, 201, 400, 404...) */
  private int statusCode;

  /**
   * Code d'erreur stable – présent sur toutes les erreurs, absent sur les succès. Le front doit
   * s'appuyer sur ce code et non sur {@link #message}.
   */
  private ErrorCode code;

  /** Message lisible (ex : "Tontine créée avec succès") */
  private String message;

  /** Données retournées (objet, liste, Page, null pour les 204) ; détails éventuels d'une erreur */
  private Object data;

  /** Réponse de succès (ou d'erreur sans code). */
  public CustomResponse(String status, int statusCode, String message, Object data) {
    this(status, statusCode, null, message, data);
  }

  public CustomResponse(
      String status, int statusCode, ErrorCode code, String message, Object data) {
    this.status = status;
    this.statusCode = statusCode;
    this.code = code;
    this.message = message;
    this.data = data;
  }
}
