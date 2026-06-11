package com.learningos.agent.api;

import com.learningos.agent.application.AgentTraceGovernanceService;
import com.learningos.agent.dto.AgentTraceSearchResponse;
import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/agent/traces")
public class AgentTraceGovernanceController {

    private final AgentTraceGovernanceService governanceService;
    private final CurrentUserService currentUserService;

    public AgentTraceGovernanceController(
            AgentTraceGovernanceService governanceService,
            CurrentUserService currentUserService
    ) {
        this.governanceService = governanceService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<AgentTraceSearchResponse> search(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String agentType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String failureReason
    ) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(governanceService.search(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                userId,
                agentType,
                status,
                from,
                to,
                failureReason
        ));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
