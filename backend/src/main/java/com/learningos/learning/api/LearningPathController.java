package com.learningos.learning.api;

import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import com.learningos.learning.application.LearningWorkflowService;
import com.learningos.learning.dto.CreateLearningPathRequest;
import com.learningos.learning.dto.LearningPathResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning-paths")
public class LearningPathController {

    private final LearningWorkflowService learningWorkflowService;
    private final CurrentUserService currentUserService;

    public LearningPathController(LearningWorkflowService learningWorkflowService, CurrentUserService currentUserService) {
        this.learningWorkflowService = learningWorkflowService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<LearningPathResponse> create(@Valid @RequestBody CreateLearningPathRequest request) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(learningWorkflowService.createPathForUser(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                request
        ));
    }

    @GetMapping("/{pathId}")
    public ApiResponse<LearningPathResponse> get(@PathVariable String pathId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(learningWorkflowService.getPathForUser(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                pathId
        ));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
