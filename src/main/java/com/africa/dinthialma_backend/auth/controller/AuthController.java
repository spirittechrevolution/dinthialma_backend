package com.africa.dinthialma_backend.auth.controller;

import com.africa.dinthialma_backend.auth.dto.*;
import com.africa.dinthialma_backend.auth.service.interfaces.KeycloakAuthService;
import com.africa.dinthialma_backend.auth.service.interfaces.LoginService;
import com.africa.dinthialma_backend.auth.service.interfaces.PasswordResetService;
import com.africa.dinthialma_backend.auth.service.interfaces.PinService;
import com.africa.dinthialma_backend.auth.service.interfaces.RegistrationService;
import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Contrôleur d'authentification – inscription, connexion, déconnexion, reset, PIN. */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentification et gestion du compte utilisateur")
public class AuthController {

  private final LoginService loginService;
  private final KeycloakAuthService keycloakAuthService;
  private final RegistrationService registrationService;
  private final PasswordResetService passwordResetService;
  private final PinService pinService;
  private final RequestHeaderParser requestHeaderParser;

  // ─── Connexion ────────────────────────────────────────────────────

  @PostMapping("/login")
  @Operation(
      summary = "Connexion par téléphone ou email + mot de passe",
      description =
          "L'identifiant peut être le numéro de téléphone (avec ou sans +) ou l'adresse email.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Connexion réussie – tokens JWT retournés"),
    @ApiResponse(responseCode = "401", description = "Identifiants incorrects"),
    @ApiResponse(responseCode = "403", description = "Compte désactivé"),
  })
  public ResponseEntity<CustomResponse> login(@RequestBody @Valid LoginRequest request)
      throws CustomException {
    LoginResponse tokens = loginService.login(request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AUTH_LOGIN_SUCCESS,
            tokens));
  }

  // ─── Déconnexion ──────────────────────────────────────────────────

  @PostMapping("/logout")
  @Operation(summary = "Déconnexion – invalide la session Keycloak")
  @ApiResponse(responseCode = "200", description = "Déconnexion réussie")
  public ResponseEntity<CustomResponse> logout(@RequestBody @Valid LogoutRequest request)
      throws CustomException {
    keycloakAuthService.logout(request.getRefreshToken());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AUTH_LOGOUT_SUCCESS,
            null));
  }

  // ─── Inscription ──────────────────────────────────────────────────

  @PostMapping("/register/send-otp")
  @Operation(summary = "Étape 1/3 – Envoyer un OTP par SMS")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "OTP envoyé"),
    @ApiResponse(responseCode = "409", description = "Téléphone déjà utilisé"),
  })
  public ResponseEntity<CustomResponse> sendOtp(@RequestBody @Valid SendOtpRequest request)
      throws CustomException {
    registrationService.sendOtp(request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AUTH_OTP_SENT,
            null));
  }

  @PostMapping("/register/verify-otp")
  @Operation(summary = "Étape 2/3 – Vérifier l'OTP reçu par SMS")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "OTP vérifié"),
    @ApiResponse(responseCode = "400", description = "OTP invalide ou expiré"),
  })
  public ResponseEntity<CustomResponse> verifyOtp(@RequestBody @Valid VerifyOtpRequest request)
      throws CustomException {
    registrationService.verifyOtp(request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AUTH_OTP_VERIFIED,
            null));
  }

  @PostMapping("/register/complete")
  @Operation(summary = "Étape 3/3 – Finaliser l'inscription")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Compte créé avec succès"),
    @ApiResponse(responseCode = "400", description = "OTP non vérifié ou données invalides"),
    @ApiResponse(responseCode = "409", description = "Téléphone déjà utilisé"),
  })
  public ResponseEntity<CustomResponse> completeRegistration(
      @RequestBody @Valid RegisterCompleteRequest request) throws CustomException {
    RegisterResponse response = registrationService.completeRegistration(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.AUTH_REGISTER_SUCCESS,
                response));
  }

  // ─── Mot de passe oublié ──────────────────────────────────────────

  @PostMapping("/forgot-password/send-otp")
  @Operation(
      summary = "Reset mot de passe 1/3 – Envoyer OTP par SMS",
      description = "Anti-énumération : répond toujours HTTP 200, même si le numéro est inconnu.")
  @ApiResponse(responseCode = "200", description = "OTP envoyé (même réponse si numéro inconnu)")
  public ResponseEntity<CustomResponse> sendForgotPasswordOtp(
      @RequestBody @Valid SendOtpRequest request) throws CustomException {
    passwordResetService.sendForgotPasswordOtp(request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AUTH_OTP_SENT,
            null));
  }

  @PostMapping("/forgot-password/verify-otp")
  @Operation(summary = "Reset mot de passe 2/3 – Vérifier l'OTP")
  @ApiResponse(responseCode = "200", description = "OTP vérifié")
  public ResponseEntity<CustomResponse> verifyForgotPasswordOtp(
      @RequestBody @Valid VerifyOtpRequest request) throws CustomException {
    passwordResetService.verifyForgotPasswordOtp(request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AUTH_OTP_VERIFIED,
            null));
  }

  @PostMapping("/forgot-password/reset")
  @Operation(summary = "Reset mot de passe 3/3 – Choisir un nouveau mot de passe")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Mot de passe réinitialisé"),
    @ApiResponse(responseCode = "400", description = "OTP invalide ou expiré"),
  })
  public ResponseEntity<CustomResponse> resetPasswordByPhone(
      @RequestBody @Valid ResetPasswordByPhoneRequest request) throws CustomException {
    passwordResetService.resetPassword(request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AUTH_PASSWORD_RESET_SUCCESS,
            null));
  }

  // ─── PIN – Configuration ──────────────────────────────────────────

  @PostMapping("/pin/setup")
  @Operation(
      summary = "Configurer le code PIN",
      description = "Accessible uniquement après connexion complète (JWT requis).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Code PIN configuré"),
    @ApiResponse(responseCode = "400", description = "PIN invalide ou confirmation incorrecte"),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou invalide"),
  })
  public ResponseEntity<CustomResponse> setupPin(
      @RequestBody @Valid PinSetupRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    String keycloakId = requestHeaderParser.extractKeycloakId(httpRequest);
    pinService.setupPin(keycloakId, request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AUTH_PIN_SETUP_SUCCESS,
            null));
  }

  // ─── PIN – Connexion rapide ───────────────────────────────────────

  @PostMapping("/login-pin")
  @Operation(
      summary = "Connexion rapide par code PIN (WEB + MOBILE)",
      description =
          "Pré-requis : PIN configuré et session active existante (connexion mot de passe préalable).")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Connexion PIN réussie – tokens JWT"),
    @ApiResponse(responseCode = "400", description = "PIN non configuré"),
    @ApiResponse(responseCode = "401", description = "PIN incorrect ou session expirée"),
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable"),
    @ApiResponse(responseCode = "429", description = "PIN verrouillé (trop de tentatives)"),
  })
  public ResponseEntity<CustomResponse> loginWithPin(@RequestBody @Valid PinLoginRequest request)
      throws CustomException {
    LoginResponse tokens = pinService.loginWithPin(request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AUTH_PIN_LOGIN_SUCCESS,
            tokens));
  }

  // ─── PIN – Reset ─────────────────────────────────────────────────

  @PostMapping("/pin/reset/send-otp")
  @Operation(
      summary = "Reset PIN 1/3 – Envoyer un OTP par SMS",
      description = "Anti-énumération : répond toujours HTTP 200.")
  @ApiResponse(responseCode = "200", description = "OTP envoyé")
  public ResponseEntity<CustomResponse> sendResetPinOtp(@RequestBody @Valid SendOtpRequest request)
      throws CustomException {
    pinService.sendResetPinOtp(request.getPhone());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AUTH_OTP_SENT,
            null));
  }

  @PostMapping("/pin/reset/verify-otp")
  @Operation(summary = "Reset PIN 2/3 – Vérifier l'OTP")
  @ApiResponse(responseCode = "200", description = "OTP vérifié")
  public ResponseEntity<CustomResponse> verifyResetPinOtp(
      @RequestBody @Valid VerifyOtpRequest request) throws CustomException {
    pinService.verifyResetPinOtp(request.getPhone(), request.getCode());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AUTH_OTP_VERIFIED,
            null));
  }

  @PostMapping("/pin/reset")
  @Operation(summary = "Reset PIN 3/3 – Choisir un nouveau code PIN")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Code PIN réinitialisé"),
    @ApiResponse(
        responseCode = "400",
        description = "OTP invalide, PIN faible ou confirmation incorrecte"),
  })
  public ResponseEntity<CustomResponse> resetPin(@RequestBody @Valid PinResetRequest request)
      throws CustomException {
    pinService.resetPin(request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AUTH_PIN_RESET_SUCCESS,
            null));
  }
}
