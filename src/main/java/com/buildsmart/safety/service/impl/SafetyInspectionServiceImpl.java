package com.buildsmart.safety.service.impl;

import com.buildsmart.safety.client.ProjectClient;
import com.buildsmart.safety.client.UserClient;
import com.buildsmart.safety.client.dto.ProjectDto;
import com.buildsmart.safety.client.dto.UserDto;
import com.buildsmart.safety.common.exception.DuplicateResourceException;
import com.buildsmart.safety.common.exception.ResourceNotFoundException;
import com.buildsmart.safety.common.util.IdGeneratorUtil;
import com.buildsmart.safety.domain.model.InspectionStatus;
import com.buildsmart.safety.domain.model.SafetyInspection;
import com.buildsmart.safety.domain.repository.SafetyInspectionRepository;
import com.buildsmart.safety.exception.InvalidStatusTransitionException;
import com.buildsmart.safety.exception.UnauthorizedOperationException;
import com.buildsmart.safety.security.JwtUtil;
import com.buildsmart.safety.service.SafetyInspectionService;
import com.buildsmart.safety.service.NotificationService;
import com.buildsmart.safety.validator.SafetyInspectionValidator;
import com.buildsmart.safety.web.dto.InspectionDtos.CreateInspectionRequest;
import com.buildsmart.safety.web.dto.InspectionDtos.InspectionResponse;
import com.buildsmart.safety.web.mapper.InspectionMapper;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SafetyInspectionServiceImpl implements SafetyInspectionService {

    private final SafetyInspectionRepository inspectionRepository;
    private final SafetyInspectionValidator inspectionValidator;
    private final ProjectClient projectClient;
    private final UserClient userClient;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;

    private static final Map<InspectionStatus, Set<InspectionStatus>> ALLOWED_TRANSITIONS = Map.of(
            InspectionStatus.SCHEDULED,     Set.of(InspectionStatus.IN_PROGRESS),
            InspectionStatus.IN_PROGRESS,   Set.of(InspectionStatus.COMPLETED, InspectionStatus.NON_COMPLIANT),
            InspectionStatus.COMPLETED,     Set.of(InspectionStatus.CLOSED),
            InspectionStatus.NON_COMPLIANT, Set.of(InspectionStatus.CLOSED),
            InspectionStatus.CLOSED,        Set.of()
    );

    @Override
    public InspectionResponse create(CreateInspectionRequest request) {
        inspectionValidator.validate(request);

        // Resolve current officer from IAM (live — gets fresh name + status)
        UserDto officer = resolveCurrentUser();

        // Guard: user must be ACTIVE
        if (!"ACTIVE".equals(officer.status())) {
            throw new UnauthorizedOperationException(
                    "Your account is not active. Current status: " + officer.status());
        }

        // Role guard
        if (!"SAFETY_OFFICER".equals(officer.role()) && !"ADMIN".equals(officer.role())) {
            throw new UnauthorizedOperationException(
                    "Only users with role SAFETY_OFFICER can schedule inspections");
        }

        // Validate project via project-service
        ProjectDto project = resolveProject(request.projectId());

        // Guard: project must be active
        if ("Completed".equals(project.status()) || "Cancelled".equals(project.status())) {
            throw new UnauthorizedOperationException(
                    "Cannot schedule an inspection for a project with status: " + project.status());
        }

        // Duplicate guard
        List<InspectionStatus> activeStatuses = List.of(InspectionStatus.SCHEDULED, InspectionStatus.IN_PROGRESS);
        if (inspectionRepository.existsByProjectIdAndDateAndInspectionTypeAndStatusIn(
                request.projectId(), LocalDate.now(), request.inspectionType(), activeStatuses)) {
            throw new DuplicateResourceException(
                    "An active " + request.inspectionType() + " inspection already exists "
                            + "for project " + request.projectId() + " today.");
        }

        SafetyInspection last = inspectionRepository.findTopByOrderByInspectionIdDesc();
        SafetyInspection inspection = new SafetyInspection();
        inspection.setInspectionId(IdGeneratorUtil.nextInspectionId(last == null ? null : last.getInspectionId()));
        inspection.setProjectId(request.projectId());
        inspection.setOfficerId(officer.userId());
        inspection.setOfficerName(officer.name());
        inspection.setInspectionType(request.inspectionType());
        inspection.setFindings(request.findings());
        inspection.setDate(LocalDate.now());
        inspection.setStatus(InspectionStatus.SCHEDULED);

        return InspectionMapper.toResponse(inspectionRepository.save(inspection));
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionResponse get(String id) {
        UserDto caller = resolveCurrentUser();
        if (!"ACTIVE".equals(caller.status()))
            throw new UnauthorizedOperationException(
                    "Your account is not active. Current status: " + caller.status());

        SafetyInspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found: " + id));
        return InspectionMapper.toResponse(inspection);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InspectionResponse> search(Optional<String> projectId, Optional<InspectionStatus> status,
                                           Optional<LocalDate> dateFrom, Optional<LocalDate> dateTo,
                                           Pageable pageable) {
        UserDto caller = resolveCurrentUser();
        if (!"ACTIVE".equals(caller.status()))
            throw new UnauthorizedOperationException(
                    "Your account is not active. Current status: " + caller.status());

        Specification<SafetyInspection> spec = (root, query, cb) -> cb.conjunction();
        if (projectId.isPresent())
            spec = spec.and((r, q, cb) -> cb.equal(r.get("projectId"), projectId.get()));
        if (status.isPresent())
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status.get()));
        if (dateFrom.isPresent())
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("date"), dateFrom.get()));
        if (dateTo.isPresent())
            spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("date"), dateTo.get()));

        return inspectionRepository.findAll(spec, pageable).map(InspectionMapper::toResponse);
    }

    @Override
    public InspectionResponse updateStatus(String id, InspectionStatus newStatus) {
        UserDto caller = resolveCurrentUser();
        if (!"ACTIVE".equals(caller.status()))
            throw new UnauthorizedOperationException(
                    "Your account is not active. Current status: " + caller.status());

        SafetyInspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found: " + id));

        InspectionStatus oldStatus = inspection.getStatus();
        if (!ALLOWED_TRANSITIONS.get(oldStatus).contains(newStatus))
            throw new InvalidStatusTransitionException(
                    "Cannot transition inspection from " + oldStatus + " to " + newStatus);

        inspection.setStatus(newStatus);
        SafetyInspection saved = inspectionRepository.save(inspection);
        notificationService.notifyInspectionStatusChanged(saved, oldStatus);
        return InspectionMapper.toResponse(saved);
    }

    @Override
    public void delete(String id) {
        UserDto caller = resolveCurrentUser();
        if (!"ACTIVE".equals(caller.status()))
            throw new UnauthorizedOperationException(
                    "Your account is not active. Current status: " + caller.status());

        SafetyInspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found: " + id));
        if (inspection.getStatus() != InspectionStatus.SCHEDULED)
            throw new UnauthorizedOperationException(
                    "Cannot delete an inspection with status: " + inspection.getStatus()
                            + ". Only SCHEDULED inspections can be deleted.");
        inspectionRepository.deleteById(id);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Calls IAM GET /users/profile forwarding the user's own JWT.
     * IAM validates it, queries DB, returns live name + status.
     * Falls back to JWT claims if IAM is unreachable.
     */
    private UserDto resolveCurrentUser() {
        String bearerToken = getAuthorizationHeader();
        String token = bearerToken.substring(7);
        try {
            UserClient.IamProfileResponse response = userClient.getCurrentUserProfile(bearerToken);
            if (response == null || response.data() == null) {
                // Circuit breaker fallback returned null — IAM unreachable, use JWT claims
                log.warn("IAM unavailable (circuit breaker open) — falling back to JWT claims");
                return jwtFallback(token);
            }
            UserClient.UserData d = response.data();
            return new UserDto(d.userId(), d.name(), d.email(), d.role(), d.status());
        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            throw new UnauthorizedOperationException("Invalid or expired token");
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Authenticated user not found in IAM");
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

    private ProjectDto resolveProject(String projectId) {
        try {
            ProjectDto project = projectClient.getProject(projectId);
            if (project == null)
                throw new ResourceNotFoundException("Project service unavailable or project not found: " + projectId);
            return project;
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
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
}