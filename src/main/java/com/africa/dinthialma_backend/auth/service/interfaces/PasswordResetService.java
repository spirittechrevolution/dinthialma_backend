package com.africa.dinthialma_backend.auth.service.interfaces;

import com.africa.dinthialma_backend.auth.dto.ResetPasswordByPhoneRequest;
import com.africa.dinthialma_backend.auth.dto.SendOtpRequest;
import com.africa.dinthialma_backend.auth.dto.VerifyOtpRequest;
import com.africa.dinthialma_backend.common.exception.CustomException;

/**
 * Service de réinitialisation du mot de passe via OTP SMS.
 *
 * <p>Flux en 3 étapes :
 *
 * <ol>
 *   <li>{@link #sendForgotPasswordOtp} – envoie un OTP (anti-énumération : toujours HTTP 200)
 *   <li>{@link #verifyForgotPasswordOtp} – vérifie le code sans le consommer
 *   <li>{@link #resetPassword} – applique le nouveau mot de passe et révoque les sessions PIN
 * </ol>
 */
public interface PasswordResetService {

  /**
   * Envoie un OTP par SMS pour la réinitialisation du mot de passe.
   *
   * <p><strong>Anti-énumération</strong> : répond toujours HTTP 200 même si le numéro n'existe pas.
   * Si le numéro est connu, l'OTP est envoyé ; sinon, l'appel est silencieux.
   *
   * @param request numéro de téléphone du demandeur
   */
  void sendForgotPasswordOtp(SendOtpRequest request) throws CustomException;

  /**
   * Vérifie l'OTP de réinitialisation (sans le consommer).
   *
   * @param request téléphone + code OTP
   */
  void verifyForgotPasswordOtp(VerifyOtpRequest request) throws CustomException;

  /**
   * Applique le nouveau mot de passe et révoque toutes les sessions PIN actives.
   *
   * @param request téléphone + OTP (re-validé) + nouveau mot de passe
   */
  void resetPassword(ResetPasswordByPhoneRequest request) throws CustomException;
}
