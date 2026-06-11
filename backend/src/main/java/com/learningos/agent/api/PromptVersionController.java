package com.learningos.agent.api;

import com.learningos.agent.application.PromptVersionService;
import com.learningos.agent.dto.PromptVersionResponse;
import com.learningos.agent.dto.PromptVersionUpsertRequest;
import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent/prompt-versions")
public class PromptVersionController {

    private final PromptVersionService promptVersionService;
    private final CurrentUserService currentUserService;

    public PromptVersionController(PromptVersionService promptVersionService, CurrentUserService currentUserService) {
        this.promptVersionService = promptVersionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<PromptVersionResponse> upsert(@Valid @RequestBody PromptVersionUpsertRequest request) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(promptVersionService.upsert(request, hasRole(currentUser, "ADMIN")));
    }

    @GetMapping
    public ApiResponse<List<PromptVersionResponse>> list(@RequestParam(required = false) String code) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(promptVersionService.list(
                code,
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER")
        ));
    }

    @GetMapping("/{code}/{version}")
    public ApiResponse<PromptVersionResponse> get(@PathVariable String code, @PathVariable String version) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(promptVersionService.get(
                code,
                version,
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER")
        ));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
