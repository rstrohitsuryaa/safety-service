package com.buildsmart.safety.web.controller;

import com.buildsmart.safety.domain.model.AssignedTaskStatus;
import com.buildsmart.safety.service.AssignedTaskService;
import com.buildsmart.safety.web.dto.AssignedTaskDtos.AssignedTaskResponse;
import com.buildsmart.safety.web.dto.AssignedTaskDtos.SyncResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety/tasks")
@RequiredArgsConstructor
@Tag(name = "Assigned Tasks", description = "Tasks assigned by Project Manager to Safety Officers")
public class AssignedTaskController {

    private final AssignedTaskService assignedTaskService;

    @PostMapping("/sync")
    @Operation(summary = "Pull new TASK_ASSIGNED notifications from project-service and store them locally. "
            + "Call this on portal load to show popup notifications. (SAFETY_OFFICER / ADMIN only)")
    @PreAuthorize("hasAnyRole('SAFETY_OFFICER','ADMIN')")
    public ResponseEntity<SyncResult> sync() {
        return ResponseEntity.ok(assignedTaskService.syncTasksFromPm());
    }

    @GetMapping
    @Operation(summary = "Get all tasks assigned to the current officer")
    @PreAuthorize("hasAnyRole('SAFETY_OFFICER','ADMIN')")
    public ResponseEntity<List<AssignedTaskResponse>> getMyTasks(
            @RequestParam(required = false) AssignedTaskStatus status) {
        if (status != null) {
            return ResponseEntity.ok(assignedTaskService.getMyTasksByStatus(status));
        }
        return ResponseEntity.ok(assignedTaskService.getMyTasks());
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get tasks assigned to the current officer for a specific project")
    @PreAuthorize("hasAnyRole('SAFETY_OFFICER','ADMIN')")
    public ResponseEntity<List<AssignedTaskResponse>> getMyTasksForProject(
            @PathVariable String projectId) {
        return ResponseEntity.ok(assignedTaskService.getMyTasksForProject(projectId));
    }
}
