package com.buildsmart.safety.web.controller;

import com.buildsmart.safety.security.JwtUtil;
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
    private final JwtUtil jwtUtil;

    @GetMapping
    @Operation(summary = "Get all notifications for the authenticated user")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(
            @RequestHeader("Authorization") String bearerToken) {
        String userId = jwtUtil.extractUserId(bearerToken.replace("Bearer ", ""));
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count for the authenticated user")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("Authorization") String bearerToken) {
        String userId = jwtUtil.extractUserId(bearerToken.replace("Bearer ", ""));
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read (must belong to the authenticated user)")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable String notificationId,
            @RequestHeader("Authorization") String bearerToken) {
        String userId = jwtUtil.extractUserId(bearerToken.replace("Bearer ", ""));
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, userId));
    }

    @PatchMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read for the authenticated user")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @RequestHeader("Authorization") String bearerToken) {
        String userId = jwtUtil.extractUserId(bearerToken.replace("Bearer ", ""));
        return ResponseEntity.ok(Map.of("markedCount", notificationService.markAllAsRead(userId)));
    }
}
