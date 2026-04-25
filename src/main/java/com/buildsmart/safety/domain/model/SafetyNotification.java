package com.buildsmart.safety.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "safety_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyNotification {

    @Id
    @Column(name = "notification_id", length = 20)
    private String notificationId;

    // userId of the recipient — stored as String (user lives in IAM DB)
    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    // projectId — stored as String (project lives in BuildSmart DB)
    @Column(name = "project_id", nullable = false, length = 20)
    private String projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SafetyNotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    // ID of the related incident or inspection
    @Column(name = "related_entity_id", length = 20)
    private String relatedEntityId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
