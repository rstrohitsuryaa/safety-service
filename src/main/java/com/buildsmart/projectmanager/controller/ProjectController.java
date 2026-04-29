package com.buildsmart.projectmanager.controller;

import com.buildsmart.projectmanager.dto.*;
import com.buildsmart.projectmanager.entity.MilestoneStatus;
import com.buildsmart.projectmanager.entity.TaskStatus;
import com.buildsmart.projectmanager.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project Management APIs")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new project from template")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse project = projectService.createProjectFromTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    @GetMapping
    @Operation(summary = "Get all projects")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable String projectId) {
        return ResponseEntity.ok(projectService.getProjectById(projectId));
    }

    @GetMapping("/{projectId}/milestones")
    @Operation(summary = "Get project milestones")
    public ResponseEntity<List<MilestoneResponse>> getProjectMilestones(@PathVariable String projectId) {
        return ResponseEntity.ok(projectService.getProjectMilestones(projectId));
    }

    @PatchMapping("/milestones/{milestoneId}/status")
    @Operation(summary = "Update milestone status")
    public ResponseEntity<MilestoneResponse> updateMilestoneStatus(
            @PathVariable String milestoneId,
            @RequestParam MilestoneStatus status) {
        return ResponseEntity.ok(projectService.updateMilestoneStatus(milestoneId, status));
    }

    @GetMapping("/{projectId}/tasks")
    @Operation(summary = "Get project tasks")
    public ResponseEntity<List<TaskResponse>> getProjectTasks(@PathVariable String projectId) {
        return ResponseEntity.ok(projectService.getProjectTasks(projectId));
    }

    @PostMapping("/{projectId}/tasks")
    @Operation(summary = "Create a new task for a project")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable String projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse task = projectService.createTask(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PatchMapping("/tasks/{taskId}/status")
    @Operation(summary = "Update task status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable String taskId,
            @RequestParam TaskStatus status) {
        return ResponseEntity.ok(projectService.updateTaskStatus(taskId, status));
    }
}
