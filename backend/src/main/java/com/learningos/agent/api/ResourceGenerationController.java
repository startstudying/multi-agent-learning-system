package com.learningos.agent.api;

import com.learningos.agent.application.ResourceGenerationService;
import com.learningos.agent.dto.LearnerResourceListResponse;
import com.learningos.agent.dto.ResourceGenerationRequest;
import com.learningos.agent.dto.ResourceGenerationResponse;
import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResourceGenerationController {

    private final ResourceGenerationService resourceGenerationService;
    private final CurrentUserService currentUserService;

    public ResourceGenerationController(ResourceGenerationService resourceGenerationService, CurrentUserService currentUserService) {
        this.resourceGenerationService = resourceGenerationService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/api/resources/generation-tasks")
    public ApiResponse<ResourceGenerationResponse> create(@Valid @RequestBody ResourceGenerationRequest request) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(resourceGenerationService.createTask(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                request
        ));
    }

    @GetMapping("/api/resources/generation-tasks/{taskId}")
    public ApiResponse<ResourceGenerationResponse> get(@PathVariable String taskId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(resourceGenerationService.getTask(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                taskId
        ));
    }

    @GetMapping("/api/agent/resource-generation/tasks/{taskId}/learner-resources")
    public ApiResponse<LearnerResourceListResponse> learnerResources(@PathVariable String taskId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(resourceGenerationService.getLearnerResources(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                taskId
        ));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
