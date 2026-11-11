package com.africa.dinthialma_backend.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.notification.codeList.NotificationType;
import com.africa.dinthialma_backend.notification.dto.NotificationLiens;
import com.africa.dinthialma_backend.notification.dto.NotificationResponse;
import com.africa.dinthialma_backend.notification.entity.UserNotification;
import com.africa.dinthialma_backend.notification.mapper.NotificationMapper;
import com.africa.dinthialma_backend.notification.repository.NotificationRepository;
import com.africa.dinthialma_backend.support.Fixtures;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl")
class NotificationServiceImplTest {

  @Mock private NotificationRepository notificationRepository;
  @Mock private UserRepository userRepository;

  private NotificationServiceImpl service;
  private User user;

  @BeforeEach
  void setUp() {
    service =
        new NotificationServiceImpl(
            notificationRepository, Mappers.getMapper(NotificationMapper.class), userRepository);
    user = Fixtures.user("Awa");
  }

  @Test
  @DisplayName("notify enregistre la notification non lue avec ses liens précis")
  void notifyAvecLiens() {
    UUID tontineId = UUID.randomUUID();
    UUID cycleId = UUID.randomUUID();
    UUID cotisationId = UUID.randomUUID();
    UUID membreId = UUID.randomUUID();
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    service.notify(
        user.getId(),
        NotificationType.COTISATION_VALIDEE,
        "Titre",
        "Corps",
        tontineId,
        new NotificationLiens(cycleId, cotisationId, membreId));

    ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
    verify(notificationRepository).save(captor.capture());
    UserNotification n = captor.getValue();
    assertThat(n.getUser()).isSameAs(user);
    assertThat(n.getType()).isEqualTo(NotificationType.COTISATION_VALIDEE);
    assertThat(n.isRead()).isFalse();
    assertThat(n.getTontineId()).isEqualTo(tontineId);
    assertThat(n.getCycleId()).isEqualTo(cycleId);
    assertThat(n.getCotisationId()).isEqualTo(cotisationId);
    assertThat(n.getMembreId()).isEqualTo(membreId);
  }

  @Test
  @DisplayName("notify sans liens (ou liens null) n'en renseigne aucun")
  void notifySansLiens() {
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    service.notify(user.getId(), NotificationType.CYCLE_OUVERT, "T", "C", null);
    service.notify(user.getId(), NotificationType.CYCLE_OUVERT, "T", "C", null, null);

    ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
    verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
    assertThat(captor.getAllValues())
        .allSatisfy(
            n -> {
              assertThat(n.getCycleId()).isNull();
              assertThat(n.getCotisationId()).isNull();
              assertThat(n.getMembreId()).isNull();
            });
  }

  @Test
  @DisplayName("notify n'échoue jamais : utilisateur inconnu ou erreur base -> rien n'est propagé")
  void notifyNeLeveJamais() {
    UUID inconnu = UUID.randomUUID();
    when(userRepository.findById(inconnu)).thenReturn(Optional.empty());
    assertThatCode(() -> service.notify(inconnu, NotificationType.CYCLE_OUVERT, "T", "C", null))
        .doesNotThrowAnyException();
    verify(notificationRepository, never()).save(any());

    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(notificationRepository.save(any())).thenThrow(new RuntimeException("base indisponible"));
    assertThatCode(
            () -> service.notify(user.getId(), NotificationType.CYCLE_OUVERT, "T", "C", null))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("getMyNotifications renvoie la page convertie (champ read)")
  void mesNotifications() throws Exception {
    Pageable page = PageRequest.of(0, 20);
    UserNotification n =
        UserNotification.builder()
            .id(UUID.randomUUID())
            .user(user)
            .type(NotificationType.JACKPOT_DISTRIBUE)
            .title("Jackpot")
            .body("Bravo")
            .isRead(true)
            .build();
    when(userRepository.findByKeycloakId(user.getKeycloakId())).thenReturn(Optional.of(user));
    when(notificationRepository.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
            user.getId(), page))
        .thenReturn(new PageImpl<>(List.of(n), page, 1));

    List<NotificationResponse> contenu =
        service.getMyNotifications(user.getKeycloakId(), page).getContent();

    assertThat(contenu).hasSize(1);
    assertThat(contenu.get(0).getTitle()).isEqualTo("Jackpot");
    assertThat(contenu.get(0).isRead()).isTrue();
  }

  @Test
  @DisplayName("countUnread délègue au compteur du repository")
  void compteurNonLues() throws Exception {
    when(userRepository.findByKeycloakId(user.getKeycloakId())).thenReturn(Optional.of(user));
    when(notificationRepository.countByUser_IdAndIsReadFalseAndDeletedAtIsNull(user.getId()))
        .thenReturn(3L);
    assertThat(service.countUnread(user.getKeycloakId())).isEqualTo(3L);
  }

  @Test
  @DisplayName("markAsRead : marque la notification de l'appelant ; inconnue -> 404")
  void marquerLue() throws Exception {
    UUID id = UUID.randomUUID();
    UserNotification n = UserNotification.builder().id(id).user(user).isRead(false).build();
    when(userRepository.findByKeycloakId(user.getKeycloakId())).thenReturn(Optional.of(user));
    when(notificationRepository.findByIdAndUser_IdAndDeletedAtIsNull(id, user.getId()))
        .thenReturn(Optional.of(n));

    service.markAsRead(user.getKeycloakId(), id);
    assertThat(n.isRead()).isTrue();
    verify(notificationRepository).save(n);

    UUID autre = UUID.randomUUID();
    when(notificationRepository.findByIdAndUser_IdAndDeletedAtIsNull(autre, user.getId()))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.markAsRead(user.getKeycloakId(), autre))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("markAllAsRead : mise à jour groupée ; utilisateur inconnu -> 404")
  void toutMarquerLu() throws Exception {
    when(userRepository.findByKeycloakId(user.getKeycloakId())).thenReturn(Optional.of(user));
    service.markAllAsRead(user.getKeycloakId());
    verify(notificationRepository).markAllReadByUserId(user.getId());

    when(userRepository.findByKeycloakId("inconnu")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.markAllAsRead("inconnu"))
        .isInstanceOf(NotFoundException.class);
  }
}
