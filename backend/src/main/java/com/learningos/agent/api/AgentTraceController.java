package com.learningos.agent.api;

import com.learningos.agent.application.AgentTraceGovernanceService;
import com.learningos.agent.application.ResourceGenerationService;
import com.learningos.agent.dto.AgentTaskCancelRequest;
import com.learningos.agent.dto.AgentTraceResponse;
import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/tasks")
public class AgentTraceController {

    private final ResourceGenerationService resourceGenerationService;
    private final AgentTraceGovernanceService governanceService;
    private final CurrentUserService currentUserService;

    public AgentTraceController(
            ResourceGenerationService resourceGenerationService,
            AgentTraceGovernanceService governanceService,
            CurrentUserService currentUserService
    ) {
        this.resourceGenerationService = resourceGenerationService;
        this.governanceService = governanceService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{taskId}/trace")
    public ApiResponse<AgentTraceResponse> trace(@PathVariable String taskId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(governanceService.getTrace(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                taskId
        ));
    }

    @PostMapping("/{taskId}/cancel")
    public ApiResponse<AgentTraceResponse> cancel(
            @PathVariable String taskId,
            @Valid @RequestBody AgentTaskCancelRequest request
    ) {
        return ApiResponse.success(resourceGenerationService.cancelAgentTask(
                currentUserService.currentUserId(),
                taskId,
                request.reason()
        ));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
