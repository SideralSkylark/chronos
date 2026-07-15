package com.timetable.timetable.domain.user.service;

import com.timetable.timetable.auth.exception.UserNotAuthorizedException;
import com.timetable.timetable.domain.user.dto.NotificationDto;
import com.timetable.timetable.domain.user.entity.Notification;
import com.timetable.timetable.domain.user.entity.UserRole;
import com.timetable.timetable.domain.user.repository.NotificationRepository;
import com.timetable.timetable.domain.user.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user notifications.
 *
 * <p>Handles creating notifications for specific users or roles, retrieving notifications, and
 * managing their read/delete status.
 *
 * @author Sideral Skylark
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;

  /**
   * Sends a notification to a specific user.
   *
   * @param userId the ID of the user to notify
   * @param message the notification message
   */
  @Transactional
  public void notify(Long userId, String message) {
    userRepository
        .findById(userId)
        .ifPresentOrElse(
            user -> {
              Notification notification =
                  Notification.builder().user(user).message(message).readFlag(false).build();
              notificationRepository.save(notification);
              log.info("Notification sent to user {}: {}", userId, message);
            },
            () -> log.warn("User {} not found, skipping notification", userId));
  }

  /**
   * Sends a notification to all users with a specific role.
   *
   * @param role the role name
   * @param message the notification message
   * @param excludeUserId optional user ID to exclude from the notification
   */
  @Transactional
  public void notifyAllWithRole(String role, String message, Long excludeUserId) {
    UserRole roleEnum;
    try {
      roleEnum = UserRole.valueOf(role.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.error("Invalid role provided for notification: {}", role);
      return;
    }

    List<Notification> notifications =
        userRepository.findAllByRole(roleEnum).stream()
            .filter(user -> excludeUserId == null || !user.getId().equals(excludeUserId))
            .map(user -> Notification.builder().user(user).message(message).readFlag(false).build())
            .toList();

    notificationRepository.saveAll(notifications);
    log.info("sent {} notifications for role {}", notifications.size(), roleEnum);
  }

  /**
   * Retrieves all notifications for a user, ordered by creation date (newest first).
   *
   * @param userId the user ID
   * @return a list of notification DTOs
   */
  public List<NotificationDto> getForUser(Long userId) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(NotificationDto::from)
        .collect(Collectors.toList());
  }

  /**
   * Counts the number of unread notifications for a user.
   *
   * @param userId the user ID
   * @return the unread count
   */
  public long countUnread(Long userId) {
    return notificationRepository.countByUserIdAndReadFlagFalse(userId);
  }

  /**
   * Marks all notifications as read for a user.
   *
   * @param userId the user ID
   */
  @Transactional
  public void markAllRead(Long userId) {
    notificationRepository.markAllReadByUserId(userId);
  }

  /**
   * Marks a specific notification as read.
   *
   * @param id the notification ID
   * @param userId the user ID (for verification)
   */
  @Transactional
  public void markAsRead(Long id, Long userId) {
    notificationRepository.markAsRead(id, userId);
  }

  /**
   * Deletes a specific notification.
   *
   * @param id the notification ID
   * @param userId the user ID (for authorization)
   * @throws UserNotAuthorizedException if the notification does not belong to the user
   */
  @Transactional
  public void deleteNotification(Long id, Long userId) {
    if (!notificationRepository.existsByIdAndUserId(id, userId)) {
      if (notificationRepository.existsById(id)) {
        throw new UserNotAuthorizedException(
            "user %d is not authorized to delete this notification".formatted(userId));
      }

      return;
    }

    notificationRepository.deleteById(id);
  }

  /**
   * Deletes all read notifications for a user.
   *
   * @param userId the user ID
   */
  @Transactional
  public void clearReadNotifications(Long userId) {
    notificationRepository.deleteReadByUserId(userId);
  }
}
