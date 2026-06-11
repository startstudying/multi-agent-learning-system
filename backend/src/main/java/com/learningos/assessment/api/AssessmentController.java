package com.learningos.assessment.api;

import com.learningos.assessment.application.AssessmentService;
import com.learningos.assessment.application.GradingEvaluationService;
import com.learningos.assessment.application.GradingEvaluationSummary;
import com.learningos.assessment.dto.AnswerSubmitRequest;
import com.learningos.assessment.dto.AnswerSubmitResponse;
import com.learningos.assessment.dto.AssessmentPageResponse;
import com.learningos.assessment.dto.AssessmentRecordDetailResponse;
import com.learningos.assessment.dto.AssessmentRecordSummaryResponse;
import com.learningos.assessment.dto.GradingEvaluationRequest;
import com.learningos.assessment.dto.WrongQuestionDetailResponse;
import com.learningos.assessment.dto.WrongQuestionSummaryResponse;
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

@RestController
@RequestMapping("/api/assessment")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final GradingEvaluationService gradingEvaluationService;
    private final CurrentUserService currentUserService;

    public AssessmentController(
            AssessmentService assessmentService,
            GradingEvaluationService gradingEvaluationService,
            CurrentUserService currentUserService
    ) {
        this.assessmentService = assessmentService;
        this.gradingEvaluationService = gradingEvaluationService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/answers")
    public ApiResponse<AnswerSubmitResponse> submit(@Valid @RequestBody AnswerSubmitRequest request) {
        return ApiResponse.success(assessmentService.submitAnswer(currentUserService.currentUserId(), request));
    }

    @GetMapping("/answers")
    public ApiResponse<AssessmentPageResponse<AssessmentRecordSummaryResponse>> listAnswers(
            @RequestParam(required = false) String learnerId,
            @RequestParam(required = false) String courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(assessmentService.listAnswers(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                learnerId,
                courseId,
                page,
                size
        ));
    }

    @GetMapping("/answers/{answerId}")
    public ApiResponse<AssessmentRecordDetailResponse> answerDetail(@PathVariable String answerId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(assessmentService.answerDetail(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                answerId
        ));
    }

    @GetMapping("/wrong-questions")
    public ApiResponse<AssessmentPageResponse<WrongQuestionSummaryResponse>> listWrongQuestions(
            @RequestParam(required = false) String learnerId,
            @RequestParam(required = false) String courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(assessmentService.listWrongQuestions(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                learnerId,
                courseId,
                page,
                size
        ));
    }

    @GetMapping("/wrong-questions/{wrongQuestionId}")
    public ApiResponse<WrongQuestionDetailResponse> wrongQuestionDetail(@PathVariable String wrongQuestionId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(assessmentService.wrongQuestionDetail(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                wrongQuestionId
        ));
    }

    @PostMapping("/grading-evaluations")
    public ApiResponse<GradingEvaluationSummary> evaluateGrading(@RequestBody GradingEvaluationRequest request) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(gradingEvaluationService.evaluate(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                request
        ));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
