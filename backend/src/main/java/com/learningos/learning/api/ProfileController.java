package com.learningos.learning.api;

import com.learningos.common.api.ApiResponse;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.exception.ApiException;
import com.learningos.learning.application.LearningWorkflowService;
import com.learningos.learning.dto.ProfileExtractRequest;
import com.learningos.learning.dto.ProfileExtractResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final LearningWorkflowService learningWorkflowService;
    private final CurrentUserService currentUserService;

    public ProfileController(LearningWorkflowService learningWorkflowService, CurrentUserService currentUserService) {
        this.learningWorkflowService = learningWorkflowService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/dialogue/extract")
    public ApiResponse<ProfileExtractResponse> extract(@Valid @RequestBody ProfileExtractRequest request) {
        if (!currentUserService.currentUserId().equals(request.learnerId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learner profile access denied");
        }
        return ApiResponse.success(learningWorkflowService.extractProfile(request));
    }
}
