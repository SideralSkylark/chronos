package com.timetable.timetable.domain.user.controller;

import com.timetable.timetable.common.response.ApiResponse;
import com.timetable.timetable.common.response.ResponseFactory;
import com.timetable.timetable.domain.user.dto.NotificationDto;
import com.timetable.timetable.domain.user.service.NotificationService;
import com.timetable.timetable.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotifications() {
        Long userId = SecurityUtil.getAuthenticatedId();
        log.debug("Fetching notifications for user {}", userId);
        return ResponseFactory.ok(
                notificationService.getForUser(userId),
                "Notifications retrieved successfully"
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        Long userId = SecurityUtil.getAuthenticatedId();
        log.debug("Fetching unread notification count for user {}", userId);
        return ResponseFactory.ok(
                Map.of("count", notificationService.countUnread(userId)),
                "Unread count retrieved successfully"
        );
    }

    @PostMapping("/mark-read")
    public ResponseEntity<Void> markAllRead() {
        Long userId = SecurityUtil.getAuthenticatedId();
        log.debug("Marking all notifications as read for user {}", userId);
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/mark-read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtil.getAuthenticatedId();
        log.debug("Marking notification {} as read for user {}", id, userId);
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtil.getAuthenticatedId();
        log.debug("Deleting notification {} for user {}", id, userId);
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/clear-read")
    public ResponseEntity<Void> clearRead() {
        Long userId = SecurityUtil.getAuthenticatedId();
        log.debug("Clearing read notifications for user {}", userId);
        notificationService.clearReadNotifications(userId);
        return ResponseEntity.noContent().build();
    }
}
