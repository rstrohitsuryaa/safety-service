package com.buildsmart.safety.client;

import com.buildsmart.safety.client.dto.ProjectDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;


@FeignClient(name = "project-service", contextId = "projectClient", fallback = ProjectClientFallback.class)
public interface ProjectClient {

    @GetMapping("/api/projects/{projectId}")
    ProjectDto getProject(@PathVariable String projectId,
                          @RequestHeader("Authorization") String bearerToken);
}
