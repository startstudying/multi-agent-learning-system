package com.learningos.orchestrator.api;

import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import com.learningos.orchestrator.application.OrchestratorWorkflowService;
import com.learningos.orchestrator.dto.CreateWorkflowRequest;
import com.learningos.orchestrator.dto.OrchestratorWorkflowResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orchestrator/workflows")
public class OrchestratorWorkflowController {

    private final OrchestratorWorkflowService orchestratorWorkflowService;
    private final CurrentUserService currentUserService;

    public OrchestratorWorkflowController(
            OrchestratorWorkflowService orchestratorWorkflowService,
            CurrentUserService currentUserService
    ) {
        this.orchestratorWorkflowService = orchestratorWorkflowService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<OrchestratorWorkflowResponse> create(@Valid @RequestBody CreateWorkflowRequest request) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(orchestratorWorkflowService.createWorkflow(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                request
        ));
    }

    @GetMapping("/{workflowId}")
    public ApiResponse<OrchestratorWorkflowResponse> get(@PathVariable String workflowId) {
        return ApiResponse.success(orchestratorWorkflowService.getWorkflow(currentUserService.currentUserId(), workflowId));
    }

    @PostMapping("/{workflowId}/retry")
    public ApiResponse<OrchestratorWorkflowResponse> retry(@PathVariable String workflowId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(orchestratorWorkflowService.retryWorkflow(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                workflowId
        ));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
