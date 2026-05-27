package com.africa.dinthialma_backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Réponse API standardisée pour tous les endpoints. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomResponse {

  /** "success" ou "error" */
  private String status;

  /** Code HTTP (ex : 200, 201, 400, 404...) */
  private int statusCode;

  /** Message lisible (ex : "Tontine créée avec succès") */
  private String message;

  /** Données retournées (objet, liste, Page, null pour les 204) */
  private Object data;
}
