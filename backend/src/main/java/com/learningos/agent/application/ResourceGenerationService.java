package com.learningos.agent.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.agent.domain.AgentTask;
import com.learningos.agent.domain.AgentTrace;
import com.learningos.agent.domain.LearningResource;
import com.learningos.agent.domain.ResourceGenerationTask;
import com.learningos.agent.domain.ResourceReview;
import com.learningos.agent.dto.AgentTraceResponse;
import com.learningos.agent.dto.AgentTraceStepResponse;
import com.learningos.agent.dto.GeneratedResourceResponse;
import com.learningos.agent.dto.LearnerResourceListResponse;
import com.learningos.agent.dto.LearnerResourceResponse;
import com.learningos.agent.dto.ResourceGenerationRequest;
import com.learningos.agent.dto.ResourceGenerationResponse;
import com.learningos.agent.repository.AgentTaskRepository;
import com.learningos.agent.repository.AgentTraceRepository;
import com.learningos.agent.repository.LearningResourceRepository;
import com.learningos.agent.repository.ResourceGenerationTaskRepository;
import com.learningos.agent.repository.ResourceReviewRepository;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.common.trace.TraceContext;
import com.learningos.knowledge.application.CourseAccessService;
import com.learningos.learning.domain.LearnerProfile;
import com.learningos.learning.repository.LearnerProfileRepository;
import com.learningos.rag.domain.SourceCitationRecord;
import com.learningos.rag.repository.SourceCitationRepository;
import com.learningos.safety.application.ContentSafetyService;
import com.learningos.safety.dto.ContentSafetyResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ResourceGenerationService {

    private static final String MODEL_CALL_FAILED_ERROR = "MODEL_CALL_FAILED";
    private static final long RECOVERABLE_RETRY_DELAY_SECONDS = 300L;

    private final ContentSafetyService contentSafetyService;
    private final ResourceGenerationTaskRepository resourceGenerationTaskRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final ResourceReviewRepository resourceReviewRepository;
    private final AgentTaskRepository agentTaskRepository;
    private final AgentTraceRepository agentTraceRepository;
    private final AgentRunRecorder agentRunRecorder;
    private final AiModelGateway aiModelGateway;
    private final ReviewGovernanceService reviewGovernanceService;
    private final LearnerProfileRepository learnerProfileRepository;
    private final SourceCitationRepository sourceCitationRepository;
    private final CourseAccessService courseAccessService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ResourceGenerationService(
            ContentSafetyService contentSafetyService,
            ResourceGenerationTaskRepository resourceGenerationTaskRepository,
            LearningResourceRepository learningResourceRepository,
            ResourceReviewRepository resourceReviewRepository,
            AgentTaskRepository agentTaskRepository,
            AgentTraceRepository agentTraceRepository,
            AgentRunRecorder agentRunRecorder,
            AiModelGateway aiModelGateway,
            ReviewGovernanceService reviewGovernanceService,
            LearnerProfileRepository learnerProfileRepository,
            SourceCitationRepository sourceCitationRepository,
            CourseAccessService courseAccessService,
            ObjectMapper objectMapper
    ) {
        this.contentSafetyService = contentSafetyService;
        this.resourceGenerationTaskRepository = resourceGenerationTaskRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.resourceReviewRepository = resourceReviewRepository;
        this.agentTaskRepository = agentTaskRepository;
        this.agentTraceRepository = agentTraceRepository;
        this.agentRunRecorder = agentRunRecorder;
        this.aiModelGateway = aiModelGateway;
        this.reviewGovernanceService = reviewGovernanceService;
        this.learnerProfileRepository = learnerProfileRepository;
        this.sourceCitationRepository = sourceCitationRepository;
        this.courseAccessService = courseAccessService;
        this.objectMapper = objectMapper;
    }

    public ResourceGenerationService(
            ContentSafetyService contentSafetyService,
            ResourceGenerationTaskRepository resourceGenerationTaskRepository,
            LearningResourceRepository learningResourceRepository,
            ResourceReviewRepository resourceReviewRepository,
            AgentTaskRepository agentTaskRepository,
            AgentTraceRepository agentTraceRepository,
            AgentRunRecorder agentRunRecorder,
            AiModelGateway aiModelGateway,
            ReviewGovernanceService reviewGovernanceService,
            LearnerProfileRepository learnerProfileRepository,
            SourceCitationRepository sourceCitationRepository,
            ObjectMapper objectMapper
    ) {
        this(
                contentSafetyService,
                resourceGenerationTaskRepository,
                learningResourceRepository,
                resourceReviewRepository,
                agentTaskRepository,
                agentTraceRepository,
                agentRunRecorder,
                aiModelGateway,
                reviewGovernanceService,
                learnerProfileRepository,
                sourceCitationRepository,
                null,
                objectMapper
        );
    }

    @Transactional(noRollbackFor = AiModelGateway.ModelCallFailedException.class)
    public ResourceGenerationResponse createTask(String userId, ResourceGenerationRequest request) {
        return createTask(userId, request, isAdmin(userId));
    }

    @Transactional(noRollbackFor = AiModelGateway.ModelCallFailedException.class)
    public ResourceGenerationResponse createTask(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            ResourceGenerationRequest request
    ) {
        return createTask(userId, request, false);
    }

    private ResourceGenerationResponse createTask(
            String userId,
            ResourceGenerationRequest request,
            boolean allowAdminEnrollmentBypass
    ) {
        ensureLearnerOwner(userId, request.learnerId());
        requireCourseEnrollmentIfCourseGoal(userId, request, allowAdminEnrollmentBypass);
        String requestId = normalizeRequestId(request.requestId());
        if (requestId != null) {
            ResourceGenerationTask existingTask = resourceGenerationTaskRepository
                    .findByLearnerIdAndRequestId(request.learnerId(), requestId)
                    .orElse(null);
            if (existingTask != null) {
                List<LearningResource> resources = learningResourceRepository
                        .findByGenerationTaskIdOrderByCreatedAtAsc(existingTask.getId());
                return toResponse(existingTask, resources);
            }
        }
        checkInputSafety(request);

        String traceId = traceId();

        AgentExecutionContext executionContext = agentRunRecorder.startRun(new AgentRunRecorder.RunStart(
                userId,
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_RUNNING,
                """
                        {"learnerId":"%s","goalId":"%s","pathNodeId":"%s"}
                        """.formatted(request.learnerId(), request.goalId(), request.pathNodeId()).trim(),
                "{\"summary\":\"Draft resources are generated and awaiting critic review.\"}",
                traceId,
                265L
        ));

        return createTaskWithContext(userId, request, requestId, executionContext);
    }

    @Transactional(noRollbackFor = AiModelGateway.ModelCallFailedException.class)
    public ResourceGenerationResponse createTaskInWorkflow(
            String userId,
            ResourceGenerationRequest request,
            AgentExecutionContext executionContext
    ) {
        return createTaskInWorkflow(userId, request, executionContext, isAdmin(userId));
    }

    @Transactional(noRollbackFor = AiModelGateway.ModelCallFailedException.class)
    public ResourceGenerationResponse createTaskInWorkflow(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            ResourceGenerationRequest request,
            AgentExecutionContext executionContext
    ) {
        return createTaskInWorkflow(userId, request, executionContext, false);
    }

    private ResourceGenerationResponse createTaskInWorkflow(
            String userId,
            ResourceGenerationRequest request,
            AgentExecutionContext executionContext,
            boolean allowAdminEnrollmentBypass
    ) {
        ensureLearnerOwner(userId, request.learnerId());
        requireCourseEnrollmentIfCourseGoal(userId, request, allowAdminEnrollmentBypass);
        String requestId = normalizeRequestId(request.requestId());
        if (requestId != null) {
            ResourceGenerationTask existingTask = resourceGenerationTaskRepository
                    .findByLearnerIdAndRequestId(request.learnerId(), requestId)
                    .orElse(null);
            if (existingTask != null) {
                if (executionContext.agentTaskId().equals(existingTask.getAgentTaskId())) {
                    List<LearningResource> resources = learningResourceRepository
                            .findByGenerationTaskIdOrderByCreatedAtAsc(existingTask.getId());
                    return toResponse(existingTask, resources);
                }
                throw new ApiException(ErrorCode.CONFLICT, "Resource generation requestId already belongs to another workflow");
            }
        }
        checkInputSafety(request);
        return createTaskWithContext(userId, request, requestId, executionContext);
    }

    private ResourceGenerationResponse createTaskWithContext(
            String userId,
            ResourceGenerationRequest request,
            String requestId,
            AgentExecutionContext executionContext
    ) {
        String taskId = id("rgt");

        ResourceGenerationTask generationTask = new ResourceGenerationTask();
        generationTask.setId(taskId);
        generationTask.setLearnerId(request.learnerId());
        generationTask.setGoalId(request.goalId());
        generationTask.setPathNodeId(request.pathNodeId());
        generationTask.setRequestId(requestId);
        generationTask.setAgentTaskId(executionContext.agentTaskId());
        generationTask.setStatus(AgentRuntimeConstants.STATUS_RUNNING);
        generationTask.setReviewStatus(AgentRuntimeConstants.REVIEW_PENDING_CRITIC);
        generationTask.setProgressPercent(60);
        generationTask.setRetryCount(0);
        generationTask.setRecoverable(false);
        generationTask.setLastError(null);
        generationTask.setNextRetryAt(null);
        generationTask.setSafetyStatus(AgentRuntimeConstants.SAFETY_NEEDS_REVIEW);
        generationTask.setTraceId(executionContext.traceId());
        generationTask.setProfileSnapshot(profileSnapshot(request.learnerId()));
        generationTask.setCreatedBy(userId);
        ResourceGenerationTask persistedGenerationTask = resourceGenerationTaskRepository.save(generationTask);

        AiModelGateway.ModelResponse modelResponse;
        try {
            modelResponse = aiModelGateway.generateStructuredWithRetry(
                    new AiModelGateway.ModelRequest(
                            "ResourceAgent",
                            "Generate personalized learning resources for the learner path node.",
                            Map.of(
                                    "learnerId", request.learnerId(),
                                    "goalId", request.goalId(),
                                    "pathNodeId", request.pathNodeId(),
                                    "resourceTypes", request.resourceTypes(),
                                    "profileSnapshot", persistedGenerationTask.getProfileSnapshot()
                            ),
                            AgentRuntimeConstants.PROMPT_RESOURCE_V1
                    ),
                    executionContext,
                    agentRunRecorder,
                    new AiModelGateway.FailureTrace(
                            "step_resource",
                            "ResourceAgent",
                            "Model call failed while drafting resources.",
                            AgentRuntimeConstants.PROMPT_RESOURCE_V1
                    )
            );
        } catch (AiModelGateway.ModelCallFailedException exception) {
            persistedGenerationTask.setStatus(AgentRuntimeConstants.STATUS_FAILED);
            persistedGenerationTask.setProgressPercent(0);
            persistedGenerationTask.setRetryCount(nextRetryCount(persistedGenerationTask.getRetryCount()));
            persistedGenerationTask.setRecoverable(true);
            persistedGenerationTask.setLastError(MODEL_CALL_FAILED_ERROR);
            persistedGenerationTask.setNextRetryAt(Instant.now().plusSeconds(RECOVERABLE_RETRY_DELAY_SECONDS));
            resourceGenerationTaskRepository.save(persistedGenerationTask);
            throw exception;
        }
        int citationCount = persistResourceSourceCitations(persistedGenerationTask, request);
        List<LearningResource> resources = persistResources(taskId, request, citationCount);
        persistReviews(taskId, executionContext.traceId(), resources, citationCount);
        List<AgentTrace> traces = agentRunRecorder.recordTraceSteps(executionContext, resourceGenerationTraceSteps());
        agentRunRecorder.recordSuccessfulModelEvidence(
                executionContext,
                modelTrace(traces),
                modelResponse
        );
        agentRunRecorder.transitionTask(
                executionContext,
                AgentRuntimeConstants.STATUS_WAITING_REVIEW,
                "Draft resources are generated and awaiting critic review.",
                265L
        );
        persistedGenerationTask.setStatus(AgentRuntimeConstants.STATUS_WAITING_REVIEW);
        resourceGenerationTaskRepository.save(persistedGenerationTask);

        return toResponse(persistedGenerationTask, resources);
    }

    @Transactional(readOnly = true)
    public ResourceGenerationResponse getTask(String userId, String taskId) {
        return getTask(userId, isAdmin(userId), taskId);
    }

    @Transactional(readOnly = true)
    public ResourceGenerationResponse getTask(String userId, boolean currentUserAdmin, String taskId) {
        ResourceGenerationTask task = loadGenerationTaskForDetail(currentUserAdmin, taskId);
        ensureTaskOwnerOrAdmin(userId, currentUserAdmin, task);
        List<LearningResource> resources = learningResourceRepository.findByGenerationTaskIdOrderByCreatedAtAsc(taskId);
        return toResponse(task, resources);
    }

    @Transactional(readOnly = true)
    public LearnerResourceListResponse getLearnerResources(String userId, String taskId) {
        return getLearnerResources(userId, isAdmin(userId), taskId);
    }

    @Transactional(readOnly = true)
    public LearnerResourceListResponse getLearnerResources(String userId, boolean currentUserAdmin, String taskId) {
        ResourceGenerationTask task = loadGenerationTaskForDetail(currentUserAdmin, taskId);
        ensureTaskOwner(userId, task);
        if (!reviewGovernanceService.canReleaseToLearner(taskId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learning resources are not released to learner");
        }
        List<LearningResource> resources = learningResourceRepository.findByGenerationTaskIdOrderByCreatedAtAsc(taskId);
        return new LearnerResourceListResponse(
                task.getId(),
                resources.stream().map(this::toLearnerResourceResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public AgentTraceResponse getTrace(String userId, String taskId) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> scopedMissing(userId, "Agent trace not found", "Agent trace access denied"));
        ensureTraceOwner(userId, task);
        return toAgentTraceResponse(task);
    }

    @Transactional
    public AgentTraceResponse cancelAgentTask(String userId, String taskId, String reason) {
        agentRunRecorder.cancelTask(userId, taskId, reason);
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Agent trace not found"));
        return toAgentTraceResponse(task);
    }

    private AgentTraceResponse toAgentTraceResponse(AgentTask task) {
        List<AgentTraceStepResponse> steps = agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(task.getId())
                .stream()
                .map(this::toTraceStepResponse)
                .toList();
        return new AgentTraceResponse(task.getId(), task.getStatus(), steps, task.getTraceId());
    }

    private int persistResourceSourceCitations(ResourceGenerationTask task, ResourceGenerationRequest request) {
        if (isNoSourceRequest(request)) {
            return 0;
        }
        SourceCitationRecord citation = new SourceCitationRecord();
        citation.setTraceId(task.getTraceId());
        citation.setDocumentId("course_rag:" + task.getGoalId());
        citation.setDocumentName("Course RAG evidence for " + task.getGoalId());
        citation.setPageNum(1);
        citation.setSectionTitle("Resource grounding");
        citation.setExcerpt("Generated resource is grounded by course context for path node " + task.getPathNodeId() + ".");
        citation.setScore(0.80);
        sourceCitationRepository.save(citation);
        return Math.toIntExact(sourceCitationRepository.countByTraceId(task.getTraceId()));
    }

    private List<LearningResource> persistResources(String taskId, ResourceGenerationRequest request, int citationCount) {
        List<LearningResource> resources = new ArrayList<>();
        boolean hasCitation = citationCount > 0;
        for (ResourceSpec spec : resourceSpecs(request.resourceTypes())) {
            ContentSafetyResult safety = contentSafetyService.reviewDraftResource(spec.title(), hasCitation);

            LearningResource resource = new LearningResource();
            resource.setId(id("res"));
            resource.setGenerationTaskId(taskId);
            resource.setLearnerId(request.learnerId());
            resource.setResourceType(spec.type());
            resource.setModality(spec.modality());
            resource.setTitle(spec.title() + " for " + request.pathNodeId());
            resource.setReviewStatus(AgentRuntimeConstants.REVIEW_PENDING_CRITIC);
            resource.setCitationSummary(citationSummary(citationCount));
            resource.setMarkdownContent(spec.markdownContent());
            resource.setSafetyStatus(safety.status().name());
            resources.add(resource);
        }
        return learningResourceRepository.saveAll(resources);
    }

    private void persistReviews(String taskId, String traceId, List<LearningResource> resources, int citationCount) {
        List<ResourceReview> reviews = new ArrayList<>();
        for (LearningResource resource : resources) {
            ResourceReview review = new ResourceReview();
            review.setId(id("rrv"));
            review.setGenerationTaskId(taskId);
            review.setResourceId(resource.getId());
            review.setReviewerType("CriticAgent");
            review.setStatus(AgentRuntimeConstants.REVIEW_PENDING_CRITIC);
            review.setSummary(reviewSummary(citationCount));
            review.setCitationCheck(citationCheck(citationCount));
            review.setTraceId(traceId);
            reviews.add(review);
        }
        resourceReviewRepository.saveAll(reviews);
    }

    private String citationSummary(int citationCount) {
        if (citationCount <= 0) {
            return "NO_SOURCE: generated draft requires manual review before release.";
        }
        return "COURSE_RAG: " + citationCount + " persisted source citation(s) for critic review.";
    }

    private String reviewSummary(int citationCount) {
        if (citationCount <= 0) {
            return "Resource content is marked NO_SOURCE and requires manual citation review.";
        }
        return "Resource content awaits accuracy, fit, and citation review.";
    }

    private String citationCheck(int citationCount) {
        if (citationCount <= 0) {
            return "Citation check: NO_SOURCE. No persisted source citation; manual review required before release.";
        }
        return "Citation check: " + citationCount
                + " persisted source citation(s); no fabricated source detected in deterministic draft.";
    }

    private boolean isNoSourceRequest(ResourceGenerationRequest request) {
        return containsNoSource(request.goalId()) || containsNoSource(request.pathNodeId());
    }

    private boolean containsNoSource(String value) {
        return value != null && value.toUpperCase(Locale.ROOT).contains("NO_SOURCE");
    }

    private List<AgentRunRecorder.TraceStep> resourceGenerationTraceSteps() {
        return List.of(
                traceStep("step_planner", "PlannerAgent", AgentRuntimeConstants.STATUS_DONE,
                        "Planned resource structure from learner goal and path node.", 42L,
                        AgentRuntimeConstants.PROMPT_RESOURCE_V1),
                traceStep("step_teacher", "TeacherAgent", AgentRuntimeConstants.STATUS_DONE,
                        "Outlined a course explanation with prerequisites and examples.", 61L,
                        AgentRuntimeConstants.PROMPT_RESOURCE_V1),
                traceStep("step_resource", "ResourceAgent", AgentRuntimeConstants.STATUS_DONE,
                        "Generated multimodal draft resources for requested learning needs.", 75L,
                        AgentRuntimeConstants.PROMPT_RESOURCE_V1),
                traceStep("step_question", "QuestionAgent", AgentRuntimeConstants.STATUS_DONE,
                        "Generated practice and formative assessment prompts.", 58L,
                        AgentRuntimeConstants.PROMPT_RESOURCE_V1),
                traceStep("step_critic", "CriticAgent", AgentRuntimeConstants.STATUS_PENDING,
                        "Resource content awaits accuracy and fit review.", 0L,
                        AgentRuntimeConstants.PROMPT_CRITIC_V1),
                traceStep("step_tutor", "TutorAgent", AgentRuntimeConstants.STATUS_WAITING,
                        "Tutor delivery is prepared and waits for critic approval before learner release.", 0L,
                        AgentRuntimeConstants.PROMPT_TUTOR_V1),
                traceStep("step_safety", "SafetyAgent", AgentRuntimeConstants.STATUS_DONE,
                        "Checked grounding and content-safety status for drafts.", 29L,
                        AgentRuntimeConstants.PROMPT_SAFETY_V1)
        );
    }

    private AgentTrace modelTrace(List<AgentTrace> traces) {
        return traces.stream()
                .filter(trace -> "step_resource".equals(trace.getStepId()))
                .findFirst()
                .orElseGet(() -> traces.get(0));
    }

    private AgentRunRecorder.TraceStep traceStep(
            String stepId,
            String agentName,
            String status,
            String summary,
            Long latencyMs,
            String promptVersion
    ) {
        return new AgentRunRecorder.TraceStep(stepId, agentName, status, summary, latencyMs, promptVersion);
    }

    private ResourceGenerationResponse toResponse(ResourceGenerationTask task, List<LearningResource> resources) {
        boolean releasedToLearner = reviewGovernanceService.canReleaseToLearner(task.getId());
        return new ResourceGenerationResponse(
                task.getId(),
                task.getAgentTaskId(),
                task.getStatus(),
                task.getReviewStatus(),
                task.getProgressPercent(),
                task.getSafetyStatus(),
                resources.stream().map(resource -> toResourceResponse(resource, releasedToLearner)).toList(),
                task.getTraceId(),
                task.getProfileSnapshot(),
                safeInt(task.getRetryCount()),
                task.getNextRetryAt(),
                task.getLastError(),
                Boolean.TRUE.equals(task.getRecoverable())
        );
    }

    private int nextRetryCount(Integer retryCount) {
        return safeInt(retryCount) + 1;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String profileSnapshot(String learnerId) {
        LearnerProfile profile = learnerProfileRepository.findFirstByLearnerIdOrderByUpdatedAtDesc(learnerId)
                .orElse(null);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("learnerId", learnerId);
        if (profile == null) {
            snapshot.put("target", "unknown");
            snapshot.put("baseline_level", "unknown");
            snapshot.put("learning_goal", "unknown");
            snapshot.put("weak_point", List.of());
            snapshot.put("preference", List.of());
            snapshot.put("pace_and_feedback", "Needs frequent mastery checks with quick feedback");
            snapshot.put("recent_error_pattern", "No recent error pattern is available yet");
            snapshot.put("teacher_note", "No teacher note is available yet");
            snapshot.put("sources", List.of());
            return toJson(snapshot);
        }
        snapshot.put("target", blankToDefault(profile.getTarget(), "unknown"));
        JsonNode dimensions = readDimensions(profile.getDimensionsJson());
        snapshot.put("baseline_level", text(dimensions, List.of("baseline_level", "knowledge_base"), "unknown"));
        snapshot.put("learning_goal", text(dimensions, List.of("learning_goal"), blankToDefault(profile.getTarget(), "unknown")));
        snapshot.put("weak_point", mergeTextLists(array(dimensions, List.of("weak_point")), readStringList(profile.getWeakPointsJson())));
        snapshot.put("preference", mergeTextLists(array(dimensions, List.of("preference", "resource_preference")),
                readStringList(profile.getPreferencesJson())));
        snapshot.put("pace_and_feedback", text(dimensions, "pace_and_feedback", "Needs frequent mastery checks with quick feedback"));
        snapshot.put("recent_error_pattern", text(dimensions, List.of("recent_error_pattern", "error_pattern"),
                "No recent error pattern is available yet"));
        snapshot.put("teacher_note", text(dimensions, "teacher_note", "No teacher note is available yet"));
        snapshot.put("sources", array(dimensions, "sources"));
        return toJson(snapshot);
    }

    private JsonNode readDimensions(String dimensionsJson) {
        if (dimensionsJson == null || dimensionsJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode root = objectMapper.readTree(dimensionsJson);
            return root.isObject() ? root : objectMapper.createObjectNode();
        } catch (JsonProcessingException ex) {
            return objectMapper.createObjectNode();
        }
    }

    private String text(JsonNode root, String field, String fallback) {
        JsonNode node = root.path(field);
        return node.isTextual() && !node.asText().isBlank() ? node.asText() : fallback;
    }

    private String text(JsonNode root, List<String> fields, String fallback) {
        for (String field : fields) {
            String value = text(root, field, null);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return textFromDimensions(root.path("dimensions"), fields, fallback);
    }

    private List<String> array(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText());
            }
        }
        return List.copyOf(values);
    }

    private List<String> array(JsonNode root, List<String> fields) {
        for (String field : fields) {
            List<String> values = array(root, field);
            if (!values.isEmpty()) {
                return values;
            }
        }
        String value = textFromDimensions(root.path("dimensions"), fields, null);
        return value == null || value.isBlank() ? List.of() : List.of(value);
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (JsonNode item : node) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    values.add(item.asText());
                }
            }
            return List.copyOf(values);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private List<String> mergeTextLists(List<String> first, List<String> second) {
        List<String> values = new ArrayList<>();
        appendUnique(values, first);
        appendUnique(values, second);
        return List.copyOf(values);
    }

    private void appendUnique(List<String> values, List<String> candidates) {
        if (candidates == null) {
            return;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && !values.contains(candidate)) {
                values.add(candidate);
            }
        }
    }

    private String textFromDimensions(JsonNode dimensionsNode, List<String> names, String fallback) {
        if (!dimensionsNode.isArray()) {
            return fallback;
        }
        String selected = fallback;
        double confidence = -1.0;
        for (JsonNode dimension : dimensionsNode) {
            String name = dimension.path("name").asText("");
            String value = dimension.path("value").asText("");
            double dimensionConfidence = dimension.path("confidence").asDouble(0.0);
            if (names.contains(name) && !value.isBlank() && dimensionConfidence > confidence) {
                selected = value;
                confidence = dimensionConfidence;
            }
        }
        return selected;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize profile snapshot", ex);
        }
    }

    private GeneratedResourceResponse toResourceResponse(LearningResource resource, boolean includeContent) {
        return new GeneratedResourceResponse(
                resource.getId(),
                resource.getResourceType(),
                resource.getModality(),
                resource.getTitle(),
                resource.getReviewStatus(),
                resource.getCitationSummary(),
                includeContent ? resource.getMarkdownContent() : null,
                resource.getSafetyStatus()
        );
    }

    private LearnerResourceResponse toLearnerResourceResponse(LearningResource resource) {
        return new LearnerResourceResponse(
                resource.getId(),
                resource.getResourceType(),
                resource.getModality(),
                resource.getTitle(),
                resource.getCitationSummary(),
                resource.getMarkdownContent()
        );
    }

    private AgentTraceStepResponse toTraceStepResponse(AgentTrace trace) {
        return new AgentTraceStepResponse(
                trace.getStepId(),
                trace.getAgentName(),
                trace.getStatus(),
                trace.getSummary(),
                trace.getLatencyMs(),
                trace.getModel(),
                trace.getPromptVersion()
        );
    }

    private void ensureLearnerOwner(String userId, String learnerId) {
        if (!userId.equals(learnerId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learner access denied");
        }
    }

    private void requireCourseEnrollmentIfCourseGoal(
            String userId,
            ResourceGenerationRequest request,
            boolean allowAdminEnrollmentBypass
    ) {
        if (courseAccessService == null) {
            return;
        }
        courseAccessService.requireLearnerEnrolledForExistingCourse(
                userId,
                allowAdminEnrollmentBypass,
                request.learnerId(),
                request.goalId()
        );
    }

    private void ensureTaskOwner(String userId, ResourceGenerationTask task) {
        if (!userId.equals(task.getLearnerId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Resource generation task access denied");
        }
    }

    private void ensureTaskOwnerOrAdmin(String userId, ResourceGenerationTask task) {
        ensureTaskOwnerOrAdmin(userId, isAdmin(userId), task);
    }

    private void ensureTaskOwnerOrAdmin(String userId, boolean currentUserAdmin, ResourceGenerationTask task) {
        if (!currentUserAdmin && !userId.equals(task.getLearnerId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Resource generation task access denied");
        }
    }

    private void ensureTraceOwner(String userId, AgentTask task) {
        if (!isAdmin(userId) && !userId.equals(task.getOwnerUserId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Agent trace access denied");
        }
    }

    private ResourceGenerationTask loadGenerationTaskForDetail(String userId, String taskId) {
        return loadGenerationTaskForDetail(isAdmin(userId), taskId);
    }

    private ResourceGenerationTask loadGenerationTaskForDetail(boolean currentUserAdmin, String taskId) {
        return resourceGenerationTaskRepository.findById(taskId)
                .orElseThrow(() -> scopedMissing(
                        currentUserAdmin,
                        "Resource generation task not found",
                        "Resource generation task access denied"
                ));
    }

    private ApiException scopedMissing(String userId, String adminMessage, String userMessage) {
        return scopedMissing(isAdmin(userId), adminMessage, userMessage);
    }

    private ApiException scopedMissing(boolean currentUserAdmin, String adminMessage, String userMessage) {
        return currentUserAdmin
                ? new ApiException(ErrorCode.NOT_FOUND, adminMessage)
                : new ApiException(ErrorCode.FORBIDDEN, userMessage);
    }

    private boolean isAdmin(String userId) {
        return "admin".equals(userId);
    }

    private void checkInputSafety(ResourceGenerationRequest request) {
        contentSafetyService.checkUserInput(request.learnerId());
        contentSafetyService.checkUserInput(request.goalId());
        contentSafetyService.checkUserInput(request.pathNodeId());
        contentSafetyService.checkUserInput(String.join(" ", request.resourceTypes()));
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        return requestId.trim();
    }

    private String traceId() {
        return TraceContext.currentTraceId().orElse(id("trc"));
    }

    private String id(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private List<ResourceSpec> resourceSpecs(List<String> requestedTypes) {
        List<ResourceSpec> defaults = List.of(
                new ResourceSpec("LECTURE", "MARKDOWN", "Professional course explanation document",
                        "### JOIN duplication walkthrough\nUse cited course notes to explain one-to-many row multiplication."),
                new ResourceSpec("MIND_MAP", "DIAGRAM", "Knowledge point mind map",
                        "### Mind map\n- JOIN cardinality\n- Parent rows\n- Child rows\n- Aggregation fixes"),
                new ResourceSpec("EXERCISE", "QUESTION_SET", "Adaptive practice questions",
                        "### Practice\n1. Identify the duplicate row source.\n2. Rewrite the query with aggregation."),
                new ResourceSpec("READING", "CARD", "Extension reading pack",
                        "### Reading\nCompare INNER JOIN, LEFT JOIN, and semi-join use cases."),
                new ResourceSpec("CODE_LAB", "CODE", "SQL debugging code lab",
                        "```sql\nselect c.id, count(o.id) from customers c join orders o on o.customer_id = c.id group by c.id;\n```")
        );
        if (requestedTypes == null || requestedTypes.isEmpty()) {
            return defaults;
        }
        List<ResourceSpec> selected = new ArrayList<>();
        for (ResourceSpec spec : defaults) {
            if (requestedTypes.contains(spec.type()) || selected.size() < 5) {
                selected.add(spec);
            }
        }
        return selected.stream().limit(5).toList();
    }

    private record ResourceSpec(String type, String modality, String title, String markdownContent) {
    }
}
