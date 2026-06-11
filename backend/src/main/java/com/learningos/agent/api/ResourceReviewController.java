package com.learningos.agent.api;

import com.learningos.agent.application.ReviewGovernanceService;
import com.learningos.agent.application.ReviewGovernanceService.ReviewDecisionRequest;
import com.learningos.agent.application.ReviewGovernanceService.ResourceReviewSummary;
import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews/resources")
public class ResourceReviewController {

    private final ReviewGovernanceService reviewGovernanceService;
    private final CurrentUserService currentUserService;

    public ResourceReviewController(
            ReviewGovernanceService reviewGovernanceService,
            CurrentUserService currentUserService
    ) {
        this.reviewGovernanceService = reviewGovernanceService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<ResourceReviewSummary>> list(@RequestParam(required = false) String status) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(reviewGovernanceService.listResourceReviews(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                status
        ));
    }

    @PostMapping("/{reviewId}/decision")
    public ApiResponse<ResourceReviewSummary> decide(
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewDecisionRequestBody request
    ) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(reviewGovernanceService.decide(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                reviewId,
                request.toRequest()
        ));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }

    public record ReviewDecisionRequestBody(
            @NotBlank String decision,
            @NotBlank String summary,
            String reason,
            String citationCheck,
            String safetyCheck,
            String revisionSuggestion
    ) {
        private ReviewDecisionRequest toRequest() {
            return new ReviewDecisionRequest(decision, summary, reason, citationCheck, safetyCheck, revisionSuggestion);
        }
    }
}
