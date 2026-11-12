package com.africa.dinthialma_backend.auth.service.interfaces;

import com.africa.dinthialma_backend.auth.dto.RegisterCompleteRequest;
import com.africa.dinthialma_backend.auth.dto.RegisterResponse;
import com.africa.dinthialma_backend.common.exception.CustomException;

/** Orchestration de l'étape finale d'inscription (création du compte + connexion automatique). */
public interface InscriptionService {

  /**
   * Finalise l'inscription puis, si {@code clientType} est fourni, connecte l'utilisateur (B13).
   *
   * <p>La connexion n'a lieu qu'après la validation en base de l'inscription : un échec de
   * connexion n'annule jamais un compte créé ; les jetons sont alors simplement absents de la
   * réponse.
   */
  RegisterResponse finaliserInscription(RegisterCompleteRequest request) throws CustomException;
}
