package com.learningos.agent.application;

import com.learningos.agent.domain.AgentTask;
import com.learningos.agent.domain.LearningResource;
import com.learningos.agent.domain.ResourceGenerationTask;
import com.learningos.agent.domain.ResourceReview;
import com.learningos.agent.repository.AgentTaskRepository;
import com.learningos.agent.repository.LearningResourceRepository;
import com.learningos.agent.repository.ResourceGenerationTaskRepository;
import com.learningos.agent.repository.ResourceReviewRepository;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({ReviewGovernanceService.class, AgentRunRecorder.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ReviewGovernanceServiceTest {

    @Autowired
    private ReviewGovernanceService reviewGovernanceService;

    @Autowired
    private ResourceReviewRepository resourceReviewRepository;

    @Autowired
    private LearningResourceRepository learningResourceRepository;

    @Autowired
    private ResourceGenerationTaskRepository resourceGenerationTaskRepository;

    @Autowired
    private AgentTaskRepository agentTaskRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void learnerReleaseRemainsGatedUntilEveryResourceAndReviewIsApproved() {
        String taskId = createTaskWithReviews(2);

        assertThat(reviewGovernanceService.canReleaseToLearner(taskId)).isFalse();

        ResourceReview firstReview = resourceReviewRepository.findAll().getFirst();
        reviewGovernanceService.decide("teacher", firstReview.getId(),
                new ReviewGovernanceService.ReviewDecisionRequest("APPROVED", "First resource is accurate."));

        assertThat(reviewGovernanceService.canReleaseToLearner(taskId)).isFalse();

        ResourceReview secondReview = resourceReviewRepository.findAll().stream()
                .filter(review -> !review.getId().equals(firstReview.getId()))
                .findFirst()
                .orElseThrow();
        reviewGovernanceService.decide("teacher", secondReview.getId(),
                new ReviewGovernanceService.ReviewDecisionRequest("APPROVED", "Second resource is accurate."));

        assertThat(reviewGovernanceService.canReleaseToLearner(taskId)).isTrue();
        assertThat(resourceGenerationTaskRepository.findById(taskId))
                .hasValueSatisfying(task -> {
                    assertThat(task.getReviewStatus()).isEqualTo("PUBLISHED");
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_DONE);
                });
        assertThat(agentTaskRepository.findById("agent_task"))
                .hasValueSatisfying(task -> assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_DONE));
    }

    @Test
    void learnerReleaseRequiresAuditedApprovedReviewsNotOnlyApprovedResources() {
        String taskId = createTaskWithReviews(1);
        ResourceReview review = resourceReviewRepository.findAll().getFirst();
        LearningResource resource = learningResourceRepository.findById(review.getResourceId()).orElseThrow();
        ResourceGenerationTask task = resourceGenerationTaskRepository.findById(taskId).orElseThrow();

        review.setStatus(AgentRuntimeConstants.REVIEW_PENDING_CRITIC);
        resource.setReviewStatus("APPROVED");
        task.setReviewStatus("APPROVED");
        resourceReviewRepository.save(review);
        learningResourceRepository.save(resource);
        resourceGenerationTaskRepository.save(task);

        assertThat(reviewGovernanceService.canReleaseToLearner(taskId)).isFalse();
    }

    @Test
    void reviewSummariesExposeExistingAuditLinkage() {
        String taskId = createTaskWithReviews(1);
        ResourceReview review = resourceReviewRepository.findAll().getFirst();

        ReviewGovernanceService.ResourceReviewSummary summary = reviewGovernanceService
                .listResourceReviews("teacher", AgentRuntimeConstants.REVIEW_PENDING_CRITIC)
                .getFirst();

        assertThat(summary.reviewId()).isEqualTo(review.getId());
        assertThat(summary.generationTaskId()).isEqualTo(taskId);
        assertThat(summary.reviewerType()).isEqualTo("CriticAgent");
        assertThat(summary.traceId()).isEqualTo("trace_review_governance");
    }

    @Test
    void learnerReleaseRequiresPublishedTaskAndResourcesNotOnlyApprovedReviews() {
        String taskId = createTaskWithReviews(1);
        ResourceReview review = resourceReviewRepository.findAll().getFirst();
        LearningResource resource = learningResourceRepository.findById(review.getResourceId()).orElseThrow();
        ResourceGenerationTask task = resourceGenerationTaskRepository.findById(taskId).orElseThrow();

        review.setStatus("APPROVED");
        resource.setReviewStatus("APPROVED");
        task.setReviewStatus("APPROVED");
        resourceReviewRepository.save(review);
        learningResourceRepository.save(resource);
        resourceGenerationTaskRepository.save(task);

        assertThat(reviewGovernanceService.canReleaseToLearner(taskId)).isFalse();
    }

    @Test
    void rejectedReviewBlocksLearnerReleaseAndStoresAuditReason() {
        String taskId = createTaskWithReviews(1);
        ResourceReview review = resourceReviewRepository.findAll().getFirst();

        ReviewGovernanceService.ResourceReviewSummary summary = reviewGovernanceService.decide(
                "teacher",
                review.getId(),
                new ReviewGovernanceService.ReviewDecisionRequest(
                        "REJECTED",
                        "Unsupported claim.",
                        "Claim is not in the uploaded course material.",
                        "No citation supports this paragraph.",
                        "Safe wording but ungrounded.",
                        "Regenerate from cited chunks."
                )
        );

        assertThat(summary.status()).isEqualTo("REJECTED");
        assertThat(summary.reason()).isEqualTo("Claim is not in the uploaded course material.");
        assertThat(summary.citationCheck()).isEqualTo("No citation supports this paragraph.");
        assertThat(summary.safetyCheck()).isEqualTo("Safe wording but ungrounded.");
        assertThat(summary.revisionSuggestion()).isEqualTo("Regenerate from cited chunks.");
        assertThat(reviewGovernanceService.canReleaseToLearner(taskId)).isFalse();
        assertThat(resourceGenerationTaskRepository.findById(taskId))
                .hasValueSatisfying(task -> assertThat(task.getReviewStatus()).isEqualTo("REJECTED"));
    }

    @Test
    void deniesReviewListBeforeLoadingDetailsForStudent() {
        assertThatThrownBy(() -> reviewGovernanceService.listResourceReviews("alice", AgentRuntimeConstants.REVIEW_PENDING_CRITIC))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN))
                .hasMessage("Resource review access denied");
    }

    @Test
    void deniesReviewDecisionBeforeLoadingDetailsForStudent() {
        assertThatThrownBy(() -> reviewGovernanceService.decide(
                "alice",
                "missing_review",
                new ReviewGovernanceService.ReviewDecisionRequest("APPROVED", "Student must not approve.")
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN))
                .hasMessage("Resource review access denied");
    }

    private String createTaskWithReviews(int resourceCount) {
        String taskId = "task_review_governance";
        String courseId = "goal";
        Course course = new Course();
        course.setId(courseId);
        course.setTitle("Review Governance Course");
        course.setDescription("Course for review governance tests.");
        course.setTeacherId("teacher");
        course.setStatus("PUBLISHED");
        courseRepository.save(course);

        AgentTask agentTask = new AgentTask();
        agentTask.setId("agent_task");
        agentTask.setOwnerUserId("learner");
        agentTask.setTaskType(AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION);
        agentTask.setStatus(AgentRuntimeConstants.STATUS_WAITING_REVIEW);
        agentTask.setInputJson("{}");
        agentTask.setOutputJson("{}");
        agentTask.setTraceId("trace_review_governance");
        agentTask.setLatencyMs(60L);
        agentTaskRepository.save(agentTask);

        ResourceGenerationTask task = new ResourceGenerationTask();
        task.setId(taskId);
        task.setLearnerId("learner");
        task.setGoalId(courseId);
        task.setPathNodeId("node");
        task.setAgentTaskId("agent_task");
        task.setStatus(AgentRuntimeConstants.STATUS_WAITING_REVIEW);
        task.setReviewStatus(AgentRuntimeConstants.REVIEW_PENDING_CRITIC);
        task.setProgressPercent(60);
        task.setSafetyStatus(AgentRuntimeConstants.SAFETY_NEEDS_REVIEW);
        task.setTraceId("trace_review_governance");
        task.setCreatedBy("learner");
        resourceGenerationTaskRepository.save(task);

        for (int index = 0; index < resourceCount; index++) {
            String resourceId = "res_review_governance_" + index;
            LearningResource resource = new LearningResource();
            resource.setId(resourceId);
            resource.setGenerationTaskId(taskId);
            resource.setLearnerId("learner");
            resource.setResourceType("LECTURE");
            resource.setModality("MARKDOWN");
            resource.setTitle("Resource " + index);
            resource.setReviewStatus(AgentRuntimeConstants.REVIEW_PENDING_CRITIC);
            resource.setCitationSummary("Course source " + index);
            resource.setMarkdownContent("Content " + index);
            resource.setSafetyStatus(AgentRuntimeConstants.SAFETY_NEEDS_REVIEW);
            learningResourceRepository.save(resource);

            ResourceReview review = new ResourceReview();
            review.setId("review_governance_" + index);
            review.setGenerationTaskId(taskId);
            review.setResourceId(resourceId);
            review.setReviewerType("CriticAgent");
            review.setStatus(AgentRuntimeConstants.REVIEW_PENDING_CRITIC);
            review.setSummary("Awaiting review.");
            review.setTraceId("trace_review_governance");
            resourceReviewRepository.save(review);
        }

        return taskId;
    }
}
