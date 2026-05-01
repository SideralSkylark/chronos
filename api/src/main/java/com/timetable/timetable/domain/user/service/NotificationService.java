package com.timetable.timetable.domain.user.service;

import com.timetable.timetable.domain.user.dto.NotificationDto;
import com.timetable.timetable.domain.user.entity.ApplicationUser;
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
                () -> log.warn("User {} not found, skipping notification", userId)
        );
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

        List<ApplicationUser> users = userRepository.findAllByRole(roleEnum);
        for (ApplicationUser user : users) {
            if (excludeUserId == null || !user.getId().equals(excludeUserId)) {
                notify(user.getId(), message);
            }
        }
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
        notificationRepository.findById(id).ifPresent(notification -> {
            if (notification.getUser().getId().equals(userId)) {
                notificationRepository.delete(notification);
            } else {
                throw new org.springframework.security.access.AccessDeniedException("Não tem permissão para eliminar esta notificação.");
            }
        });
    }

    @Transactional
    public void clearReadNotifications(Long userId) {
        notificationRepository.deleteReadByUserId(userId);
    }
}
