package com.buildsmart.projectmanager.service;

import com.buildsmart.projectmanager.dto.*;
import com.buildsmart.projectmanager.entity.*;
import com.buildsmart.projectmanager.exception.*;
import com.buildsmart.projectmanager.repository.*;
import com.buildsmart.projectmanager.feign.IamServiceClient;
import com.buildsmart.projectmanager.feign.dto.IamAllUsersResponse;
import com.buildsmart.projectmanager.feign.dto.IamUserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMilestoneRepository milestoneRepository;
    private final ProjectTaskRepository taskRepository;
    private final TemplateService templateService;
    private final IdGeneratorService idGeneratorService;
    private final NotificationService notificationService;
    private final IamServiceClient iamServiceClient;

    @Transactional
    public ProjectResponse createProjectFromTemplate(CreateProjectRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate()) || 
            request.getEndDate().isEqual(request.getStartDate())) {
            throw new InvalidDateRangeException(request.getStartDate(), request.getEndDate());
        }

        ProjectTemplate template = templateService.getTemplateEntityById(request.getTemplateId());

        String projectId = idGeneratorService.generateProjectId();

        if (projectRepository.existsByProjectId(projectId)) {
            throw new DuplicateProjectIdException(projectId);
        }

        Project project = Project.builder()
            .projectId(projectId)
            .projectName(request.getProjectName())
            .description(request.getDescription())
            .template(template)
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .status(ProjectStatus.PLANNING)
            .budget(request.getBudget())
            .createdBy(resolveCreatedBy())
            .build();

        project = projectRepository.save(project);

        createMilestonesFromTemplate(project, template);

        return mapToResponse(project);
    }

    private void createMilestonesFromTemplate(Project project, ProjectTemplate template) {
        log.info("Creating milestones for project {} from template {}. Template has {} milestones.", 
            project.getProjectId(), template.getTemplateId(), template.getMilestones().size());
        
        LocalDate currentDate = project.getStartDate();

        for (TemplateMilestone templateMilestone : template.getMilestones()) {
            LocalDate plannedStartDate = currentDate;
            LocalDate plannedEndDate = currentDate.plusDays(templateMilestone.getEstimatedDurationDays());

            ProjectMilestone milestone = ProjectMilestone.builder()
                .milestoneId(project.getProjectId() + "-" + templateMilestone.getMilestoneId())
                .name(templateMilestone.getName())
                .description(templateMilestone.getDescription())
                .orderNumber(templateMilestone.getOrderNumber())
                .status(MilestoneStatus.NOT_STARTED)
                .plannedStartDate(plannedStartDate)
                .plannedEndDate(plannedEndDate)
                .project(project)
                .build();

            milestoneRepository.save(milestone);
            log.debug("Created milestone: {} - {}", milestone.getMilestoneId(), milestone.getName());
            currentDate = plannedEndDate;
        }
        
        log.info("Successfully created {} milestones for project {}", template.getMilestones().size(), project.getProjectId());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(String projectId) {
        Project project = projectRepository.findByProjectId(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    public List<MilestoneResponse> getProjectMilestones(String projectId) {
        return milestoneRepository.findByProjectProjectIdOrderByOrderNumberAsc(projectId)
            .stream()
            .map(this::mapMilestoneToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getProjectTasks(String projectId) {
        List<ProjectTask> tasks = taskRepository.findByProjectProjectId(projectId);
        return tasks.stream()
            .map(this::mapTaskToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public MilestoneResponse updateMilestoneStatus(String milestoneId, MilestoneStatus status) {
        ProjectMilestone milestone = milestoneRepository.findByMilestoneId(milestoneId)
            .orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));

        milestone.setStatus(status);
        
        if (status == MilestoneStatus.IN_PROGRESS && milestone.getActualStartDate() == null) {
            milestone.setActualStartDate(LocalDate.now());
        } else if (status == MilestoneStatus.COMPLETED) {
            milestone.setActualEndDate(LocalDate.now());
        }

        milestone = milestoneRepository.save(milestone);

        // notificationService.notifyMilestoneUpdate(milestone) removed: User entity is no longer present

        return mapMilestoneToResponse(milestone);
    }

    @Transactional
    public TaskResponse updateTaskStatus(String taskId, TaskStatus status) {
        ProjectTask task = taskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        task.setStatus(status);

        if (status == TaskStatus.IN_PROGRESS && task.getActualStart() == null) {
            task.setActualStart(LocalDate.now());
        } else if (status == TaskStatus.COMPLETED) {
            task.setActualEnd(LocalDate.now());
        }

        task = taskRepository.save(task);
        return mapTaskToResponse(task);
    }

    @Transactional
    public TaskResponse createTask(String projectId, CreateTaskRequest request) {
        if (request.getAssignedDepartment() == null) {
            throw new InvalidTaskAssignmentException(
                "Invalid assigned department",
                "Assigned department is required and must match a valid department code."
            );
        }

        // Validate assignedTo user exists in IAM
        IamAllUsersResponse usersResponse = iamServiceClient.getAllUsers(null); // null for Authorization, Feign config injects token
        IamUserProfile assignedUser = usersResponse.data().stream()
                .filter(user -> user.userId().equals(request.getAssignedTo()))
                .findFirst()
                .orElse(null);
        if (assignedUser == null) {
            throw new UserNotFoundException(request.getAssignedTo());
        }

        String assignedUserRole = assignedUser.role() == null ? "" : assignedUser.role().toUpperCase(Locale.ROOT);
        if (DepartmentCode.PROJECT_MANAGER.name().equals(assignedUserRole)) {
            throw new InvalidTaskAssignmentException(
                "Invalid task assignment",
                "Tasks cannot be assigned to users with role PROJECT_MANAGER."
            );
        }

        if (!request.getAssignedDepartment().name().equals(assignedUserRole)) {
            throw new InvalidTaskAssignmentException(
                "Invalid task assignment",
                "Assigned department must match the user's role."
            );
        }

        Project project = projectRepository.findByProjectId(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        String taskId = idGeneratorService.generateTaskId(request.getAssignedDepartment());

        ProjectTask task = ProjectTask.builder()
            .taskId(taskId)
            .description(request.getDescription())
            .assignedDepartment(request.getAssignedDepartment())
            .assignedTo(request.getAssignedTo())
            .assignedBy(resolveCreatedBy())
            .plannedStart(request.getPlannedStart())
            .plannedEnd(request.getPlannedEnd())
            .actualStart(request.getActualStart())
            .actualEnd(request.getActualEnd())
            .status(TaskStatus.PENDING)
            .project(project)
            .build();

        task = taskRepository.save(task);
        log.info("Created task {} for project {}", taskId, projectId);

        // Notify the assigned user
        notificationService.notifyTaskAssignment(task);

        return mapTaskToResponse(task);
    }

    private ProjectResponse mapToResponse(Project project) {
        long totalMilestones = milestoneRepository.countByProjectProjectId(project.getProjectId());
        long completedMilestones = milestoneRepository.countByProjectProjectIdAndStatus(
            project.getProjectId(), MilestoneStatus.COMPLETED);
        
        long totalTasks = taskRepository.findByProjectProjectId(project.getProjectId()).size();
        long completedTasks = taskRepository.countByProjectProjectIdAndStatus(
            project.getProjectId(), TaskStatus.COMPLETED);

        List<MilestoneResponse> milestones = milestoneRepository
            .findByProjectProjectIdOrderByOrderNumberAsc(project.getProjectId())
            .stream()
            .map(this::mapMilestoneToResponse)
            .collect(Collectors.toList());

        return ProjectResponse.builder()
            .projectId(project.getProjectId())
            .projectName(project.getProjectName())
            .description(project.getDescription())
            .templateId(project.getTemplate().getTemplateId())
            .templateName(project.getTemplate().getTemplateName())
            .startDate(project.getStartDate())
            .endDate(project.getEndDate())
            .status(project.getStatus().getDisplayName())
            .budget(project.getBudget())
            .createdBy(project.getCreatedBy())
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .milestones(milestones)
            .totalMilestones((int) totalMilestones)
            .completedMilestones((int) completedMilestones)
            .totalTasks((int) totalTasks)
            .completedTasks((int) completedTasks)
            .build();
    }

    private MilestoneResponse mapMilestoneToResponse(ProjectMilestone milestone) {
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), milestone.getPlannedEndDate());
        boolean isOverdue = daysRemaining < 0 && milestone.getStatus() != MilestoneStatus.COMPLETED;

        return MilestoneResponse.builder()
            .milestoneId(milestone.getMilestoneId())
            .projectId(milestone.getProject().getProjectId())
            .name(milestone.getName())
            .description(milestone.getDescription())
            .order(milestone.getOrderNumber())
            .status(milestone.getStatus().getDisplayName())
            .plannedStartDate(milestone.getPlannedStartDate())
            .plannedEndDate(milestone.getPlannedEndDate())
            .actualStartDate(milestone.getActualStartDate())
            .actualEndDate(milestone.getActualEndDate())
            .daysRemaining((int) daysRemaining)
            .isOverdue(isOverdue)
            .build();
    }

    private TaskResponse mapTaskToResponse(ProjectTask task) {
        return TaskResponse.builder()
            .taskId(task.getTaskId())
            .projectId(task.getProject().getProjectId())
            .description(task.getDescription())
            .assignedDepartment(task.getAssignedDepartment().name())
            .assignedTo(task.getAssignedTo())
            .assignedBy(task.getAssignedBy())
            .plannedStart(task.getPlannedStart())
            .plannedEnd(task.getPlannedEnd())
            .actualStart(task.getActualStart())
            .actualEnd(task.getActualEnd())
            .status(task.getStatus().getDisplayName())
            .build();
    }

    private String resolveCreatedBy() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "unknown";
        }
        return authentication.getName();
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }
}
