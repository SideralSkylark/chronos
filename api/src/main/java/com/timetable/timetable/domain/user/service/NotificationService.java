package com.timetable.timetable.domain.user.service;

import com.timetable.timetable.auth.exception.UserNotAuthorizedException;
import com.timetable.timetable.domain.user.dto.NotificationDto;
import com.timetable.timetable.domain.user.entity.Notification;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.repository.NotificationRepository;
import com.timetable.timetable.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void notify(Long userId, String message) {
        userRepository.findById(userId).ifPresentOrElse(
                user -> {
                    Notification notification = Notification.builder()
                            .user(user)
                            .message(message)
                            .readFlag(false)
                            .build();
                    notificationRepository.save(notification);
                    log.info("Notification sent to user {}: {}", userId, message);
                },
                () -> log.warn("User {} not found, skipping notification", userId));
    }

    @Transactional
    public void notifyAllWithRole(String role, String message, Long excludeUserId) {
        UserRole roleEnum;
        try {
            roleEnum = UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid role provided for notification: {}", role);
            return;
        }

        List<Notification> notifications = userRepository.findAllByRole(roleEnum).stream()
            .filter(user -> excludeUserId == null || !user.getId().equals(excludeUserId))
            .map(user -> Notification.builder()
                .user(user)
                .message(message)
                .readFlag(false)
                .build())
            .toList();

        notificationRepository.saveAll(notifications);
        log.info("sent {} notifications for role {}", notifications.size(), roleEnum);
    }

    public List<NotificationDto> getForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDto::from)
                .collect(Collectors.toList());
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadFlagFalse(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        notificationRepository.markAsRead(id, userId);
    }

    @Transactional
    public void deleteNotification(Long id, Long userId) {
        if (!notificationRepository.existsByIdAndUserId(id, userId)) {
            if (notificationRepository.existsById(id)) {
                throw new UserNotAuthorizedException("user %d is not authorized to delete this notification".formatted(userId));
            }

            return;
        }

        notificationRepository.deleteById(id);
    }

    @Transactional
    public void clearReadNotifications(Long userId) {
        notificationRepository.deleteReadByUserId(userId);
    }
}
