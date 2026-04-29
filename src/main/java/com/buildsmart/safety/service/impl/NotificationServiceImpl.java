package com.buildsmart.safety.service.impl;

import com.buildsmart.safety.client.PmNotificationClient;
import com.buildsmart.safety.client.dto.PmInternalNotificationRequest;
import com.buildsmart.safety.common.exception.ResourceNotFoundException;
import com.buildsmart.safety.common.util.IdGeneratorUtil;
import com.buildsmart.safety.domain.model.*;
import com.buildsmart.safety.domain.repository.AssignedTaskRepository;
import com.buildsmart.safety.domain.repository.NotificationRepository;
import com.buildsmart.safety.service.NotificationService;
import com.buildsmart.safety.web.dto.NotificationDtos.NotificationResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final AssignedTaskRepository assignedTaskRepository;
    private final PmNotificationClient pmNotificationClient;

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
    public NotificationResponse markAsRead(String notificationId, String callerUserId) {
        SafetyNotification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (!notification.getUserId().equals(callerUserId)) {
            throw new com.buildsmart.safety.exception.UnauthorizedOperationException(
                    "You are not allowed to mark another user's notification as read.");
        }
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
        // Notify the officer who reported
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

        // Notify the project manager via PM service (writes into PM's DB)
        resolvePmUserId(incident.getProjectId()).ifPresent(pmId ->
            pushToPm(pmId, new PmInternalNotificationRequest(
                    nextId(),
                    incident.getProjectId(),
                    "SAFETY_INCIDENT_REPORTED",
                    "[Safety] New Incident Reported",
                    String.format("Officer %s reported incident %s for project %s with severity %s.",
                            incident.getReportedBy(), incident.getIncidentId(),
                            incident.getProjectId(), incident.getSeverity()),
                    false,
                    java.time.LocalDateTime.now(),
                    incident.getReportedBy(),
                    pmId,
                    null, null, null))
        );
    }

    @Override
    public void notifyIncidentStatusChanged(Incident incident, IncidentStatus oldStatus) {
        // Notify the officer
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

        // Notify the project manager via PM service (writes into PM's DB)
        resolvePmUserId(incident.getProjectId()).ifPresent(pmId ->
            pushToPm(pmId, new PmInternalNotificationRequest(
                    nextId(),
                    incident.getProjectId(),
                    "SAFETY_INCIDENT_UPDATED",
                    "[Safety] Incident Status Updated",
                    String.format("Incident %s (project %s) status changed from %s to %s.",
                            incident.getIncidentId(), incident.getProjectId(), oldStatus, incident.getStatus()),
                    false,
                    java.time.LocalDateTime.now(),
                    incident.getReportedBy(),
                    pmId,
                    null, null, null))
        );
    }

    @Override
    public void notifyInspectionScheduled(SafetyInspection inspection) {
        // Notify the officer
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

        // Notify the project manager via PM service (writes into PM's DB)
        resolvePmUserId(inspection.getProjectId()).ifPresent(pmId ->
            pushToPm(pmId, new PmInternalNotificationRequest(
                    nextId(),
                    inspection.getProjectId(),
                    "SAFETY_INSPECTION_SCHEDULED",
                    "[Safety] New Inspection Scheduled",
                    String.format("Officer %s scheduled a %s inspection (%s) for project %s.",
                            inspection.getOfficerId(), inspection.getInspectionType(),
                            inspection.getInspectionId(), inspection.getProjectId()),
                    false,
                    java.time.LocalDateTime.now(),
                    inspection.getOfficerId(),
                    pmId,
                    null, null, null))
        );
    }

    @Override
    public void notifyInspectionStatusChanged(SafetyInspection inspection, InspectionStatus oldStatus) {
        // Notify the officer
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

        // Notify the project manager via PM service (writes into PM's DB)
        resolvePmUserId(inspection.getProjectId()).ifPresent(pmId ->
            pushToPm(pmId, new PmInternalNotificationRequest(
                    nextId(),
                    inspection.getProjectId(),
                    "SAFETY_INSPECTION_UPDATED",
                    "[Safety] Inspection Status Updated",
                    String.format("Officer %s updated inspection %s (project %s) from %s to %s.",
                            inspection.getOfficerId(), inspection.getInspectionId(),
                            inspection.getProjectId(), oldStatus, inspection.getStatus()),
                    false,
                    java.time.LocalDateTime.now(),
                    inspection.getOfficerId(),
                    pmId,
                    null, null, null))
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Finds the project manager's userId for a given project by looking at who
     * assigned tasks to officers on that project (assignedBy = PM userId).
     * Returns the first distinct PM found, or empty if no tasks exist yet.
     */
    private java.util.Optional<String> resolvePmUserId(String projectId) {
        return assignedTaskRepository.findByProjectIdOrderBySyncedAtDesc(projectId)
                .stream()
                .map(AssignedTask::getAssignedBy)
                .filter(id -> id != null && !id.isBlank())
                .findFirst();
    }

    /**
     * Forwards a notification to the PM service via Feign so it lands
     * in the PM's own database and appears in GET /api/notifications/me.
     * Failures are swallowed — never break the main operation.
     */
    private void pushToPm(String pmId, PmInternalNotificationRequest request) {
        try {
            String bearerToken = getAuthorizationHeader();
            pmNotificationClient.createInternal(request, bearerToken);
        } catch (Exception e) {
            log.warn("Could not push safety notification to PM service for user {}: {}",
                    pmId, e.getMessage());
        }
    }

    private String getAuthorizationHeader() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) throw new IllegalStateException("No active HTTP request");
        HttpServletRequest request = attrs.getRequest();
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) return header;
        throw new IllegalStateException("Authorization header missing");
    }

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
