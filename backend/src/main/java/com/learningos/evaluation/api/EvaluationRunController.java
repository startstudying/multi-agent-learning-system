package com.learningos.evaluation.api;

import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import com.learningos.evaluation.application.EvaluationRunService;
import com.learningos.evaluation.dto.EvaluationRunRecordRequest;
import com.learningos.evaluation.dto.EvaluationRunResponse;
import com.learningos.evaluation.dto.PromptVersionQualityComparisonResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/evaluation-runs")
public class EvaluationRunController {

    private final EvaluationRunService evaluationRunService;
    private final CurrentUserService currentUserService;

    public EvaluationRunController(EvaluationRunService evaluationRunService, CurrentUserService currentUserService) {
        this.evaluationRunService = evaluationRunService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<EvaluationRunResponse> record(@RequestBody EvaluationRunRecordRequest request) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(evaluationRunService.record(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                request
        ));
    }

    @GetMapping("/comparison")
    public ApiResponse<PromptVersionQualityComparisonResponse> compare(
            @RequestParam String evaluationSetId,
            @RequestParam String promptCode,
            @RequestParam String promptVersions,
            @RequestParam(required = false) String baselinePromptVersion
    ) {
        List<String> versions = Arrays.stream(promptVersions.split(","))
                .map(String::trim)
                .filter(version -> !version.isBlank())
                .toList();
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(evaluationRunService.compare(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                evaluationSetId,
                promptCode,
                versions,
                baselinePromptVersion
        ));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
