package com.africa.dinthialma_backend.notification.controller;

import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.response.CustomResponse;
import com.africa.dinthialma_backend.common.util.RequestHeaderParser;
import com.africa.dinthialma_backend.notification.dto.NotificationResponse;
import com.africa.dinthialma_backend.notification.service.interfaces.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints notifications in-app de l'utilisateur connecté. */
@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notifications in-app de l'utilisateur connecté")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

  private static final String SUCCESS = "success";
  private static final int OK = 200;

  private final NotificationService notificationService;
  private final RequestHeaderParser headerParser;

  @GetMapping
  @Operation(
      summary = "Mes notifications",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\n"
              + "Retourne toutes les notifications de l'utilisateur (lues et non lues) triées par"
              + " date décroissante. Le frontend peut distinguer NOUVELLES (isRead=false) et"
              + " PRÉCÉDENTES (isRead=true) en filtrant côté client.\n\n"
              + "Pagination : `?page=0&size=20`")
  public ResponseEntity<CustomResponse> getMyNotifications(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    Page<NotificationResponse> notifications =
        notificationService.getMyNotifications(keycloakId, pageable);
    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, "Notifications récupérées", notifications));
  }

  @GetMapping("/unread-count")
  @Operation(
      summary = "Nombre de notifications non lues",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\nRetourne le compteur pour le badge"
              + " (cloche). Appeler à chaque montée en premier plan de l'application.")
  public ResponseEntity<CustomResponse> countUnread(HttpServletRequest httpRequest)
      throws CustomException {
    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    long count = notificationService.countUnread(keycloakId);
    return ResponseEntity.ok(new CustomResponse(SUCCESS, OK, "Notifications non lues", count));
  }

  @PatchMapping("/{id}/read")
  @Operation(
      summary = "Marquer une notification comme lue",
      description = "🔒 Rôles : tout utilisateur authentifié. Idempotent.")
  public ResponseEntity<CustomResponse> markAsRead(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    notificationService.markAsRead(keycloakId, id);
    return ResponseEntity.ok(new CustomResponse(SUCCESS, OK, "Notification marquée lue", null));
  }

  @PatchMapping("/read-all")
  @Operation(
      summary = "Tout marquer comme lu",
      description =
          "🔒 Rôles : tout utilisateur authentifié.\n\nMarque toutes les notifications non lues"
              + " comme lues en une seule opération (bouton « Tout lire »).")
  public ResponseEntity<CustomResponse> markAllAsRead(HttpServletRequest httpRequest)
      throws CustomException {
    String keycloakId = headerParser.extractKeycloakId(httpRequest);
    notificationService.markAllAsRead(keycloakId);
    return ResponseEntity.ok(
        new CustomResponse(SUCCESS, OK, "Toutes les notifications marquées lues", null));
  }
}
