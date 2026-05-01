package com.timetable.timetable.domain.user.dto;

import java.time.LocalDateTime;

import com.timetable.timetable.domain.user.entity.Notification;

public record NotificationDto(
    Long id,
    String message,
    boolean read,
    LocalDateTime createdAt
) {
    public static NotificationDto from(Notification notification) {
        return new NotificationDto(
            notification.getId(), 
            notification.getMessage(), 
            notification.getReadFlag(), 
            notification.getCreatedAt()
        );
    }
 }
