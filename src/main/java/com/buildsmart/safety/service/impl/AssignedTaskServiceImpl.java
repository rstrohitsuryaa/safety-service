package com.buildsmart.safety.service.impl;

import com.buildsmart.safety.client.PmNotificationClient;
import com.buildsmart.safety.client.UserClient;
import com.buildsmart.safety.client.dto.PmNotificationDto;
import com.buildsmart.safety.client.dto.UserDto;
import com.buildsmart.safety.common.util.IdGeneratorUtil;
import com.buildsmart.safety.domain.model.AssignedTask;
import com.buildsmart.safety.domain.model.AssignedTaskStatus;
import com.buildsmart.safety.domain.model.SafetyNotification;
import com.buildsmart.safety.domain.model.SafetyNotificationType;
import com.buildsmart.safety.domain.repository.AssignedTaskRepository;
import com.buildsmart.safety.domain.repository.NotificationRepository;
import com.buildsmart.safety.exception.UnauthorizedOperationException;
import com.buildsmart.safety.security.JwtUtil;
import com.buildsmart.safety.service.AssignedTaskService;
import com.buildsmart.safety.web.dto.AssignedTaskDtos.AssignedTaskResponse;
import com.buildsmart.safety.web.dto.AssignedTaskDtos.SyncResult;
import com.buildsmart.safety.web.mapper.AssignedTaskMapper;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AssignedTaskServiceImpl implements AssignedTaskService {

    private final AssignedTaskRepository assignedTaskRepository;
    private final NotificationRepository notificationRepository;
    private final PmNotificationClient pmNotificationClient;
    private final UserClient userClient;
    private final JwtUtil jwtUtil;

    @Override
    public SyncResult syncTasksFromPm() {
        String bearerToken = getAuthorizationHeader();
        UserDto officer = resolveCurrentUser(bearerToken);

        if (!"ACTIVE".equals(officer.status())) {
            throw new UnauthorizedOperationException(
                    "Your account is not active. Current status: " + officer.status());
        }
        if (!"SAFETY_OFFICER".equals(officer.role()) && !"ADMIN".equals(officer.role())) {
            throw new UnauthorizedOperationException(
                    "Only SAFETY_OFFICER or ADMIN can sync assigned tasks.");
        }

        // Fetch TASK_ASSIGNED notifications from PM service
        List<PmNotificationDto> pmNotifications;
        try {
            pmNotifications = pmNotificationClient.getNotificationsTo(officer.userId(), bearerToken);
        } catch (FeignException e) {
            log.warn("Could not reach project-service to sync tasks: {}", e.getMessage());
            pmNotifications = List.of();
        }

        int newCount = 0;
        int existedCount = 0;
        List<AssignedTaskResponse> newTasks = new ArrayList<>();

        for (PmNotificationDto notif : pmNotifications) {
            // Only process TASK_ASSIGNED notifications
            if (!"TASK_ASSIGNED".equals(notif.type())) continue;
            if (notif.relatedTaskId() == null || notif.relatedTaskId().isBlank()) continue;

            // Skip already-synced notifications
            if (assignedTaskRepository.existsByPmNotificationId(notif.notificationId())) {
                existedCount++;
                continue;
            }
            // Skip duplicate task IDs (edge case: two notifications for the same task)
            if (assignedTaskRepository.existsByPmTaskId(notif.relatedTaskId())) {
                existedCount++;
                continue;
            }

            // Build AssignedTask from notification data
            AssignedTask last = assignedTaskRepository.findTopByOrderByIdDesc();
            String newId = IdGeneratorUtil.nextAssignedTaskId(last == null ? null : last.getId());

            AssignedTask task = AssignedTask.builder()
                    .id(newId)
                    .pmTaskId(notif.relatedTaskId())
                    .pmNotificationId(notif.notificationId())
                    .projectId(notif.projectId() != null ? notif.projectId() : "")
                    .assignedTo(officer.userId())
                    .assignedBy(notif.notificationFrom() != null ? notif.notificationFrom() : "")
                    .description(buildDescription(notif))
                    .status(AssignedTaskStatus.PENDING)
                    .syncedAt(LocalDateTime.now())
                    .build();

            assignedTaskRepository.save(task);
            newCount++;

            // Create popup SafetyNotification for the safety officer
            createPopupNotification(task, notif, officer.userId());

            newTasks.add(AssignedTaskMapper.toResponse(task));
        }

        log.info("Task sync for officer {}: {} new, {} already existed", officer.userId(), newCount, existedCount);
        return new SyncResult(newCount, existedCount, newTasks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedTaskResponse> getMyTasks() {
        String bearerToken = getAuthorizationHeader();
        UserDto officer = resolveCurrentUser(bearerToken);
        return assignedTaskRepository
                .findByAssignedToOrderBySyncedAtDesc(officer.userId())
                .stream().map(AssignedTaskMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedTaskResponse> getMyTasksByStatus(AssignedTaskStatus status) {
        String bearerToken = getAuthorizationHeader();
        UserDto officer = resolveCurrentUser(bearerToken);
        return assignedTaskRepository
                .findByAssignedToAndStatusOrderBySyncedAtDesc(officer.userId(), status)
                .stream().map(AssignedTaskMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedTaskResponse> getMyTasksForProject(String projectId) {
        String bearerToken = getAuthorizationHeader();
        UserDto officer = resolveCurrentUser(bearerToken);
        return assignedTaskRepository
                .findByAssignedToAndProjectId(officer.userId(), projectId)
                .stream().map(AssignedTaskMapper::toResponse).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String buildDescription(PmNotificationDto notif) {
        // PM message format: "You have been assigned a new task (TASKXXX): <desc>. Planned: <start> to <end>"
        // Use the full PM message as description so no data is lost.
        String base = notif.title() != null ? notif.title() : "";
        if (notif.message() != null && !notif.message().isBlank()) {
            base = notif.message();
        }
        return base.length() > 1000 ? base.substring(0, 1000) : base;
    }

    private void createPopupNotification(AssignedTask task, PmNotificationDto pmNotif, String officerId) {
        try {
            SafetyNotification last = notificationRepository.findAll()
                    .stream()
                    .max(java.util.Comparator.comparing(SafetyNotification::getNotificationId))
                    .orElse(null);
            String notifId = IdGeneratorUtil.nextNotificationId(last == null ? null : last.getNotificationId());

            SafetyNotification notification = SafetyNotification.builder()
                    .notificationId(notifId)
                    .userId(officerId)
                    .projectId(task.getProjectId())
                    .type(SafetyNotificationType.TASK_ASSIGNED)
                    .title("New Task Assigned: " + task.getPmTaskId())
                    .message("Project Manager has assigned you a task on project "
                            + task.getProjectId() + ". Task ID: " + task.getPmTaskId()
                            + ". Details: " + task.getDescription())
                    .relatedEntityId(task.getId())
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to create popup notification for task {}: {}", task.getPmTaskId(), e.getMessage());
        }
    }

    private UserDto resolveCurrentUser(String bearerToken) {
        String token = bearerToken.substring(7);
        try {
            UserClient.IamProfileResponse response = userClient.getCurrentUserProfile(bearerToken);
            if (response == null || response.data() == null) {
                return jwtFallback(token);
            }
            UserClient.UserData d = response.data();
            return new UserDto(d.userId(), d.name(), d.email(), d.role(), d.status());
        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            throw new UnauthorizedOperationException("Invalid or expired token");
        } catch (FeignException e) {
            log.warn("IAM unreachable — falling back to JWT claims");
            return jwtFallback(token);
        }
    }

    private UserDto jwtFallback(String token) {
        return new UserDto(
                jwtUtil.extractUserId(token),
                jwtUtil.extractName(token),
                jwtUtil.extractEmail(token),
                jwtUtil.extractRoles(token).stream().findFirst().orElse(""),
                "ACTIVE"
        );
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
}
