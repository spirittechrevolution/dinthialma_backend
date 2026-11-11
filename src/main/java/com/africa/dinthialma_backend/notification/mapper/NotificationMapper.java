package com.africa.dinthialma_backend.notification.mapper;

import com.africa.dinthialma_backend.notification.dto.NotificationResponse;
import com.africa.dinthialma_backend.notification.entity.UserNotification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversions des notifications in-app. */
@Mapper
public interface NotificationMapper {

  @Mapping(target = "isRead", source = "read")
  NotificationResponse toResponse(UserNotification notification);
}
