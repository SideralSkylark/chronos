package com.timetable.timetable.domain.user.controller;

import com.timetable.timetable.common.response.ApiResponse;
import com.timetable.timetable.common.response.ResponseFactory;
import com.timetable.timetable.domain.user.dto.NotificationDto;
import com.timetable.timetable.domain.user.service.NotificationService;
import com.timetable.timetable.security.SecurityUtil;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing user notifications.
 *
 * <p>Provides endpoints for retrieving, marking as read, and deleting notifications for the
 * currently authenticated user.
 *
 * @author Sideral Skylark
 */
@RestController
@RequestMapping("api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

  private final NotificationService notificationService;

  /**
   * Retrieves all notifications for the authenticated user.
   *
   * @return 200 OK with a list of notifications
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotifications() {
    Long userId = SecurityUtil.getAuthenticatedId();
    log.debug("Fetching notifications for user {}", userId);
    return ResponseFactory.ok(
        notificationService.getForUser(userId), "Notifications retrieved successfully");
  }

  /**
   * Retrieves the count of unread notifications for the authenticated user.
   *
   * @return 200 OK with the unread count
   */
  @GetMapping("/unread-count")
  public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
    Long userId = SecurityUtil.getAuthenticatedId();
    log.debug("Fetching unread notification count for user {}", userId);
    return ResponseFactory.ok(
        Map.of("count", notificationService.countUnread(userId)),
        "Unread count retrieved successfully");
  }

  /**
   * Marks all notifications as read for the authenticated user.
   *
   * @return 204 No Content
   */
  @PostMapping("/mark-read")
  public ResponseEntity<Void> markAllRead() {
    Long userId = SecurityUtil.getAuthenticatedId();
    log.debug("Marking all notifications as read for user {}", userId);
    notificationService.markAllRead(userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Marks a specific notification as read.
   *
   * @param id the notification ID
   * @return 204 No Content
   */
  @PostMapping("/{id}/mark-read")
  public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
    Long userId = SecurityUtil.getAuthenticatedId();
    log.debug("Marking notification {} as read for user {}", id, userId);
    notificationService.markAsRead(id, userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Deletes a specific notification.
   *
   * @param id the notification ID
   * @return 204 No Content
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    Long userId = SecurityUtil.getAuthenticatedId();
    log.debug("Deleting notification {} for user {}", id, userId);
    notificationService.deleteNotification(id, userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Clears all read notifications for the authenticated user.
   *
   * @return 204 No Content
   */
  @PostMapping("/clear-read")
  public ResponseEntity<Void> clearRead() {
    Long userId = SecurityUtil.getAuthenticatedId();
    log.debug("Clearing read notifications for user {}", userId);
    notificationService.clearReadNotifications(userId);
    return ResponseEntity.noContent().build();
  }
}
