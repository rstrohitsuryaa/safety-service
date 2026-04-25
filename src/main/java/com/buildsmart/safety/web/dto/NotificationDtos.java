package com.buildsmart.safety.web.dto;

import com.buildsmart.safety.domain.model.SafetyNotificationType;

import java.time.LocalDateTime;

public class NotificationDtos {

    public record NotificationResponse(
            String notificationId,
            String userId,
            String projectId,
            SafetyNotificationType type,
            String title,
            String message,
            String relatedEntityId,
            Boolean isRead,
            LocalDateTime createdAt
    ) {}
}
