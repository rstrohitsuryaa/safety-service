package com.buildsmart.projectmanager.service;

import com.buildsmart.projectmanager.dto.InternalNotificationRequest;
import com.buildsmart.projectmanager.dto.NotificationResponse;
import com.buildsmart.projectmanager.entity.*;
import com.buildsmart.projectmanager.exception.ResourceNotFoundException;
import com.buildsmart.projectmanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final IdGeneratorService idGeneratorService;
    private final ProjectRepository projectRepository;

    @Transactional
    public void notifyTaskAssignment(ProjectTask task) {
        if (task.getAssignedTo() == null || task.getAssignedTo().isBlank()) {
            return;
        }

        Notification notification = Notification.builder()
            .notificationId(idGeneratorService.generateNotificationId())
            .userId(task.getAssignedTo())
            .notificationFrom(task.getAssignedBy())
            .notificationTo(task.getAssignedTo())
            .project(task.getProject())
            .type(NotificationType.TASK_ASSIGNED)
            .title("New Task Assigned: " + task.getTaskId())
            .message("You have been assigned a new task (" + task.getTaskId() + "): " 
                + task.getDescription() + ". Planned: " 
                + task.getPlannedStart() + " to " + task.getPlannedEnd())
            .isRead(false)
            .relatedTask(task)
            .build();

        notificationRepository.save(notification);
    }

    @Transactional
    public NotificationResponse createInternalNotification(InternalNotificationRequest req) {
        // Use provided notificationId if given, otherwise generate one
        String notifId = (req.getNotificationId() != null && !req.getNotificationId().isBlank())
                ? req.getNotificationId()
                : idGeneratorService.generateNotificationId();

        // Resolve project entity — required FK
        Project project = projectRepository.findByProjectId(req.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", req.getProjectId()));

        Notification notification = Notification.builder()
                .notificationId(notifId)
                .userId(req.getNotificationTo())      // who receives it
                .notificationFrom(req.getNotificationFrom())
                .notificationTo(req.getNotificationTo())
                .project(project)
                .type(NotificationType.PROJECT_UPDATE) // generic fallback type
                .title(req.getTitle())
                .message(req.getMessage())
                .isRead(req.getIsRead() != null ? req.getIsRead() : false)
                .build();

        return mapToResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAll()
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(String notificationId) {
        Notification notification = notificationRepository.findByNotificationId(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        notification.setIsRead(true);
        notification = notificationRepository.save(notification);
        return mapToResponse(notification);
    }

    @Transactional
    public int markAllAsRead(String userId) {
        return notificationRepository.markAllAsReadForUser(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForCurrentUser(String userId) {
        return notificationRepository.findByNotificationToOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByFrom(String notificationFrom) {
        return notificationRepository.findByNotificationFromOrderByCreatedAtDesc(notificationFrom)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByTo(String notificationTo) {
        return notificationRepository.findByNotificationToOrderByCreatedAtDesc(notificationTo)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
            .notificationId(notification.getNotificationId())
            .projectId(notification.getProject().getProjectId())
            .type(notification.getType().name())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .isRead(notification.getIsRead())
            .createdAt(notification.getCreatedAt())
            .notificationFrom(notification.getNotificationFrom())
            .notificationTo(notification.getNotificationTo())
            .relatedTaskId(notification.getRelatedTask() != null 
                ? notification.getRelatedTask().getTaskId() : null)
            .relatedApprovalId(notification.getRelatedApproval() != null 
                ? notification.getRelatedApproval().getApprovalId() : null)
            .relatedMilestoneId(notification.getRelatedMilestone() != null 
                ? notification.getRelatedMilestone().getMilestoneId() : null)
            .build();
    }
}
