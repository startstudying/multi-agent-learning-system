package com.learningos.evaluation.api;

import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import com.learningos.evaluation.application.EvaluationSetService;
import com.learningos.evaluation.dto.EvaluationSetResponse;
import com.learningos.evaluation.dto.EvaluationSetUpsertRequest;
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
@RequestMapping("/api/evaluation-sets")
public class EvaluationSetController {

    private final EvaluationSetService evaluationSetService;
    private final CurrentUserService currentUserService;

    public EvaluationSetController(EvaluationSetService evaluationSetService, CurrentUserService currentUserService) {
        this.evaluationSetService = evaluationSetService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<EvaluationSetResponse> upsert(@Valid @RequestBody EvaluationSetUpsertRequest request) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(evaluationSetService.upsert(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                request
        ));
    }

    @GetMapping
    public ApiResponse<List<EvaluationSetResponse>> list(@RequestParam(required = false) String type) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(evaluationSetService.list(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                type
        ));
    }

    @GetMapping("/{setId}")
    public ApiResponse<EvaluationSetResponse> get(@PathVariable String setId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(evaluationSetService.get(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                setId
        ));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
