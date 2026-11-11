package com.africa.dinthialma_backend.auth.service.impl;

import com.africa.dinthialma_backend.auth.dto.LoginRequest;
import com.africa.dinthialma_backend.auth.dto.RegisterCompleteRequest;
import com.africa.dinthialma_backend.auth.dto.RegisterResponse;
import com.africa.dinthialma_backend.auth.service.interfaces.InscriptionService;
import com.africa.dinthialma_backend.auth.service.interfaces.LoginService;
import com.africa.dinthialma_backend.auth.service.interfaces.RegistrationService;
import com.africa.dinthialma_backend.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Volontairement sans transaction englobante : {@link RegistrationService#completeRegistration} est
 * validé dans sa propre transaction avant la tentative de connexion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class InscriptionServiceImpl implements InscriptionService {

  private final RegistrationService registrationService;
  private final LoginService loginService;

  @Override
  public RegisterResponse finaliserInscription(RegisterCompleteRequest request)
      throws CustomException {
    RegisterResponse response = registrationService.completeRegistration(request);

    if (request.getClientType() != null) {
      try {
        LoginRequest login = new LoginRequest();
        login.setUsername(response.getPhone());
        login.setPassword(request.getPassword());
        login.setClientType(request.getClientType());
        login.setDeviceInfo(request.getDeviceInfo());
        response.setTokens(loginService.login(login));
      } catch (Exception e) {
        log.warn(
            "[Register] Connexion automatique impossible pour userId={} : {}",
            response.getUserId(),
            e.getMessage());
      }
    }
    return response;
  }
}
