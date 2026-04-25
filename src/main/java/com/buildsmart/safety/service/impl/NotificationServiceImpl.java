package com.buildsmart.safety.service.impl;

import com.buildsmart.safety.common.exception.ResourceNotFoundException;
import com.buildsmart.safety.common.util.IdGeneratorUtil;
import com.buildsmart.safety.domain.model.*;
import com.buildsmart.safety.domain.repository.NotificationRepository;
import com.buildsmart.safety.service.NotificationService;
import com.buildsmart.safety.web.dto.NotificationDtos.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    // ── Read Operations ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public NotificationResponse markAsRead(String notificationId) {
        SafetyNotification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        notification.setIsRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    public int markAllAsRead(String userId) {
        return notificationRepository.markAllAsReadForUser(userId);
    }

    // ── Trigger Methods (called internally by Incident/Inspection services) ─

    @Override
    public void notifyIncidentReported(Incident incident) {
        save(SafetyNotification.builder()
                .notificationId(nextId())
                .userId(incident.getReportedBy())
                .projectId(incident.getProjectId())
                .type(SafetyNotificationType.INCIDENT_REPORTED)
                .title("Incident Reported")
                .message(String.format("Incident %s has been reported for project %s with severity %s.",
                        incident.getIncidentId(), incident.getProjectId(), incident.getSeverity()))
                .relatedEntityId(incident.getIncidentId())
                .build());
    }

    @Override
    public void notifyIncidentStatusChanged(Incident incident, IncidentStatus oldStatus) {
        save(SafetyNotification.builder()
                .notificationId(nextId())
                .userId(incident.getReportedBy())
                .projectId(incident.getProjectId())
                .type(SafetyNotificationType.INCIDENT_STATUS_CHANGED)
                .title("Incident Status Updated")
                .message(String.format("Incident %s status changed from %s to %s.",
                        incident.getIncidentId(), oldStatus, incident.getStatus()))
                .relatedEntityId(incident.getIncidentId())
                .build());
    }

    @Override
    public void notifyInspectionScheduled(SafetyInspection inspection) {
        save(SafetyNotification.builder()
                .notificationId(nextId())
                .userId(inspection.getOfficerId())
                .projectId(inspection.getProjectId())
                .type(SafetyNotificationType.INSPECTION_SCHEDULED)
                .title("Inspection Scheduled")
                .message(String.format("%s inspection %s has been scheduled for project %s.",
                        inspection.getInspectionType(), inspection.getInspectionId(), inspection.getProjectId()))
                .relatedEntityId(inspection.getInspectionId())
                .build());
    }

    @Override
    public void notifyInspectionStatusChanged(SafetyInspection inspection, InspectionStatus oldStatus) {
        save(SafetyNotification.builder()
                .notificationId(nextId())
                .userId(inspection.getOfficerId())
                .projectId(inspection.getProjectId())
                .type(SafetyNotificationType.INSPECTION_STATUS_CHANGED)
                .title("Inspection Status Updated")
                .message(String.format("Inspection %s status changed from %s to %s.",
                        inspection.getInspectionId(), oldStatus, inspection.getStatus()))
                .relatedEntityId(inspection.getInspectionId())
                .build());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void save(SafetyNotification notification) {
        try {
            notificationRepository.save(notification);
        } catch (Exception e) {
            // Never let notification failure break the main operation
            log.warn("Failed to save notification: {}", e.getMessage());
        }
    }

    private String nextId() {
        SafetyNotification last = notificationRepository.findAll()
                .stream()
                .max(java.util.Comparator.comparing(SafetyNotification::getNotificationId))
                .orElse(null);
        return IdGeneratorUtil.nextNotificationId(last == null ? null : last.getNotificationId());
    }

    private NotificationResponse toResponse(SafetyNotification n) {
        return new NotificationResponse(
                n.getNotificationId(),
                n.getUserId(),
                n.getProjectId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getRelatedEntityId(),
                n.getIsRead(),
                n.getCreatedAt()
        );
    }
}
