package com.africa.dinthialma_backend.config;

import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Gestionnaire HTTP 401 – réponse JSON normalisée lors d'une tentative non authentifiée. */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    CustomResponse body =
        new CustomResponse(
            Constants.Message.ERROR_BODY,
            HttpServletResponse.SC_UNAUTHORIZED,
            "Authentification requise – token JWT manquant ou invalide",
            null);

    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
