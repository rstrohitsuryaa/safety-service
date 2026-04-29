package com.buildsmart.safety.service;

import com.buildsmart.safety.domain.model.AssignedTaskStatus;
import com.buildsmart.safety.web.dto.AssignedTaskDtos.AssignedTaskResponse;
import com.buildsmart.safety.web.dto.AssignedTaskDtos.SyncResult;

import java.util.List;

public interface AssignedTaskService {

    /**
     * Pulls TASK_ASSIGNED notifications from the project-service for the
     * currently authenticated officer, stores any new ones as AssignedTask
     * records, and creates popup SafetyNotifications for each new task.
     */
    SyncResult syncTasksFromPm();

    /** All tasks assigned to the current officer. */
    List<AssignedTaskResponse> getMyTasks();

    /** Tasks for the current officer filtered by status. */
    List<AssignedTaskResponse> getMyTasksByStatus(AssignedTaskStatus status);

    /** All tasks for a given project (current officer only). */
    List<AssignedTaskResponse> getMyTasksForProject(String projectId);
}
