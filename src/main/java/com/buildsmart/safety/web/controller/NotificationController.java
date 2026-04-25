package com.buildsmart.safety.web.controller;

import com.buildsmart.safety.service.NotificationService;
import com.buildsmart.safety.web.dto.NotificationDtos.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/safety/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Safety notification APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all notifications for a user")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(@PathVariable String userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/unread-count/{userId}")
    @Operation(summary = "Get unread notification count for a user")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable String notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    @PatchMapping("/mark-all-read/{userId}")
    @Operation(summary = "Mark all notifications as read for a user")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of("markedCount", notificationService.markAllAsRead(userId)));
    }
}
