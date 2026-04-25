package com.buildsmart.safety.client;

import com.buildsmart.safety.client.dto.ProjectDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "project-manager-service", fallback = ProjectClientFallback.class)
public interface ProjectClient {

    @GetMapping("/api/v1/projects/{projectId}")
    ProjectDto getProject(@PathVariable String projectId);
}
