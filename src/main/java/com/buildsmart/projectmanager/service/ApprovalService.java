package com.buildsmart.projectmanager.service;

import com.buildsmart.projectmanager.dto.CreateApprovalRequest;
import com.buildsmart.projectmanager.entity.*;
import com.buildsmart.projectmanager.exception.*;
import com.buildsmart.projectmanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRequestRepository approvalRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final NotificationService notificationService;
    private final IdGeneratorService idGeneratorService;

    @Transactional
    public ApprovalRequest createApprovalRequest(CreateApprovalRequest request) {
        Project project = projectRepository.findByProjectId(request.getProjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));

        ProjectTask task = taskRepository.findByTaskId(request.getTaskId())
            .orElseThrow(() -> new ResourceNotFoundException("Task", request.getTaskId()));

        String approvalId = request.getApprovalId();
        if (approvalId != null && !approvalId.isBlank()) {
            if (approvalRepository.existsByApprovalId(approvalId)) {
                throw new DuplicateApprovalIdException(approvalId);
            }
        } else {
            approvalId = idGeneratorService.generateApprovalId();
        }

        ApprovalRequest approval = ApprovalRequest.builder()
            .approvalId(approvalId)
            .project(project)
            .task(task)
            .approvalType(ApprovalType.valueOf(request.getApprovalType()))
            .description(request.getDescription())
            .amount(request.getAmount())
            .status(ApprovalStatus.PENDING)
            .requestedByName(request.getRequestedBy() != null ? request.getRequestedBy() : "System")
            .requestedByDepartment(request.getRequestedByDepartment() != null ? DepartmentCode.valueOf(request.getRequestedByDepartment()) : DepartmentCode.FINANCE_OFFICER)
            .requestedAt(LocalDateTime.now())
            .build();

        approval = approvalRepository.save(approval);

        task.setStatus(TaskStatus.AWAITING_APPROVAL);
        taskRepository.save(task);

        return approval;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getAllApprovals() {
        return approvalRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getPendingApprovals() {
        return approvalRepository.findByStatus(ApprovalStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getApprovalsByProject(String projectId) {
        return approvalRepository.findByProjectProjectId(projectId);
    }

    @Transactional
    public ApprovalRequest approveRequest(String approvalId) {
        ApprovalRequest approval = approvalRepository.findByApprovalId(approvalId)
            .orElseThrow(() -> new ResourceNotFoundException("Approval", approvalId));

        approval.setStatus(ApprovalStatus.ACCEPTED);
        approval.setApprovedAt(LocalDateTime.now());

        approval = approvalRepository.save(approval);

        ProjectTask task = approval.getTask();
        task.setStatus(TaskStatus.IN_PROGRESS);
        taskRepository.save(task);

        return approval;
    }

    @Transactional
    public ApprovalRequest rejectRequest(String approvalId, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new MissingRejectionReasonException();
        }

        ApprovalRequest approval = approvalRepository.findByApprovalId(approvalId)
            .orElseThrow(() -> new ResourceNotFoundException("Approval", approvalId));

        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setApprovedAt(LocalDateTime.now());
        approval.setRejectionReason(rejectionReason);

        approval = approvalRepository.save(approval);

        ProjectTask task = approval.getTask();
        task.setStatus(TaskStatus.PENDING);
        taskRepository.save(task);

        return approval;
    }

    @Transactional(readOnly = true)
    public ApprovalStats getApprovalStats() {
        return new ApprovalStats(
            approvalRepository.countByStatus(ApprovalStatus.PENDING),
            approvalRepository.countByStatus(ApprovalStatus.ACCEPTED),
            approvalRepository.countByStatus(ApprovalStatus.REJECTED),
            approvalRepository.count()
        );
    }

    public record ApprovalStats(long pending, long accepted, long rejected, long total) {}
}
