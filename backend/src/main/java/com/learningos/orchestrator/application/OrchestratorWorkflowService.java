package com.learningos.orchestrator.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningos.agent.application.AgentExecutionContext;
import com.learningos.agent.application.AgentRunRecorder;
import com.learningos.agent.application.AgentRuntimeConstants;
import com.learningos.agent.application.AiModelGateway;
import com.learningos.agent.application.ResourceGenerationService;
import com.learningos.agent.domain.AgentTask;
import com.learningos.agent.domain.AgentTrace;
import com.learningos.agent.dto.ResourceGenerationRequest;
import com.learningos.agent.repository.AgentTaskRepository;
import com.learningos.agent.repository.AgentTraceRepository;
import com.learningos.assessment.application.AssessmentService;
import com.learningos.assessment.dto.AnswerSubmitRequest;
import com.learningos.assessment.dto.AnswerSubmitResponse;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.common.trace.TraceContext;
import com.learningos.orchestrator.dto.CreateWorkflowRequest;
import com.learningos.orchestrator.dto.OrchestratorWorkflowResponse;
import com.learningos.orchestrator.dto.OrchestratorWorkflowStepResponse;
import com.learningos.orchestrator.dto.OrchestratorWorkflowTraceSummary;
import com.learningos.rag.api.dto.RagQueryDtos.RagQueryResponse;
import com.learningos.rag.application.RagQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrchestratorWorkflowService {

    private static final String ORCHESTRATOR_AGENT_NAME = "Orchestrator";
    private static final String WORKFLOW_START_STEP = "workflow_start";
    private static final String STEP_RUNTIME_FAILURE = "step_runtime_failure";
    private static final String DTO_CREATE_WORKFLOW_REQUEST = "CreateWorkflowRequest";
    private static final String DTO_WORKFLOW_RESPONSE = "OrchestratorWorkflowResponse";
    private static final String DTO_WORKFLOW_FAILURE_EVIDENCE = "WorkflowFailureEvidence";
    private static final String DTO_RESOURCE_GENERATION_REQUEST = "ResourceGenerationRequest";
    private static final String DTO_RESOURCE_GENERATION_RESPONSE = "ResourceGenerationResponse";
    private static final String DTO_RAG_QA_REQUEST = "RagQaWorkflowRequest";
    private static final String DTO_RAG_QA_RESPONSE = "RagQueryResponse";
    private static final String DTO_ANSWER_SUBMIT_REQUEST = "AnswerSubmitRequest";
    private static final String DTO_ANSWER_SUBMIT_RESPONSE = "AnswerSubmitResponse";
    private static final String FAILURE_VALIDATE_OR_PERSIST = "VALIDATE_BEFORE_TASK_OR_PERSIST_RUNTIME_FAILURE";
    private static final String FAILURE_PERSIST_TRACE = "PERSIST_FAILED_TRACE";
    private static final String FAILURE_SANITIZE_AND_PERSIST = "SANITIZE_AND_PERSIST_FAILED_TRACE";
    private static final String RETRY_RESOURCE_ONLY = "RETRY_WORKFLOW_RESOURCE_GENERATION_ONLY";
    private static final String RETRY_RESUBMIT_ORIGINAL = "RESUBMIT_ORIGINAL_REQUEST";
    private static final String RETRY_BY_WORKFLOW_TYPE = "RETRY_BY_WORKFLOW_TYPE";

    private final AgentRunRecorder agentRunRecorder;
    private final AgentTaskRepository agentTaskRepository;
    private final AgentTraceRepository agentTraceRepository;
    private final ResourceGenerationService resourceGenerationService;
    private final RagQueryService ragQueryService;
    private final AssessmentService assessmentService;
    private final ObjectMapper objectMapper;

    public OrchestratorWorkflowService(
            AgentRunRecorder agentRunRecorder,
            AgentTaskRepository agentTaskRepository,
            AgentTraceRepository agentTraceRepository,
            ResourceGenerationService resourceGenerationService,
            RagQueryService ragQueryService,
            AssessmentService assessmentService,
            ObjectMapper objectMapper
    ) {
        this.agentRunRecorder = agentRunRecorder;
        this.agentTaskRepository = agentTaskRepository;
        this.agentTraceRepository = agentTraceRepository;
        this.resourceGenerationService = resourceGenerationService;
        this.ragQueryService = ragQueryService;
        this.assessmentService = assessmentService;
        this.objectMapper = objectMapper;
    }

    @Transactional(noRollbackFor = {
            AiModelGateway.ModelCallFailedException.class,
            ApiException.class,
            RuntimeException.class
    })
    public OrchestratorWorkflowResponse createWorkflow(String ownerUserId, CreateWorkflowRequest request) {
        return createWorkflow(ownerUserId, false, false, request);
    }

    @Transactional(noRollbackFor = {
            AiModelGateway.ModelCallFailedException.class,
            ApiException.class,
            RuntimeException.class
    })
    public OrchestratorWorkflowResponse createWorkflow(
            String ownerUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            CreateWorkflowRequest request
    ) {
        OrchestratorWorkflowType workflowType = OrchestratorWorkflowType.from(request.workflowType());
        ResourceGenerationRequest resourceGenerationRequest = workflowType == OrchestratorWorkflowType.RESOURCE_GENERATION
                ? resourceGenerationRequest(ownerUserId, request)
                : null;
        RagQaWorkflowRequest ragQaRequest = workflowType == OrchestratorWorkflowType.RAG_QA
                ? ragQaRequest(ownerUserId, request)
                : null;
        AnswerSubmitRequest answerSubmitRequest = workflowType == OrchestratorWorkflowType.ANSWER_SUBMISSION
                ? answerSubmissionRequest(ownerUserId, request)
                : null;
        if (ragQaRequest != null && ragQueryService
                .replayQueryIfPresent(
                        ownerUserId,
                        currentUserAdmin,
                        currentUserTeacher,
                        ragQaRequest.kbIds(),
                        ragQaRequest.question(),
                        ragQaRequest.topK(),
                        ragQaRequest.requestId()
                )
                .isPresent()) {
            return replayRagQaWorkflow(ownerUserId, ragQaRequest);
        }
        if (answerSubmitRequest != null && assessmentService.replayAnswerIfPresent(ownerUserId, answerSubmitRequest).isPresent()) {
            return replayAnswerSubmissionWorkflow(ownerUserId, answerSubmitRequest);
        }
        String workflowId = id("wf");
        String traceId = TraceContext.currentTraceId().orElse(id("trc"));
        String inputJson = workflowEnvelope(ownerUserId, workflowId, workflowType, request, ragQaRequest);

        AgentExecutionContext context = agentRunRecorder.startRun(new AgentRunRecorder.RunStart(
                ownerUserId,
                workflowType.taskType(),
                AgentRuntimeConstants.STATUS_RUNNING,
                inputJson,
                """
                        {"status":"RUNNING","summary":"Workflow envelope created."}
                        """.trim(),
                traceId,
                0L
        ));

        agentRunRecorder.recordTraceSteps(context, List.of(new AgentRunRecorder.TraceStep(
                WORKFLOW_START_STEP,
                ORCHESTRATOR_AGENT_NAME,
                AgentRuntimeConstants.STATUS_RUNNING,
                "Workflow " + workflowId + " started for " + workflowType.value() + ".",
                0L,
                null
        )));

        try {
            if (workflowType == OrchestratorWorkflowType.RESOURCE_GENERATION) {
                resourceGenerationService.createTaskInWorkflow(
                        ownerUserId,
                        currentUserAdmin,
                        currentUserTeacher,
                        resourceGenerationRequest,
                        context
                );
            }
            if (workflowType == OrchestratorWorkflowType.RAG_QA) {
                executeRagQaWorkflow(ownerUserId, currentUserAdmin, currentUserTeacher, ragQaRequest, context);
            }
            if (workflowType == OrchestratorWorkflowType.ANSWER_SUBMISSION) {
                OrchestratorWorkflowResponse replayed = executeAnswerSubmissionWorkflow(ownerUserId, answerSubmitRequest, context);
                if (replayed != null) {
                    cleanupTransientWorkflow(context);
                    return replayed;
                }
            }
        } catch (AiModelGateway.ModelCallFailedException exception) {
            throw exception;
        } catch (ApiException exception) {
            recordRuntimeFailure(workflowType, context, exception);
            throw exception;
        } catch (RuntimeException exception) {
            recordRuntimeFailure(workflowType, context, ErrorCode.INTERNAL_ERROR);
            throw exception;
        }

        AgentTask task = agentTaskRepository.findById(context.agentTaskId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Workflow not found"));
        List<AgentTrace> traces = agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(context.agentTaskId());
        return workflowResponse(
                workflowId,
                workflowType.value(),
                context.agentTaskId(),
                context.traceId(),
                task.getStatus(),
                traces,
                request.retryOfWorkflowId()
        );
    }

    private ResourceGenerationRequest resourceGenerationRequest(String ownerUserId, CreateWorkflowRequest request) {
        if (!ownerUserId.equals(request.learnerId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learner access denied");
        }
        JsonNode payload = payloadNode(request.payloadJson());
        String goalId = requiredText(payload, "goalId");
        String pathNodeId = requiredText(payload, "pathNodeId");
        List<String> resourceTypes = resourceTypes(payload);
        return new ResourceGenerationRequest(
                request.learnerId(),
                goalId,
                pathNodeId,
                resourceTypes,
                request.requestId()
        );
    }

    private RagQaWorkflowRequest ragQaRequest(String ownerUserId, CreateWorkflowRequest request) {
        if (!ownerUserId.equals(request.learnerId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learner access denied");
        }
        String requestId = normalizedRequestId(request.requestId());
        if (requestId == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "requestId is required for RAG_QA");
        }
        if (requestId.length() > 120) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "requestId length must be less than or equal to 120");
        }
        JsonNode payload = payloadNode(request.payloadJson());
        JsonNode kbIdsNode = payload.path("kbIds");
        if (!kbIdsNode.isArray() || kbIdsNode.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "payloadJson.kbIds must contain at least one item");
        }
        List<String> kbIds = new ArrayList<>();
        for (JsonNode item : kbIdsNode) {
            String value = item.asText(null);
            if (value != null && !value.isBlank()) {
                kbIds.add(value.trim());
            }
        }
        if (kbIds.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "payloadJson.kbIds must contain at least one item");
        }
        String question = requiredText(payload, "question");
        Integer topK = payload.hasNonNull("topK") ? payload.path("topK").asInt() : null;
        return new RagQaWorkflowRequest(kbIds, question, topK, requestId);
    }

    private AnswerSubmitRequest answerSubmissionRequest(String ownerUserId, CreateWorkflowRequest request) {
        if (!ownerUserId.equals(request.learnerId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learner access denied");
        }
        String requestId = normalizedRequestId(request.requestId());
        if (requestId == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "requestId is required for ANSWER_SUBMISSION");
        }
        if (requestId.length() > 120) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "requestId length must be less than or equal to 120");
        }
        JsonNode payload = payloadNode(request.payloadJson());
        return new AnswerSubmitRequest(
                request.learnerId(),
                requiredText(payload, "questionId"),
                requiredText(payload, "answer"),
                requestId
        );
    }

    private void executeRagQaWorkflow(
            String ownerUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            RagQaWorkflowRequest request,
            AgentExecutionContext context
    ) {
        RagQueryResponse response = ragQueryService.queryWithTraceIdAndRequestId(
                ownerUserId,
                currentUserAdmin,
                currentUserTeacher,
                request.kbIds(),
                request.question(),
                request.topK(),
                context.traceId(),
                request.requestId()
        );
        agentRunRecorder.recordTraceSteps(context, List.of(
                new AgentRunRecorder.TraceStep(
                        "step_rag_safety",
                        "CourseRagAgent",
                        AgentRuntimeConstants.STATUS_DONE,
                        "RAG input safety check completed.",
                        0L,
                        AgentRuntimeConstants.PROMPT_TUTOR_V1
                ),
                new AgentRunRecorder.TraceStep(
                        "step_rag_retrieval",
                        "CourseRagAgent",
                        AgentRuntimeConstants.STATUS_DONE,
                        "RAG retrieval finished with strategy %s, %d candidates, %d retrieved chunks, %d citations."
                                .formatted(
                                        response.retrieval().strategy(),
                                        response.retrieval().candidateCount(),
                                        response.retrieval().retrievalCount(),
                                        response.retrieval().citationCount()
                                ),
                        0L,
                        AgentRuntimeConstants.PROMPT_TUTOR_V1
                ),
                new AgentRunRecorder.TraceStep(
                        "step_rag_answer",
                        "CourseRagAgent",
                        AgentRuntimeConstants.STATUS_DONE,
                        response.retrieval().noSource()
                                ? "RAG returned NO_SOURCE response without persisted citations."
                                : "RAG answer generated with %d source citation(s).".formatted(response.sources().size()),
                        0L,
                        AgentRuntimeConstants.PROMPT_TUTOR_V1
                )
        ));
        agentRunRecorder.transitionTask(
                context,
                AgentRuntimeConstants.STATUS_DONE,
                "RAG_QA workflow completed with strategy " + response.retrieval().strategy() + ".",
                0L
        );
    }

    private OrchestratorWorkflowResponse executeAnswerSubmissionWorkflow(
            String ownerUserId,
            AnswerSubmitRequest request,
            AgentExecutionContext context
    ) {
        AnswerSubmitResponse response = assessmentService.submitAnswerWithTraceId(
                ownerUserId,
                request,
                context.traceId()
        );
        if (!context.traceId().equals(response.traceId())) {
            return replayAnswerSubmissionWorkflow(ownerUserId, request, response.traceId());
        }
        agentRunRecorder.recordTraceSteps(context, List.of(
                new AgentRunRecorder.TraceStep(
                        "step_assessment_safety",
                        "AssessmentAgent",
                        AgentRuntimeConstants.STATUS_DONE,
                        "Answer safety check completed for " + response.answerId() + ".",
                        0L,
                        AgentRuntimeConstants.PROMPT_SAFETY_V1
                ),
                new AgentRunRecorder.TraceStep(
                        "step_assessment_grading",
                        "AssessmentAgent",
                        AgentRuntimeConstants.STATUS_DONE,
                        "Answer grading completed with score %.2f.".formatted(response.score()),
                        0L,
                        null
                ),
                new AgentRunRecorder.TraceStep(
                        "step_assessment_feedback",
                        "FeedbackDiagnosisAgent",
                        AgentRuntimeConstants.STATUS_DONE,
                        "Feedback diagnosis completed for category " + feedbackCategory(response) + ".",
                        0L,
                        null
                ),
                new AgentRunRecorder.TraceStep(
                        "step_assessment_mastery",
                        "KnowledgeDiagnosisAgent",
                        AgentRuntimeConstants.STATUS_DONE,
                        "Mastery updated for " + response.masteryUpdates().size() + " knowledge point(s).",
                        0L,
                        null
                ),
                new AgentRunRecorder.TraceStep(
                        "step_assessment_replan",
                        "LearningPathPlannerAgent",
                        AgentRuntimeConstants.STATUS_DONE,
                        "Learning path replan decision: " + response.replanDecision().status() + ".",
                        0L,
                        null
                )
        ));
        agentRunRecorder.transitionTask(
                context,
                AgentRuntimeConstants.STATUS_DONE,
                "ANSWER_SUBMISSION workflow completed for answer " + response.answerId() + ".",
                0L
        );
        return null;
    }

    private OrchestratorWorkflowResponse replayAnswerSubmissionWorkflow(
            String ownerUserId,
            AnswerSubmitRequest request
    ) {
        return replayAnswerSubmissionWorkflow(ownerUserId, request, null);
    }

    private OrchestratorWorkflowResponse replayRagQaWorkflow(
            String ownerUserId,
            RagQaWorkflowRequest request
    ) {
        AgentTask task = findRagQaWorkflow(ownerUserId, request)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.CONFLICT,
                        "requestId already used outside orchestrator workflow"
                ));
        return workflowResponse(task);
    }

    private OrchestratorWorkflowResponse replayAnswerSubmissionWorkflow(
            String ownerUserId,
            AnswerSubmitRequest request,
            String requiredTraceId
    ) {
        AgentTask task = findAnswerSubmissionWorkflow(ownerUserId, request, requiredTraceId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.CONFLICT,
                        "requestId already used outside orchestrator workflow"
                ));
        return workflowResponse(task);
    }

    private void recordRuntimeFailure(
            OrchestratorWorkflowType workflowType,
            AgentExecutionContext context,
            ApiException exception
    ) {
        recordRuntimeFailure(workflowType, context, exception.getErrorCode());
    }

    private void recordRuntimeFailure(
            OrchestratorWorkflowType workflowType,
            AgentExecutionContext context,
            ErrorCode errorCode
    ) {
        String summary = "Workflow %s failed with %s."
                .formatted(workflowType.value(), errorCode.name());
        agentRunRecorder.recordRuntimeFailure(
                context,
                "step_runtime_failure",
                ORCHESTRATOR_AGENT_NAME,
                summary,
                0L,
                null,
                errorCode.name()
        );
    }

    private String requiredText(JsonNode payload, String fieldName) {
        String value = payload.path(fieldName).asText(null);
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "payloadJson." + fieldName + " is required");
        }
        return value.trim();
    }

    private List<String> resourceTypes(JsonNode payload) {
        JsonNode node = payload.path("resourceTypes");
        if (!node.isArray() || node.isEmpty()) {
            return List.of("LECTURE", "EXERCISE");
        }
        List<String> resourceTypes = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText(null);
            if (value != null && !value.isBlank()) {
                resourceTypes.add(value.trim());
            }
        }
        if (resourceTypes.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "payloadJson.resourceTypes must contain at least one item");
        }
        return resourceTypes;
    }

    @Transactional(readOnly = true)
    public OrchestratorWorkflowResponse getWorkflow(String ownerUserId, String workflowId) {
        AgentTask task = findWorkflowTask(ownerUserId, workflowId);
        return workflowResponse(task, workflowId);
    }

    @Transactional(noRollbackFor = {
            AiModelGateway.ModelCallFailedException.class,
            ApiException.class,
            RuntimeException.class
    })
    public OrchestratorWorkflowResponse retryWorkflow(String ownerUserId, String workflowId) {
        return retryWorkflow(ownerUserId, false, false, workflowId);
    }

    @Transactional(noRollbackFor = {
            AiModelGateway.ModelCallFailedException.class,
            ApiException.class,
            RuntimeException.class
    })
    public OrchestratorWorkflowResponse retryWorkflow(
            String ownerUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String workflowId
    ) {
        AgentTask originalTask = findWorkflowTask(ownerUserId, workflowId);
        if (!AgentRuntimeConstants.STATUS_FAILED.equals(originalTask.getStatus())) {
            throw new ApiException(ErrorCode.CONFLICT, "Only FAILED workflows can be retried");
        }
        JsonNode envelope = workflowEnvelopeNode(originalTask.getInputJson());
        String workflowType = envelope.path("workflowType").asText();
        if (!OrchestratorWorkflowType.RESOURCE_GENERATION.value().equals(workflowType)) {
            throw new ApiException(ErrorCode.CONFLICT, workflowType + " workflow retry requires resubmitting the original request");
        }
        JsonNode payload = envelope.path("payload");
        String retryRequestId = retryRequestId(workflowId);
        CreateWorkflowRequest retryRequest = new CreateWorkflowRequest(
                workflowType,
                envelope.path("learnerId").asText(ownerUserId),
                payload.toString(),
                retryRequestId,
                workflowId
        );
        return createWorkflow(ownerUserId, currentUserAdmin, currentUserTeacher, retryRequest);
    }

    private String workflowEnvelope(
            String ownerUserId,
            String workflowId,
            OrchestratorWorkflowType workflowType,
            CreateWorkflowRequest request,
            RagQaWorkflowRequest ragQaRequest
    ) {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("workflowId", workflowId);
        envelope.put("workflowType", workflowType.value());
        envelope.put("ownerUserId", ownerUserId);
        envelope.put("learnerId", request.learnerId());
        String requestId = normalizedRequestId(request.requestId());
        if (requestId == null) {
            envelope.putNull("requestId");
        } else {
            envelope.put("requestId", requestId);
        }
        String retryOfWorkflowId = normalizedRequestId(request.retryOfWorkflowId());
        if (retryOfWorkflowId != null) {
            envelope.put("retryOfWorkflowId", retryOfWorkflowId);
        }
        envelope.set("payload", workflowPayload(workflowType, request.payloadJson(), ragQaRequest));
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Unable to serialize workflow envelope");
        }
    }

    private JsonNode workflowPayload(
            OrchestratorWorkflowType workflowType,
            String payloadJson,
            RagQaWorkflowRequest ragQaRequest
    ) {
        JsonNode payload = payloadNode(payloadJson);
        if (workflowType == OrchestratorWorkflowType.RAG_QA) {
            return ragQaPayloadSnapshot(ragQaRequest);
        }
        if (workflowType != OrchestratorWorkflowType.ANSWER_SUBMISSION) {
            return payload;
        }
        ObjectNode sanitized = objectMapper.createObjectNode();
        sanitized.put("questionId", payload.path("questionId").asText(null));
        String answer = payload.path("answer").asText("");
        sanitized.put("answerLength", answer.length());
        return sanitized;
    }

    private ObjectNode ragQaPayloadSnapshot(RagQaWorkflowRequest request) {
        ObjectNode sanitized = objectMapper.createObjectNode();
        sanitized.put("questionHash", sha256(request.question()));
        sanitized.put("questionLength", request.question().length());
        sanitized.put("kbIdsHash", sha256(String.join(",", normalizeKbIds(request.kbIds()))));
        sanitized.put("kbCount", normalizeKbIds(request.kbIds()).size());
        sanitized.put("topK", request.topK() == null || request.topK() <= 0 ? 20 : request.topK());
        return sanitized;
    }

    private JsonNode payloadNode(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(payloadJson);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "payloadJson must be valid JSON");
        }
    }

    private OrchestratorWorkflowResponse workflowResponse(
            String workflowId,
            String workflowType,
            String agentTaskId,
            String traceId,
            String status,
            List<AgentTrace> traces,
            String retryOfWorkflowId
    ) {
        List<OrchestratorWorkflowStepResponse> steps = traces.stream()
                .map(trace -> toStepResponse(workflowType, trace))
                .toList();
        OrchestratorWorkflowStepResponse recentFailedStep = recentFailedStep(steps);
        return new OrchestratorWorkflowResponse(
                workflowId,
                workflowType,
                agentTaskId,
                traceId,
                status,
                steps,
                recentFailedStep,
                traceSummary(traceId, agentTaskId, steps),
                nextActions(status, workflowType),
                retryOfWorkflowId
        );
    }

    private OrchestratorWorkflowResponse workflowResponse(AgentTask task) {
        return workflowResponse(task, null);
    }

    private OrchestratorWorkflowResponse workflowResponse(AgentTask task, String fallbackWorkflowId) {
        JsonNode envelope = workflowEnvelopeNode(task.getInputJson());
        List<AgentTrace> traces = agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(task.getId());
        return workflowResponse(
                envelope.path("workflowId").asText(fallbackWorkflowId),
                envelope.path("workflowType").asText(task.getTaskType()),
                task.getId(),
                task.getTraceId(),
                task.getStatus(),
                traces,
                envelope.path("retryOfWorkflowId").asText(null)
        );
    }

    private OrchestratorWorkflowStepResponse toStepResponse(String workflowType, AgentTrace trace) {
        WorkflowNodeContract contract = nodeContract(workflowType, trace.getStepId());
        return new OrchestratorWorkflowStepResponse(
                trace.getStepId(),
                trace.getAgentName(),
                trace.getStatus(),
                trace.getSummary(),
                trace.getLatencyMs(),
                trace.getModel(),
                trace.getPromptVersion(),
                trace.getSequenceNo(),
                contract.inputDto(),
                contract.outputDto(),
                contract.failurePolicy(),
                contract.retryPolicy(),
                contract.retryable()
        );
    }

    private OrchestratorWorkflowStepResponse recentFailedStep(List<OrchestratorWorkflowStepResponse> steps) {
        OrchestratorWorkflowStepResponse recentFailedStep = null;
        for (OrchestratorWorkflowStepResponse step : steps) {
            if (AgentRuntimeConstants.STATUS_FAILED.equals(step.status())) {
                recentFailedStep = step;
            }
        }
        return recentFailedStep;
    }

    private OrchestratorWorkflowTraceSummary traceSummary(
            String traceId,
            String agentTaskId,
            List<OrchestratorWorkflowStepResponse> steps
    ) {
        int failedSteps = 0;
        OrchestratorWorkflowStepResponse lastStep = null;
        for (OrchestratorWorkflowStepResponse step : steps) {
            if (AgentRuntimeConstants.STATUS_FAILED.equals(step.status())) {
                failedSteps++;
            }
            lastStep = step;
        }
        return new OrchestratorWorkflowTraceSummary(
                traceId,
                agentTaskId,
                steps.size(),
                failedSteps,
                lastStep == null ? null : lastStep.stepId(),
                lastStep == null ? null : lastStep.status()
        );
    }

    private List<String> nextActions(String status, String workflowType) {
        if (AgentRuntimeConstants.STATUS_WAITING_REVIEW.equals(status)) {
            return List.of("OPEN_REVIEW_QUEUE", "CHECK_STATUS");
        }
        if (AgentRuntimeConstants.STATUS_FAILED.equals(status)) {
            WorkflowNodeContract contract = workflowStartContract(workflowType);
            return contract.retryable()
                    ? List.of("INSPECT_TRACE", "RETRY_WORKFLOW")
                    : List.of("INSPECT_TRACE", RETRY_RESUBMIT_ORIGINAL);
        }
        if (AgentRuntimeConstants.STATUS_DONE.equals(status)) {
            return List.of("VIEW_RESULT");
        }
        if (AgentRuntimeConstants.STATUS_CANCELLED.equals(status)) {
            return List.of("START_NEW_WORKFLOW");
        }
        if (AgentRuntimeConstants.STATUS_RUNNING.equals(status)
                || AgentRuntimeConstants.STATUS_PENDING.equals(status)
                || AgentRuntimeConstants.STATUS_WAITING.equals(status)) {
            return List.of("CHECK_STATUS");
        }
        List<String> actions = new ArrayList<>();
        actions.add("CHECK_STATUS");
        return actions;
    }

    private WorkflowNodeContract nodeContract(String workflowType, String stepId) {
        if (WORKFLOW_START_STEP.equals(stepId)) {
            return workflowStartContract(workflowType);
        }
        if (STEP_RUNTIME_FAILURE.equals(stepId)) {
            WorkflowNodeContract workflowContract = workflowRuntimeContract(workflowType);
            return new WorkflowNodeContract(
                    workflowContract.inputDto(),
                    DTO_WORKFLOW_FAILURE_EVIDENCE,
                    FAILURE_SANITIZE_AND_PERSIST,
                    workflowContract.retryPolicy(),
                    workflowContract.retryable()
            );
        }
        if (OrchestratorWorkflowType.RESOURCE_GENERATION.value().equals(workflowType)) {
            return new WorkflowNodeContract(
                    DTO_RESOURCE_GENERATION_REQUEST,
                    DTO_RESOURCE_GENERATION_RESPONSE,
                    FAILURE_PERSIST_TRACE,
                    RETRY_RESOURCE_ONLY,
                    true
            );
        }
        if (OrchestratorWorkflowType.RAG_QA.value().equals(workflowType)) {
            return new WorkflowNodeContract(
                    DTO_RAG_QA_REQUEST,
                    DTO_RAG_QA_RESPONSE,
                    FAILURE_SANITIZE_AND_PERSIST,
                    RETRY_RESUBMIT_ORIGINAL,
                    false
            );
        }
        if (OrchestratorWorkflowType.ANSWER_SUBMISSION.value().equals(workflowType)) {
            return new WorkflowNodeContract(
                    DTO_ANSWER_SUBMIT_REQUEST,
                    DTO_ANSWER_SUBMIT_RESPONSE,
                    FAILURE_SANITIZE_AND_PERSIST,
                    RETRY_RESUBMIT_ORIGINAL,
                    false
            );
        }
        return new WorkflowNodeContract(
                DTO_CREATE_WORKFLOW_REQUEST,
                DTO_WORKFLOW_RESPONSE,
                FAILURE_VALIDATE_OR_PERSIST,
                RETRY_BY_WORKFLOW_TYPE,
                false
        );
    }

    private WorkflowNodeContract workflowRuntimeContract(String workflowType) {
        if (OrchestratorWorkflowType.RESOURCE_GENERATION.value().equals(workflowType)) {
            return new WorkflowNodeContract(
                    DTO_RESOURCE_GENERATION_REQUEST,
                    DTO_RESOURCE_GENERATION_RESPONSE,
                    FAILURE_PERSIST_TRACE,
                    RETRY_RESOURCE_ONLY,
                    true
            );
        }
        if (OrchestratorWorkflowType.RAG_QA.value().equals(workflowType)) {
            return new WorkflowNodeContract(
                    DTO_RAG_QA_REQUEST,
                    DTO_RAG_QA_RESPONSE,
                    FAILURE_SANITIZE_AND_PERSIST,
                    RETRY_RESUBMIT_ORIGINAL,
                    false
            );
        }
        if (OrchestratorWorkflowType.ANSWER_SUBMISSION.value().equals(workflowType)) {
            return new WorkflowNodeContract(
                    DTO_ANSWER_SUBMIT_REQUEST,
                    DTO_ANSWER_SUBMIT_RESPONSE,
                    FAILURE_SANITIZE_AND_PERSIST,
                    RETRY_RESUBMIT_ORIGINAL,
                    false
            );
        }
        return workflowStartContract(workflowType);
    }

    private WorkflowNodeContract workflowStartContract(String workflowType) {
        boolean retryable = OrchestratorWorkflowType.RESOURCE_GENERATION.value().equals(workflowType);
        return new WorkflowNodeContract(
                DTO_CREATE_WORKFLOW_REQUEST,
                DTO_WORKFLOW_RESPONSE,
                FAILURE_VALIDATE_OR_PERSIST,
                retryable ? RETRY_RESOURCE_ONLY : RETRY_RESUBMIT_ORIGINAL,
                retryable
        );
    }

    private JsonNode workflowEnvelopeNode(String inputJson) {
        try {
            return objectMapper.readTree(inputJson);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Unable to read workflow envelope");
        }
    }

    private AgentTask findWorkflowTask(String ownerUserId, String workflowId) {
        return agentTaskRepository
                .findByOwnerUserIdAndInputJsonContaining(ownerUserId, workflowIdMarker(workflowId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Workflow not found"));
    }

    private java.util.Optional<AgentTask> findAnswerSubmissionWorkflow(
            String ownerUserId,
            AnswerSubmitRequest request,
            String requiredTraceId
    ) {
        return agentTaskRepository
                .findByOwnerUserIdAndTaskTypeAndInputJsonContainingOrderByCreatedAtAsc(
                        ownerUserId,
                        OrchestratorWorkflowType.ANSWER_SUBMISSION.taskType(),
                        requestIdMarker(request.requestId())
                )
                .stream()
                .filter(task -> requiredTraceId == null || requiredTraceId.equals(task.getTraceId()))
                .filter(task -> matchesAnswerSubmissionEnvelope(task, ownerUserId, request))
                .findFirst();
    }

    private boolean matchesAnswerSubmissionEnvelope(
            AgentTask task,
            String ownerUserId,
            AnswerSubmitRequest request
    ) {
        JsonNode envelope = workflowEnvelopeNode(task.getInputJson());
        JsonNode payload = envelope.path("payload");
        return OrchestratorWorkflowType.ANSWER_SUBMISSION.value().equals(envelope.path("workflowType").asText())
                && ownerUserId.equals(envelope.path("ownerUserId").asText())
                && request.learnerId().equals(envelope.path("learnerId").asText())
                && request.requestId().equals(envelope.path("requestId").asText())
                && request.questionId().equals(payload.path("questionId").asText())
                && request.answer().length() == payload.path("answerLength").asInt(-1);
    }

    private java.util.Optional<AgentTask> findRagQaWorkflow(
            String ownerUserId,
            RagQaWorkflowRequest request
    ) {
        return agentTaskRepository
                .findByOwnerUserIdAndTaskTypeAndInputJsonContainingOrderByCreatedAtAsc(
                        ownerUserId,
                        OrchestratorWorkflowType.RAG_QA.taskType(),
                        requestIdMarker(request.requestId())
                )
                .stream()
                .filter(task -> matchesRagQaEnvelope(task, ownerUserId, request))
                .findFirst();
    }

    private boolean matchesRagQaEnvelope(
            AgentTask task,
            String ownerUserId,
            RagQaWorkflowRequest request
    ) {
        JsonNode envelope = workflowEnvelopeNode(task.getInputJson());
        JsonNode payload = envelope.path("payload");
        ObjectNode expected = ragQaPayloadSnapshot(request);
        return OrchestratorWorkflowType.RAG_QA.value().equals(envelope.path("workflowType").asText())
                && ownerUserId.equals(envelope.path("ownerUserId").asText())
                && ownerUserId.equals(envelope.path("learnerId").asText())
                && request.requestId().equals(envelope.path("requestId").asText())
                && expected.path("questionHash").asText().equals(payload.path("questionHash").asText())
                && expected.path("questionLength").asInt(-1) == payload.path("questionLength").asInt(-1)
                && expected.path("kbIdsHash").asText().equals(payload.path("kbIdsHash").asText())
                && expected.path("kbCount").asInt(-1) == payload.path("kbCount").asInt(-1)
                && expected.path("topK").asInt(-1) == payload.path("topK").asInt(-1);
    }

    private void cleanupTransientWorkflow(AgentExecutionContext context) {
        agentTraceRepository.deleteByAgentTaskId(context.agentTaskId());
        agentTaskRepository.deleteById(context.agentTaskId());
    }

    private String workflowIdMarker(String workflowId) {
        return "\"workflowId\":\"" + workflowId + "\"";
    }

    private String requestIdMarker(String requestId) {
        try {
            return "\"requestId\":" + objectMapper.writeValueAsString(requestId);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Unable to serialize requestId marker");
        }
    }

    private String retryRequestId(String workflowId) {
        String suffix = workflowId == null || workflowId.isBlank()
                ? id("wf")
                : workflowId.trim();
        if (suffix.length() > 72) {
            suffix = suffix.substring(0, 72);
        }
        return "retry_" + suffix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String normalizedRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        return requestId.trim();
    }

    private String feedbackCategory(AnswerSubmitResponse response) {
        if (response.feedbackDiagnosis() == null || response.feedbackDiagnosis().wrongCauseCategory() == null) {
            return "UNKNOWN";
        }
        return response.feedbackDiagnosis().wrongCauseCategory().name();
    }

    private String id(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private List<String> normalizeKbIds(List<String> kbIds) {
        if (kbIds == null) {
            return List.of();
        }
        return kbIds.stream()
                .filter(kbId -> kbId != null && !kbId.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private record RagQaWorkflowRequest(List<String> kbIds, String question, Integer topK, String requestId) {
    }

    private record WorkflowNodeContract(
            String inputDto,
            String outputDto,
            String failurePolicy,
            String retryPolicy,
            boolean retryable
    ) {
    }
}
