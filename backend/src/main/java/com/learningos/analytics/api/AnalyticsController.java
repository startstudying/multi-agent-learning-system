package com.learningos.analytics.api;

import com.learningos.analytics.application.AnalyticsService;
import com.learningos.analytics.application.AnalyticsService.AnalyticsOverview;
import com.learningos.analytics.application.AnalyticsService.OpsAlertSummary;
import com.learningos.analytics.application.AnalyticsService.PersistedOpsAlertRecord;
import com.learningos.analytics.application.AnalyticsService.StudentAnalyticsSummary;
import com.learningos.analytics.application.AnalyticsService.TeacherClassAnalyticsSummary;
import com.learningos.analytics.application.AnalyticsService.TokenBudgetGovernanceSummary;
import com.learningos.common.api.ApiResponse;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import com.learningos.common.exception.ApiException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;

    public AnalyticsController(AnalyticsService analyticsService, CurrentUserService currentUserService) {
        this.analyticsService = analyticsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/overview")
    public ApiResponse<AnalyticsOverview> overview() {
        UserContext currentUser = currentUserService.currentUser();
        if (!hasRole(currentUser, "ADMIN")) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Analytics overview requires admin access");
        }
        return ApiResponse.success(analyticsService.overview());
    }

    @GetMapping("/students/{learnerId}/summary")
    public ApiResponse<StudentAnalyticsSummary> studentSummary(
            @PathVariable String learnerId,
            @RequestParam(required = false) String courseId
    ) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(analyticsService.studentSummary(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                learnerId,
                courseId
        ));
    }

    @GetMapping("/classes/{courseId}/summary")
    public ApiResponse<TeacherClassAnalyticsSummary> teacherClassSummary(@PathVariable String courseId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(analyticsService.teacherClassSummary(
                courseId,
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER")
        ));
    }

    @GetMapping("/token-budget/governance")
    public ApiResponse<TokenBudgetGovernanceSummary> tokenBudgetGovernance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long tokenBudget,
            @RequestParam(required = false) Double degradeThreshold,
            @RequestParam(required = false) Double manualConfirmationThreshold,
            @RequestParam(required = false) Long highCostTokenThreshold,
            @RequestParam(required = false) Double highCostUsdThreshold,
            @RequestParam(required = false) Long anomalyLatencyMsThreshold
    ) {
        UserContext currentUser = currentUserService.currentUser();
        boolean currentUserAdmin = hasRole(currentUser, "ADMIN");
        if (!currentUserAdmin) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Token budget governance requires admin access");
        }
        return ApiResponse.success(analyticsService.tokenBudgetGovernance(
                currentUserAdmin,
                from,
                to,
                tokenBudget,
                degradeThreshold,
                manualConfirmationThreshold,
                highCostTokenThreshold,
                highCostUsdThreshold,
                anomalyLatencyMsThreshold
        ));
    }

    @GetMapping("/ops/alerts")
    public ApiResponse<OpsAlertSummary> opsAlerts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long slowQueryMs,
            @RequestParam(required = false) Long slowModelMs,
            @RequestParam(required = false) Double noSourceRateThreshold,
            @RequestParam(required = false) Long noSourceMinCount,
            @RequestParam(required = false) Long reviewBacklogHours,
            @RequestParam(required = false) Long reviewBacklogCount
    ) {
        UserContext currentUser = currentUserService.currentUser();
        if (!hasRole(currentUser, "ADMIN")) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Ops alerts require admin access");
        }
        return ApiResponse.success(analyticsService.opsAlerts(
                from,
                to,
                slowQueryMs,
                slowModelMs,
                noSourceRateThreshold,
                noSourceMinCount,
                reviewBacklogHours,
                reviewBacklogCount
        ));
    }

    @GetMapping("/ops/alerts/persisted")
    public ApiResponse<List<PersistedOpsAlertRecord>> persistedOpsAlerts() {
        UserContext currentUser = currentUserService.currentUser();
        if (!hasRole(currentUser, "ADMIN")) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Ops alerts require admin access");
        }
        return ApiResponse.success(analyticsService.recentPersistedAlerts());
    }

    @PostMapping("/ops/alerts/{alertId}/acknowledge")
    public ApiResponse<PersistedOpsAlertRecord> acknowledgeOpsAlert(@PathVariable String alertId) {
        UserContext currentUser = currentUserService.currentUser();
        if (!hasRole(currentUser, "ADMIN")) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Ops alerts require admin access");
        }
        return ApiResponse.success(analyticsService.acknowledgeOpsAlert(alertId, currentUser.userId()));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
