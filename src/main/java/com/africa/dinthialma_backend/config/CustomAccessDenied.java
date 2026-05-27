package com.africa.dinthialma_backend.config;

import com.africa.dinthialma_backend.common.constants.Constants;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Gestionnaire HTTP 403 – réponse JSON normalisée lors d'un accès refusé. */
@Component
public class CustomAccessDenied implements AccessDeniedHandler {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    CustomResponse body =
        new CustomResponse(
            Constants.Message.ERROR_BODY,
            HttpServletResponse.SC_FORBIDDEN,
            "Accès refusé – vous n'avez pas les permissions nécessaires",
            null);

    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
