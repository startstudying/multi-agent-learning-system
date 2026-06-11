package com.learningos.agent.application;

import com.learningos.agent.domain.LearningResource;
import com.learningos.agent.domain.ResourceGenerationTask;
import com.learningos.agent.domain.ResourceReview;
import com.learningos.agent.repository.LearningResourceRepository;
import com.learningos.agent.repository.ResourceGenerationTaskRepository;
import com.learningos.agent.repository.ResourceReviewRepository;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ReviewGovernanceService {

    private static final String APPROVED = "APPROVED";
    private static final String REVISION_REQUESTED = "REVISION_REQUESTED";
    private static final String REJECTED = "REJECTED";
    private static final String PUBLISHED = AgentRuntimeConstants.REVIEW_PUBLISHED;
    private static final String PENDING_CRITIC = AgentRuntimeConstants.REVIEW_PENDING_CRITIC;
    private static final String REVIEW_ACCESS_DENIED = "Resource review access denied";

    private final ResourceReviewRepository resourceReviewRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final ResourceGenerationTaskRepository resourceGenerationTaskRepository;
    private final AgentRunRecorder agentRunRecorder;
    private final CourseRepository courseRepository;

    public ReviewGovernanceService(
            ResourceReviewRepository resourceReviewRepository,
            LearningResourceRepository learningResourceRepository,
            ResourceGenerationTaskRepository resourceGenerationTaskRepository,
            AgentRunRecorder agentRunRecorder,
            CourseRepository courseRepository
    ) {
        this.resourceReviewRepository = resourceReviewRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.resourceGenerationTaskRepository = resourceGenerationTaskRepository;
        this.agentRunRecorder = agentRunRecorder;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<ResourceReviewSummary> listResourceReviews(String reviewerUserId, String status) {
        return listResourceReviews(reviewerUserId, isAdmin(reviewerUserId), isTeacher(reviewerUserId), status);
    }

    @Transactional(readOnly = true)
    public List<ResourceReviewSummary> listResourceReviews(
            String reviewerUserId,
            boolean reviewerAdmin,
            boolean reviewerTeacher,
            String status
    ) {
        assertReviewerAccess(reviewerAdmin, reviewerTeacher);
        List<ResourceReview> reviews = status == null || status.isBlank()
                ? resourceReviewRepository.findAll()
                : resourceReviewRepository.findByStatusOrderByCreatedAtAsc(status);
        return reviews.stream()
                .filter(review -> canAccessReview(reviewerUserId, reviewerAdmin, reviewerTeacher, review))
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public ResourceReviewSummary decide(String reviewerUserId, String reviewId, ReviewDecisionRequest request) {
        return decide(reviewerUserId, isAdmin(reviewerUserId), isTeacher(reviewerUserId), reviewId, request);
    }

    @Transactional
    public ResourceReviewSummary decide(
            String reviewerUserId,
            boolean reviewerAdmin,
            boolean reviewerTeacher,
            String reviewId,
            ReviewDecisionRequest request
    ) {
        assertReviewerAccess(reviewerAdmin, reviewerTeacher);
        validateDecision(request.decision());

        ResourceReview review = resourceReviewRepository.findById(reviewId)
                .orElseThrow(() -> reviewerAdmin
                        ? new ApiException(ErrorCode.NOT_FOUND, "Resource review not found")
                        : new ApiException(ErrorCode.FORBIDDEN, REVIEW_ACCESS_DENIED));
        ResourceGenerationTask task = resourceGenerationTaskRepository.findById(review.getGenerationTaskId())
                .orElseThrow(() -> reviewerAdmin
                        ? new ApiException(ErrorCode.NOT_FOUND, "Resource generation task not found")
                        : new ApiException(ErrorCode.FORBIDDEN, REVIEW_ACCESS_DENIED));
        assertCanReviewTask(reviewerUserId, reviewerAdmin, reviewerTeacher, task);
        LearningResource resource = learningResourceRepository.findById(review.getResourceId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Learning resource not found"));

        String decision = normalizeDecision(request.decision());
        if (APPROVED.equals(decision) && hasNoSourceFinding(review, resource, request)) {
            throw new ApiException(ErrorCode.CONFLICT, "NO_SOURCE resource cannot be approved without citations");
        }

        review.setStatus(decision);
        review.setSummary(request.summary());
        review.setReason(request.reason());
        review.setCitationCheck(blankToDefault(request.citationCheck(), review.getCitationCheck()));
        review.setSafetyCheck(request.safetyCheck());
        review.setRevisionSuggestion(request.revisionSuggestion());
        resource.setReviewStatus(review.getStatus());
        resourceReviewRepository.save(review);
        learningResourceRepository.save(resource);

        if (hasReviewWithStatus(task.getId(), REJECTED)) {
            task.setReviewStatus(REJECTED);
        } else if (hasReviewWithStatus(task.getId(), REVISION_REQUESTED)) {
            task.setReviewStatus(REVISION_REQUESTED);
        } else if (isTaskFullyApproved(task.getId())) {
            publishApprovedResources(task.getId());
            task.setReviewStatus(PUBLISHED);
            task.setStatus(AgentRuntimeConstants.STATUS_DONE);
            agentRunRecorder.transitionTask(
                    task.getAgentTaskId(),
                    AgentRuntimeConstants.STATUS_DONE,
                    "All generated resources are approved and ready for learner release.",
                    0L
            );
        } else {
            task.setReviewStatus(PENDING_CRITIC);
        }

        resourceGenerationTaskRepository.save(task);
        return toSummary(resourceReviewRepository.findById(review.getId()).orElse(review));
    }

    private void assertReviewerAccess(boolean reviewerAdmin, boolean reviewerTeacher) {
        if (!reviewerAdmin && !reviewerTeacher) {
            throw new ApiException(ErrorCode.FORBIDDEN, REVIEW_ACCESS_DENIED);
        }
    }

    private boolean canAccessReview(
            String reviewerUserId,
            boolean reviewerAdmin,
            boolean reviewerTeacher,
            ResourceReview review
    ) {
        if (reviewerAdmin) {
            return true;
        }
        return resourceGenerationTaskRepository.findById(review.getGenerationTaskId())
                .map(task -> canReviewTask(reviewerUserId, reviewerAdmin, reviewerTeacher, task))
                .orElse(false);
    }

    private void assertCanReviewTask(
            String reviewerUserId,
            boolean reviewerAdmin,
            boolean reviewerTeacher,
            ResourceGenerationTask task
    ) {
        if (!canReviewTask(reviewerUserId, reviewerAdmin, reviewerTeacher, task)) {
            throw new ApiException(ErrorCode.FORBIDDEN, REVIEW_ACCESS_DENIED);
        }
    }

    private boolean canReviewTask(
            String reviewerUserId,
            boolean reviewerAdmin,
            boolean reviewerTeacher,
            ResourceGenerationTask task
    ) {
        if (reviewerAdmin) {
            return true;
        }
        if (!reviewerTeacher) {
            return false;
        }
        if (!hasText(reviewerUserId) || task == null || !hasText(task.getGoalId())) {
            return false;
        }
        return courseRepository.findById(task.getGoalId())
                .map(Course::getTeacherId)
                .filter(this::hasText)
                .filter(reviewerUserId::equals)
                .isPresent();
    }

    private boolean isAdmin(String reviewerUserId) {
        return "admin".equals(reviewerUserId);
    }

    private boolean isTeacher(String reviewerUserId) {
        return hasText(reviewerUserId) && courseRepository.existsByTeacherId(reviewerUserId);
    }

    @Transactional(readOnly = true)
    public boolean canReleaseToLearner(String generationTaskId) {
        ResourceGenerationTask task = resourceGenerationTaskRepository.findById(generationTaskId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Resource generation task not found"));
        long totalResources = learningResourceRepository.countByGenerationTaskId(generationTaskId);
        long publishedResources = learningResourceRepository.countByGenerationTaskIdAndReviewStatus(generationTaskId, PUBLISHED);
        long totalReviews = resourceReviewRepository.countByGenerationTaskId(generationTaskId);
        long approvedReviews = resourceReviewRepository.countByGenerationTaskIdAndStatus(generationTaskId, APPROVED);
        List<LearningResource> resources = learningResourceRepository.findByGenerationTaskIdOrderByCreatedAtAsc(generationTaskId);
        List<ResourceReview> reviews = resourceReviewRepository.findByGenerationTaskIdOrderByCreatedAtAsc(generationTaskId);
        return PUBLISHED.equals(task.getReviewStatus())
                && totalResources > 0
                && totalResources == publishedResources
                && totalReviews == totalResources
                && totalReviews == approvedReviews
                && resources.stream().noneMatch(this::hasNoSourceFinding)
                && reviews.stream().noneMatch(this::hasNoSourceFinding);
    }

    private boolean isTaskFullyApproved(String taskId) {
        long totalResources = learningResourceRepository.countByGenerationTaskId(taskId);
        long totalReviews = resourceReviewRepository.countByGenerationTaskId(taskId);
        long approvedReviews = resourceReviewRepository.countByGenerationTaskIdAndStatus(taskId, APPROVED);
        List<LearningResource> resources = learningResourceRepository.findByGenerationTaskIdOrderByCreatedAtAsc(taskId);
        List<ResourceReview> reviews = resourceReviewRepository.findByGenerationTaskIdOrderByCreatedAtAsc(taskId);
        return totalResources > 0
                && totalReviews == totalResources
                && totalReviews == approvedReviews
                && resources.stream().noneMatch(this::hasNoSourceFinding)
                && reviews.stream().noneMatch(this::hasNoSourceFinding);
    }

    private boolean hasReviewWithStatus(String taskId, String status) {
        return resourceReviewRepository.countByGenerationTaskIdAndStatus(taskId, status) > 0;
    }

    private void publishApprovedResources(String taskId) {
        List<LearningResource> resources = learningResourceRepository.findByGenerationTaskIdOrderByCreatedAtAsc(taskId);
        resources.forEach(resource -> resource.setReviewStatus(PUBLISHED));
        learningResourceRepository.saveAll(resources);
    }

    private boolean hasNoSourceFinding(ResourceReview review, LearningResource resource, ReviewDecisionRequest request) {
        return hasNoSourceFinding(review)
                || hasNoSourceFinding(resource)
                || containsNoSource(request.citationCheck());
    }

    private boolean hasNoSourceFinding(ResourceReview review) {
        return containsNoSource(review.getCitationCheck());
    }

    private boolean hasNoSourceFinding(LearningResource resource) {
        return containsNoSource(resource.getCitationSummary());
    }

    private boolean containsNoSource(String value) {
        return value != null && value.toUpperCase(Locale.ROOT).contains("NO_SOURCE");
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void validateDecision(String decision) {
        if (decision == null || decision.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Decision is required");
        }
        normalizeDecision(decision);
    }

    private String normalizeDecision(String decision) {
        String normalized = decision.trim().toUpperCase(Locale.ROOT);
        if (!APPROVED.equals(normalized) && !REVISION_REQUESTED.equals(normalized) && !REJECTED.equals(normalized)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Decision must be APPROVED, REVISION_REQUESTED, or REJECTED");
        }
        return normalized;
    }

    private ResourceReviewSummary toSummary(ResourceReview review) {
        LearningResource resource = learningResourceRepository.findById(review.getResourceId()).orElse(null);
        return new ResourceReviewSummary(
                review.getId(),
                review.getResourceId(),
                review.getGenerationTaskId(),
                review.getStatus(),
                review.getSummary(),
                resource == null ? null : resource.getTitle(),
                resource == null ? null : resource.getResourceType(),
                resource == null ? null : resource.getReviewStatus(),
                review.getReviewerType(),
                review.getTraceId(),
                review.getReason(),
                review.getCitationCheck(),
                review.getSafetyCheck(),
                review.getRevisionSuggestion()
        );
    }

    public record ReviewDecisionRequest(
            String decision,
            String summary,
            String reason,
            String citationCheck,
            String safetyCheck,
            String revisionSuggestion
    ) {
        public ReviewDecisionRequest(String decision, String summary) {
            this(decision, summary, null, null, null, null);
        }
    }

    public record ResourceReviewSummary(
            String reviewId,
            String resourceId,
            String generationTaskId,
            String status,
            String summary,
            String resourceTitle,
            String resourceType,
            String resourceReviewStatus,
            String reviewerType,
            String traceId,
            String reason,
            String citationCheck,
            String safetyCheck,
            String revisionSuggestion
    ) {
    }
}
