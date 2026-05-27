package com.africa.dinthialma_backend.auth.service.interfaces;

import com.africa.dinthialma_backend.auth.dto.RegisterCompleteRequest;
import com.africa.dinthialma_backend.auth.dto.RegisterResponse;
import com.africa.dinthialma_backend.auth.dto.SendOtpRequest;
import com.africa.dinthialma_backend.auth.dto.VerifyOtpRequest;
import com.africa.dinthialma_backend.common.exception.CustomException;

/** Service d'inscription – OTP + finalisation du compte. */
public interface RegistrationService {

  /** Envoie un OTP par SMS au numéro fourni. Vérifie que le numéro n'est pas déjà utilisé. */
  void sendOtp(SendOtpRequest request) throws CustomException;

  /**
   * Vérifie l'OTP reçu par SMS (sans le consommer). Le compte sera créé lors de l'étape complète.
   */
  void verifyOtp(VerifyOtpRequest request) throws CustomException;

  /** Finalise l'inscription : crée le compte Keycloak et l'entité User. Consomme l'OTP vérifié. */
  RegisterResponse completeRegistration(RegisterCompleteRequest request) throws CustomException;
}
