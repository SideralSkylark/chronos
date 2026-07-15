package com.timetable.timetable.domain.user.dto;

import com.timetable.timetable.domain.user.entity.Notification;
import java.time.LocalDateTime;

/** DTO representing a user notification. */
public record NotificationDto(Long id, String message, boolean read, LocalDateTime createdAt) {
  public static NotificationDto from(Notification notification) {
    return new NotificationDto(
        notification.getId(),
        notification.getMessage(),
        notification.getReadFlag(),
        notification.getCreatedAt());
  }
}
