package com.learningos.orchestrator.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.agent.application.AgentRuntimeConstants;
import com.learningos.agent.application.AiModelGateway;
import com.learningos.agent.domain.AgentTask;
import com.learningos.agent.repository.AgentTaskRepository;
import com.learningos.agent.repository.AgentTraceRepository;
import com.learningos.agent.repository.LearningResourceRepository;
import com.learningos.agent.repository.ModelCallLogRepository;
import com.learningos.agent.repository.ResourceGenerationTaskRepository;
import com.learningos.agent.repository.ResourceReviewRepository;
import com.learningos.agent.repository.TokenUsageLogRepository;
import com.learningos.assessment.application.AssessmentService;
import com.learningos.assessment.dto.AnswerSubmitRequest;
import com.learningos.assessment.repository.AnswerRecordRepository;
import com.learningos.assessment.repository.GradingResultRepository;
import com.learningos.assessment.repository.WrongQuestionRepository;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.domain.CourseEnrollment;
import com.learningos.knowledge.domain.KnowledgePoint;
import com.learningos.knowledge.repository.CourseEnrollmentRepository;
import com.learningos.knowledge.repository.KnowledgePointRepository;
import com.learningos.knowledge.repository.CourseRepository;
import com.learningos.learning.repository.LearningEventRepository;
import com.learningos.learning.repository.MasteryRecordRepository;
import com.learningos.rag.domain.KbDocChunk;
import com.learningos.rag.domain.KbDocument;
import com.learningos.rag.domain.KnowledgeBase;
import com.learningos.rag.domain.enums.DocumentStatus;
import com.learningos.rag.domain.enums.Visibility;
import com.learningos.rag.repository.KbDocChunkRepository;
import com.learningos.rag.repository.KbDocumentRepository;
import com.learningos.rag.repository.KbQueryLogRepository;
import com.learningos.rag.repository.KnowledgeBaseRepository;
import com.learningos.rag.repository.SourceCitationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "learning-os.auth.jwt-secret=unit-test-secret-32-bytes-long!!",
        "learning-os.auth.issuer=learning-os"
})
class OrchestratorWorkflowControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final AgentTaskRepository agentTaskRepository;
    private final AgentTraceRepository agentTraceRepository;
    private final ResourceGenerationTaskRepository resourceGenerationTaskRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final ResourceReviewRepository resourceReviewRepository;
    private final ModelCallLogRepository modelCallLogRepository;
    private final TokenUsageLogRepository tokenUsageLogRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbDocumentRepository documentRepository;
    private final KbDocChunkRepository chunkRepository;
    private final KbQueryLogRepository queryLogRepository;
    private final SourceCitationRepository sourceCitationRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final GradingResultRepository gradingResultRepository;
    private final MasteryRecordRepository masteryRecordRepository;
    private final WrongQuestionRepository wrongQuestionRepository;
    private final LearningEventRepository learningEventRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final KnowledgePointRepository knowledgePointRepository;

    @MockitoSpyBean
    private AiModelGateway aiModelGateway;

    @MockitoSpyBean
    private AssessmentService assessmentService;

    OrchestratorWorkflowControllerTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            AgentTaskRepository agentTaskRepository,
            AgentTraceRepository agentTraceRepository,
            ResourceGenerationTaskRepository resourceGenerationTaskRepository,
            LearningResourceRepository learningResourceRepository,
            ResourceReviewRepository resourceReviewRepository,
            ModelCallLogRepository modelCallLogRepository,
            TokenUsageLogRepository tokenUsageLogRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            KbDocumentRepository documentRepository,
            KbDocChunkRepository chunkRepository,
            KbQueryLogRepository queryLogRepository,
            SourceCitationRepository sourceCitationRepository,
            AnswerRecordRepository answerRecordRepository,
            GradingResultRepository gradingResultRepository,
            MasteryRecordRepository masteryRecordRepository,
            WrongQuestionRepository wrongQuestionRepository,
            LearningEventRepository learningEventRepository,
            CourseRepository courseRepository,
            CourseEnrollmentRepository courseEnrollmentRepository,
            KnowledgePointRepository knowledgePointRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.agentTaskRepository = agentTaskRepository;
        this.agentTraceRepository = agentTraceRepository;
        this.resourceGenerationTaskRepository = resourceGenerationTaskRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.resourceReviewRepository = resourceReviewRepository;
        this.modelCallLogRepository = modelCallLogRepository;
        this.tokenUsageLogRepository = tokenUsageLogRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.queryLogRepository = queryLogRepository;
        this.sourceCitationRepository = sourceCitationRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.gradingResultRepository = gradingResultRepository;
        this.masteryRecordRepository = masteryRecordRepository;
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.learningEventRepository = learningEventRepository;
        this.courseRepository = courseRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.knowledgePointRepository = knowledgePointRepository;
    }

    @Test
    void createsAnswerSubmissionWorkflowAndReusesWorkflowTraceContext() throws Exception {
        String body = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "ANSWER_SUBMISSION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"questionId\\":\\"q_sql_join\\",\\"answer\\":\\"JOIN duplicates happen when a one-to-many relation is joined.\\"}",
                                  "requestId": "req_orchestrated_answer_once"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.workflowId").isNotEmpty())
                .andExpect(jsonPath("$.data.workflowType").value("ANSWER_SUBMISSION"))
                .andExpect(jsonPath("$.data.agentTaskId").isNotEmpty())
                .andExpect(jsonPath("$.data.traceId").value("trc_orchestrator_answer"))
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_DONE))
                .andExpect(jsonPath("$.data.steps.length()").value(6))
                .andExpect(jsonPath("$.data.steps[0].stepId").value("workflow_start"))
                .andExpect(jsonPath("$.data.steps[1].stepId").value("step_assessment_safety"))
                .andExpect(jsonPath("$.data.steps[2].stepId").value("step_assessment_grading"))
                .andExpect(jsonPath("$.data.steps[3].stepId").value("step_assessment_feedback"))
                .andExpect(jsonPath("$.data.steps[4].stepId").value("step_assessment_mastery"))
                .andExpect(jsonPath("$.data.steps[5].stepId").value("step_assessment_replan"))
                .andExpect(jsonPath("$.data.recentFailedStep").doesNotExist())
                .andExpect(jsonPath("$.data.traceSummary.traceId").value("trc_orchestrator_answer"))
                .andExpect(jsonPath("$.data.traceSummary.totalSteps").value(6))
                .andExpect(jsonPath("$.data.traceSummary.failedSteps").value(0))
                .andExpect(jsonPath("$.data.traceSummary.lastStepId").value("step_assessment_replan"))
                .andExpect(jsonPath("$.data.traceSummary.lastStatus").value(AgentRuntimeConstants.STATUS_DONE))
                .andExpect(jsonPath("$.data.nextActions[0]").value("VIEW_RESULT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(body).path("data");
        String workflowId = data.path("workflowId").asText();
        String agentTaskId = data.path("agentTaskId").asText();

        assertThat(agentTaskRepository.findById(agentTaskId))
                .hasValueSatisfying(task -> {
                    assertThat(task.getOwnerUserId()).isEqualTo("alice");
                    assertThat(task.getTaskType()).isEqualTo("ANSWER_SUBMISSION");
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_DONE);
                    assertThat(task.getTraceId()).isEqualTo("trc_orchestrator_answer");
                    assertThat(task.getInputJson())
                            .contains(workflowId, "ANSWER_SUBMISSION", "req_orchestrated_answer_once", "q_sql_join");
                    JsonNode inputEnvelope = readJson(task.getInputJson());
                    assertThat(inputEnvelope.path("payload").path("answerLength").asInt())
                            .isEqualTo("JOIN duplicates happen when a one-to-many relation is joined.".length());
                    assertThat(task.getInputJson()).doesNotContain("JOIN duplicates happen when a one-to-many relation is joined.");
                });
        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(agentTaskId))
                .hasSize(6)
                .allSatisfy(trace -> assertThat(trace.getTraceId()).isEqualTo("trc_orchestrator_answer"))
                .extracting("sequenceNo")
                .containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(answerRecordRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(answerRecordRepository.findAll().getFirst().getTraceId()).isEqualTo("trc_orchestrator_answer");
        assertThat(gradingResultRepository.findAll().getFirst().getTraceId()).isEqualTo("trc_orchestrator_answer");
        assertThat(masteryRecordRepository.findAll().getFirst().getTraceId()).isEqualTo("trc_orchestrator_answer");
        assertThat(wrongQuestionRepository.findAll().getFirst().getTraceId()).isEqualTo("trc_orchestrator_answer");
        assertThat(learningEventRepository.findAll().getFirst().getTraceId()).isEqualTo("trc_orchestrator_answer");
    }

    @Test
    void returnsWorkflowStatusContextAfterAnswerSubmission() throws Exception {
        String body = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_answer_get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "ANSWER_SUBMISSION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"questionId\\":\\"q_sql_join\\",\\"answer\\":\\"JOIN duplicates happen when a one-to-many relation is joined.\\"}",
                                  "requestId": "req_orchestrated_answer_get"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(body).path("data");
        String workflowId = created.path("workflowId").asText();
        String agentTaskId = created.path("agentTaskId").asText();

        mockMvc.perform(get("/api/orchestrator/workflows/{workflowId}", workflowId)
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_answer_get_query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.workflowId").value(workflowId))
                .andExpect(jsonPath("$.data.workflowType").value("ANSWER_SUBMISSION"))
                .andExpect(jsonPath("$.data.agentTaskId").value(agentTaskId))
                .andExpect(jsonPath("$.data.traceId").value("trc_orchestrator_answer_get"))
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_DONE))
                .andExpect(jsonPath("$.data.steps.length()").value(6))
                .andExpect(jsonPath("$.data.steps[0].stepId").value("workflow_start"))
                .andExpect(jsonPath("$.data.steps[5].stepId").value("step_assessment_replan"))
                .andExpect(jsonPath("$.data.recentFailedStep").doesNotExist())
                .andExpect(jsonPath("$.data.traceSummary.agentTaskId").value(agentTaskId))
                .andExpect(jsonPath("$.data.traceSummary.traceId").value("trc_orchestrator_answer_get"))
                .andExpect(jsonPath("$.data.traceSummary.totalSteps").value(6))
                .andExpect(jsonPath("$.data.traceSummary.failedSteps").value(0))
                .andExpect(jsonPath("$.data.traceSummary.lastStepId").value("step_assessment_replan"))
                .andExpect(jsonPath("$.data.traceSummary.lastStatus").value(AgentRuntimeConstants.STATUS_DONE))
                .andExpect(jsonPath("$.data.nextActions[0]").value("VIEW_RESULT"));
    }

    @Test
    void replaysAnswerSubmissionWorkflowWithSameRequestIdWithoutDuplicatingRows() throws Exception {
        String requestBody = """
                {
                  "workflowType": "ANSWER_SUBMISSION",
                  "learnerId": "alice",
                  "payloadJson": "{\\"questionId\\":\\"q_sql_join\\",\\"answer\\":\\"JOIN duplicates happen when a one-to-many relation is joined.\\"}",
                  "requestId": "req_orchestrated_answer_replay"
                }
                """;

        String firstBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_answer_replay_first")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_answer_replay_second")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode first = objectMapper.readTree(firstBody).path("data");
        JsonNode second = objectMapper.readTree(secondBody).path("data");
        assertThat(second.path("workflowId").asText()).isEqualTo(first.path("workflowId").asText());
        assertThat(second.path("agentTaskId").asText()).isEqualTo(first.path("agentTaskId").asText());
        assertThat(second.path("traceId").asText()).isEqualTo(first.path("traceId").asText());
        assertThat(second.path("status").asText()).isEqualTo(AgentRuntimeConstants.STATUS_DONE);

        assertThat(agentTaskRepository.count()).isEqualTo(1);
        assertThat(answerRecordRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(gradingResultRepository.count()).isEqualTo(1);
        assertThat(masteryRecordRepository.count()).isEqualTo(1);
        assertThat(wrongQuestionRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(learningEventRepository.countByLearnerId("alice")).isEqualTo(1);
    }

    @Test
    void answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow() throws Exception {
        String courseId = "course_orch_answer_replay_scope";
        String questionId = seedCourseKnowledgeAndEnrollment(
                courseId,
                "teacher_a",
                "alice",
                "ACTIVE",
                "Replay scope knowledge"
        );
        String answer = "Replay scope answer should stay private.";
        String requestBody = """
                {
                  "workflowType": "ANSWER_SUBMISSION",
                  "learnerId": "alice",
                  "payloadJson": "{\\"questionId\\":\\"%s\\",\\"answer\\":\\"%s\\"}",
                  "requestId": "req_orch_answer_replay_scope"
                }
                """.formatted(questionId, answer);

        String firstBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "admin")
                        .header("X-Trace-Id", "trc_orch_answer_replay_scope_first")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode first = objectMapper.readTree(firstBody).path("data");
        String workflowId = first.path("workflowId").asText();
        String agentTaskId = first.path("agentTaskId").asText();
        String traceId = first.path("traceId").asText();
        long answerCount = answerRecordRepository.count();
        long gradingCount = gradingResultRepository.count();
        long masteryCount = masteryRecordRepository.count();
        long wrongQuestionCount = wrongQuestionRepository.count();
        long learningEventCount = learningEventRepository.count();

        CourseEnrollment enrollment = courseEnrollmentRepository
                .findByCourseIdAndLearnerId(courseId, "alice")
                .orElseThrow();
        enrollment.setStatus("DROPPED");
        courseEnrollmentRepository.saveAndFlush(enrollment);

        String rejectedBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "admin")
                        .header("X-Trace-Id", "trc_orch_answer_replay_scope_second")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(rejectedBody)
                .doesNotContain(workflowId, agentTaskId, traceId, questionId, courseId,
                        "req_orch_answer_replay_scope", answer);
        assertThat(answerRecordRepository.count()).isEqualTo(answerCount);
        assertThat(gradingResultRepository.count()).isEqualTo(gradingCount);
        assertThat(masteryRecordRepository.count()).isEqualTo(masteryCount);
        assertThat(wrongQuestionRepository.count()).isEqualTo(wrongQuestionCount);
        assertThat(learningEventRepository.count()).isEqualTo(learningEventCount);
    }

    @Test
    void replaysAnswerSubmissionWorkflowByExactEnvelopeInsteadOfFirstRequestIdMarker() throws Exception {
        seedAgentTask(
                "agt_answer_replay_wrong_candidate",
                "alice",
                "ANSWER_SUBMISSION",
                AgentRuntimeConstants.STATUS_DONE,
                "trc_wrong_candidate",
                "{\"workflowId\":\"wf_wrong_candidate\",\"workflowType\":\"ANSWER_SUBMISSION\","
                        + "\"ownerUserId\":\"alice\",\"learnerId\":\"alice\","
                        + "\"requestId\":\"req_orchestrated_answer_exact_replay\","
                        + "\"payload\":{\"questionId\":\"q_other\",\"answerLength\":999}}"
        );

        String requestBody = """
                {
                  "workflowType": "ANSWER_SUBMISSION",
                  "learnerId": "alice",
                  "payloadJson": "{\\"questionId\\":\\"q_sql_join\\",\\"answer\\":\\"JOIN duplicates happen when a one-to-many relation is joined.\\"}",
                  "requestId": "req_orchestrated_answer_exact_replay"
                }
                """;

        String firstBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_answer_exact_replay_first")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_answer_exact_replay_second")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode first = objectMapper.readTree(firstBody).path("data");
        JsonNode second = objectMapper.readTree(secondBody).path("data");
        assertThat(second.path("workflowId").asText()).isEqualTo(first.path("workflowId").asText());
        assertThat(second.path("agentTaskId").asText()).isEqualTo(first.path("agentTaskId").asText());
        assertThat(second.path("traceId").asText()).isEqualTo("trc_answer_exact_replay_first");
        assertThat(second.path("agentTaskId").asText()).isNotEqualTo("agt_answer_replay_wrong_candidate");
        assertThat(agentTaskRepository.count()).isEqualTo(2);
        assertThat(answerRecordRepository.countByLearnerId("alice")).isEqualTo(1);
    }

    @Test
    void concurrentAnswerWorkflowTraceDriftReturnsWinnerWorkflowWithoutPersistingSecondTask() throws Exception {
        String requestBody = """
                {
                  "workflowType": "ANSWER_SUBMISSION",
                  "learnerId": "alice",
                  "payloadJson": "{\\"questionId\\":\\"q_sql_join\\",\\"answer\\":\\"JOIN duplicates happen when a one-to-many relation is joined.\\"}",
                  "requestId": "req_orchestrated_answer_trace_drift"
                }
                """;

        String firstBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_answer_trace_winner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode first = objectMapper.readTree(firstBody).path("data");
        doReturn(Optional.empty())
                .when(assessmentService)
                .replayAnswerIfPresent(eq("alice"), any(AnswerSubmitRequest.class));

        String secondBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_answer_trace_loser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode second = objectMapper.readTree(secondBody).path("data");
        assertThat(second.path("workflowId").asText()).isEqualTo(first.path("workflowId").asText());
        assertThat(second.path("agentTaskId").asText()).isEqualTo(first.path("agentTaskId").asText());
        assertThat(second.path("traceId").asText()).isEqualTo("trc_answer_trace_winner");
        assertThat(agentTaskRepository.count()).isEqualTo(1);
        assertThat(agentTaskRepository.findAll().getFirst().getTraceId()).isEqualTo("trc_answer_trace_winner");
        assertThat(answerRecordRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(gradingResultRepository.count()).isEqualTo(1);
        assertThat(masteryRecordRepository.count()).isEqualTo(1);
        assertThat(wrongQuestionRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(learningEventRepository.countByLearnerId("alice")).isEqualTo(1);
    }

    @Test
    void rejectsAnswerSubmissionWorkflowPayloadConflictWithoutNewRows() throws Exception {
        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_answer_conflict_first")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "ANSWER_SUBMISSION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"questionId\\":\\"q_sql_join\\",\\"answer\\":\\"JOIN duplicates happen when a one-to-many relation is joined.\\"}",
                                  "requestId": "req_orchestrated_answer_conflict"
                                }
                                """))
                .andExpect(status().isOk());

        long taskCount = agentTaskRepository.count();

        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_answer_conflict_second")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "ANSWER_SUBMISSION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"questionId\\":\\"q_sql_join\\",\\"answer\\":\\"A different answer tries to reuse the same request id.\\"}",
                                  "requestId": "req_orchestrated_answer_conflict"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertThat(agentTaskRepository.count()).isEqualTo(taskCount);
        assertThat(answerRecordRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(gradingResultRepository.count()).isEqualTo(1);
        assertThat(masteryRecordRepository.count()).isEqualTo(1);
        assertThat(wrongQuestionRepository.countByLearnerId("alice")).isEqualTo(1);
        assertThat(learningEventRepository.countByLearnerId("alice")).isEqualTo(1);
    }

    @Test
    void rejectsInvalidAnswerSubmissionPayloadBeforeCreatingWorkflowTask() throws Exception {
        long taskCount = agentTaskRepository.count();

        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_invalid_answer_payload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "ANSWER_SUBMISSION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"questionId\\":\\"q_sql_join\\",\\"answer\\":\\"   \\"}",
                                  "requestId": "req_invalid_answer_payload"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(agentTaskRepository.count()).isEqualTo(taskCount);
        assertThat(answerRecordRepository.countByLearnerId("alice")).isZero();
        assertThat(gradingResultRepository.count()).isZero();
        assertThat(masteryRecordRepository.count()).isZero();
        assertThat(wrongQuestionRepository.countByLearnerId("alice")).isZero();
        assertThat(learningEventRepository.countByLearnerId("alice")).isZero();
    }

    @Test
    void createsRagQaWorkflowAndReusesWorkflowTraceContext() throws Exception {
        seedIndexedChunk("kb_sql", "doc_sql", "SQL JOIN duplicates usually come from one-to-many relationships.");

        String body = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_rag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RAG_QA",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"kbIds\\":[\\"kb_sql\\"],\\"question\\":\\"Why does SQL JOIN duplicate rows?\\",\\"topK\\":5}",
                                  "requestId": "req_orchestrated_rag_1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.workflowId").isNotEmpty())
                .andExpect(jsonPath("$.data.workflowType").value("RAG_QA"))
                .andExpect(jsonPath("$.data.agentTaskId").isNotEmpty())
                .andExpect(jsonPath("$.data.traceId").value("trc_orchestrator_rag"))
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_DONE))
                .andExpect(jsonPath("$.data.steps.length()").value(4))
                .andExpect(jsonPath("$.data.steps[0].stepId").value("workflow_start"))
                .andExpect(jsonPath("$.data.steps[0].agentName").value("Orchestrator"))
                .andExpect(jsonPath("$.data.steps[0].sequenceNo").value(1))
                .andExpect(jsonPath("$.data.steps[1].stepId").value("step_rag_safety"))
                .andExpect(jsonPath("$.data.steps[1].sequenceNo").value(2))
                .andExpect(jsonPath("$.data.steps[2].stepId").value("step_rag_retrieval"))
                .andExpect(jsonPath("$.data.steps[2].sequenceNo").value(3))
                .andExpect(jsonPath("$.data.steps[3].stepId").value("step_rag_answer"))
                .andExpect(jsonPath("$.data.steps[3].sequenceNo").value(4))
                .andExpect(jsonPath("$.data.recentFailedStep").doesNotExist())
                .andExpect(jsonPath("$.data.traceSummary.traceId").value("trc_orchestrator_rag"))
                .andExpect(jsonPath("$.data.traceSummary.totalSteps").value(4))
                .andExpect(jsonPath("$.data.traceSummary.failedSteps").value(0))
                .andExpect(jsonPath("$.data.traceSummary.lastStepId").value("step_rag_answer"))
                .andExpect(jsonPath("$.data.traceSummary.lastStatus").value(AgentRuntimeConstants.STATUS_DONE))
                .andExpect(jsonPath("$.data.nextActions[0]").value("VIEW_RESULT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(body).path("data");
        String workflowId = data.path("workflowId").asText();
        String agentTaskId = data.path("agentTaskId").asText();

        assertThat(agentTaskRepository.findById(agentTaskId))
                .hasValueSatisfying(task -> {
                    assertThat(task.getOwnerUserId()).isEqualTo("alice");
                    assertThat(task.getTaskType()).isEqualTo("RAG_QA");
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_DONE);
                    assertThat(task.getTraceId()).isEqualTo("trc_orchestrator_rag");
                    assertThat(task.getInputJson())
                            .contains(workflowId, "RAG_QA", "req_orchestrated_rag_1", "questionHash", "kbIdsHash")
                            .doesNotContain("Why does SQL JOIN duplicate rows?");
                });
        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(agentTaskId))
                .hasSize(4)
                .allSatisfy(trace -> assertThat(trace.getTraceId()).isEqualTo("trc_orchestrator_rag"))
                .extracting("sequenceNo")
                .containsExactly(1, 2, 3, 4);
        assertThat(queryLogRepository.count()).isEqualTo(1);
        var logFields = new org.springframework.beans.DirectFieldAccessor(queryLogRepository.findAll().getFirst());
        assertThat(logFields.getPropertyValue("traceId")).isEqualTo("trc_orchestrator_rag");
        assertThat(logFields.getPropertyValue("rerankerStatus")).isEqualTo("NOT_CONFIGURED");
        assertThat(sourceCitationRepository.count()).isEqualTo(1);
        assertThat(sourceCitationRepository.findAll().getFirst().getTraceId()).isEqualTo("trc_orchestrator_rag");
    }

    @Test
    void replaysRagQaWorkflowWithSameRequestIdWithoutDuplicatingRows() throws Exception {
        seedIndexedChunk("kb_sql", "doc_sql", "SQL JOIN duplicates usually come from one-to-many relationships.");
        String requestBody = """
                {
                  "workflowType": "RAG_QA",
                  "learnerId": "alice",
                  "payloadJson": "{\\"kbIds\\":[\\"kb_sql\\"],\\"question\\":\\"Why does SQL JOIN duplicate rows?\\",\\"topK\\":5}",
                  "requestId": "req_orchestrated_rag_replay"
                }
                """;

        String firstBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_rag_replay_first")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_rag_replay_second")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode first = objectMapper.readTree(firstBody).path("data");
        JsonNode second = objectMapper.readTree(secondBody).path("data");
        assertThat(second.path("workflowId").asText()).isEqualTo(first.path("workflowId").asText());
        assertThat(second.path("agentTaskId").asText()).isEqualTo(first.path("agentTaskId").asText());
        assertThat(second.path("traceId").asText()).isEqualTo("trc_orchestrator_rag_replay_first");
        assertThat(second.path("status").asText()).isEqualTo(AgentRuntimeConstants.STATUS_DONE);

        assertThat(agentTaskRepository.count()).isEqualTo(1);
        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(first.path("agentTaskId").asText())).hasSize(4);
        assertThat(queryLogRepository.count()).isEqualTo(1);
        assertThat(sourceCitationRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsRagQaWorkflowWhenSameRequestIdPayloadDiffersBeforeCreatingWorkflowTask() throws Exception {
        seedIndexedChunk("kb_sql", "doc_sql", "SQL JOIN duplicates usually come from one-to-many relationships.");

        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_rag_conflict_first")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RAG_QA",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"kbIds\\":[\\"kb_sql\\"],\\"question\\":\\"Why does SQL JOIN duplicate rows?\\",\\"topK\\":5}",
                                  "requestId": "req_orchestrated_rag_conflict"
                                }
                                """))
                .andExpect(status().isOk());

        long taskCount = agentTaskRepository.count();
        long traceCount = agentTraceRepository.count();
        long queryLogCount = queryLogRepository.count();
        long citationCount = sourceCitationRepository.count();

        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_rag_conflict_second")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RAG_QA",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"kbIds\\":[\\"kb_sql\\"],\\"question\\":\\"How should I diagnose SQL JOIN mistakes?\\",\\"topK\\":5}",
                                  "requestId": "req_orchestrated_rag_conflict"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertThat(agentTaskRepository.count()).isEqualTo(taskCount);
        assertThat(agentTraceRepository.count()).isEqualTo(traceCount);
        assertThat(queryLogRepository.count()).isEqualTo(queryLogCount);
        assertThat(sourceCitationRepository.count()).isEqualTo(citationCount);
    }

    @Test
    void replaysRagQaWorkflowByExactEnvelopeInsteadOfFirstRequestIdMarker() throws Exception {
        seedAgentTask(
                "agt_rag_replay_wrong_candidate",
                "alice",
                "RAG_QA",
                AgentRuntimeConstants.STATUS_DONE,
                "trc_wrong_rag_candidate",
                "{\"workflowId\":\"wf_wrong_rag_candidate\",\"workflowType\":\"RAG_QA\","
                        + "\"ownerUserId\":\"alice\",\"learnerId\":\"alice\","
                        + "\"requestId\":\"req_orchestrated_rag_exact_replay\","
                        + "\"payload\":{\"questionHash\":\"wrong\",\"questionLength\":999,\"kbIdsHash\":\"wrong\",\"kbCount\":1,\"topK\":5}}"
        );
        seedIndexedChunk("kb_sql", "doc_sql", "SQL JOIN duplicates usually come from one-to-many relationships.");
        String requestBody = """
                {
                  "workflowType": "RAG_QA",
                  "learnerId": "alice",
                  "payloadJson": "{\\"kbIds\\":[\\"kb_sql\\"],\\"question\\":\\"Why does SQL JOIN duplicate rows?\\",\\"topK\\":5}",
                  "requestId": "req_orchestrated_rag_exact_replay"
                }
                """;

        String firstBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_rag_exact_replay_first")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_rag_exact_replay_second")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode first = objectMapper.readTree(firstBody).path("data");
        JsonNode second = objectMapper.readTree(secondBody).path("data");
        assertThat(second.path("workflowId").asText()).isEqualTo(first.path("workflowId").asText());
        assertThat(second.path("agentTaskId").asText()).isEqualTo(first.path("agentTaskId").asText());
        assertThat(second.path("agentTaskId").asText()).isNotEqualTo("agt_rag_replay_wrong_candidate");
        assertThat(second.path("traceId").asText()).isEqualTo("trc_rag_exact_replay_first");
        assertThat(agentTaskRepository.count()).isEqualTo(2);
        assertThat(queryLogRepository.count()).isEqualTo(1);
        assertThat(sourceCitationRepository.count()).isEqualTo(1);
    }

    @Test
    void returnsWorkflowStatusContextAfterRagQa() throws Exception {
        seedIndexedChunk("kb_sql", "doc_sql", "SQL JOIN duplicates usually come from one-to-many relationships.");

        String body = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_rag_get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RAG_QA",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"kbIds\\":[\\"kb_sql\\"],\\"question\\":\\"Why does SQL JOIN duplicate rows?\\",\\"topK\\":5}",
                                  "requestId": "req_orchestrated_rag_get"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(body).path("data");
        String workflowId = created.path("workflowId").asText();
        String agentTaskId = created.path("agentTaskId").asText();

        mockMvc.perform(get("/api/orchestrator/workflows/{workflowId}", workflowId)
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_rag_get_query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.workflowId").value(workflowId))
                .andExpect(jsonPath("$.data.workflowType").value("RAG_QA"))
                .andExpect(jsonPath("$.data.agentTaskId").value(agentTaskId))
                .andExpect(jsonPath("$.data.traceId").value("trc_orchestrator_rag_get"))
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_DONE))
                .andExpect(jsonPath("$.data.steps.length()").value(4))
                .andExpect(jsonPath("$.data.steps[0].stepId").value("workflow_start"))
                .andExpect(jsonPath("$.data.steps[1].stepId").value("step_rag_safety"))
                .andExpect(jsonPath("$.data.steps[2].stepId").value("step_rag_retrieval"))
                .andExpect(jsonPath("$.data.steps[3].stepId").value("step_rag_answer"))
                .andExpect(jsonPath("$.data.recentFailedStep").doesNotExist())
                .andExpect(jsonPath("$.data.traceSummary.agentTaskId").value(agentTaskId))
                .andExpect(jsonPath("$.data.traceSummary.traceId").value("trc_orchestrator_rag_get"))
                .andExpect(jsonPath("$.data.traceSummary.totalSteps").value(4))
                .andExpect(jsonPath("$.data.traceSummary.failedSteps").value(0))
                .andExpect(jsonPath("$.data.traceSummary.lastStepId").value("step_rag_answer"))
                .andExpect(jsonPath("$.data.traceSummary.lastStatus").value(AgentRuntimeConstants.STATUS_DONE))
                .andExpect(jsonPath("$.data.nextActions[0]").value("VIEW_RESULT"));
    }

    @Test
    void createsRagQaWorkflowWithNoSourceAndNoCitations() throws Exception {
        seedPrivateKnowledgeBase("kb_empty", "alice");

        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_rag_no_source")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RAG_QA",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"kbIds\\":[\\"kb_empty\\"],\\"question\\":\\"What does this course say about graphs?\\",\\"topK\\":5}",
                                  "requestId": "req_orchestrated_rag_no_source"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.workflowType").value("RAG_QA"))
                .andExpect(jsonPath("$.data.traceId").value("trc_orchestrator_rag_no_source"))
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_DONE))
                .andExpect(jsonPath("$.data.steps.length()").value(4))
                .andExpect(jsonPath("$.data.steps[2].summary").value(org.hamcrest.Matchers.containsString("NO_SOURCE_REFUSAL")))
                .andExpect(jsonPath("$.data.nextActions[0]").value("VIEW_RESULT"));

        assertThat(queryLogRepository.count()).isEqualTo(1);
        var logFields = new org.springframework.beans.DirectFieldAccessor(queryLogRepository.findAll().getFirst());
        assertThat(logFields.getPropertyValue("traceId")).isEqualTo("trc_orchestrator_rag_no_source");
        assertThat(logFields.getPropertyValue("rerankerStatus")).isEqualTo("SKIPPED_NO_CANDIDATES");
        assertThat(sourceCitationRepository.count()).isZero();
    }

    @Test
    void ragQaWorkflowUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        seedPrivateKnowledgeBase("kb_private_bob_admin_runtime", "bob");

        String body = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_rag_admin_role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RAG_QA",
                                  "learnerId": "ops_admin",
                                  "payloadJson": "{\\"kbIds\\":[\\"kb_private_bob_admin_runtime\\"],\\"question\\":\\"What does Bob private KB contain?\\",\\"topK\\":5}",
                                  "requestId": "req_orchestrator_rag_admin_role"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.workflowType").value("RAG_QA"))
                .andExpect(jsonPath("$.data.traceId").value("trc_orchestrator_rag_admin_role"))
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_DONE))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String agentTaskId = objectMapper.readTree(body).path("data").path("agentTaskId").asText();
        assertThat(agentTaskRepository.findById(agentTaskId))
                .hasValueSatisfying(task -> assertThat(task.getOwnerUserId()).isEqualTo("ops_admin"));
        assertThat(queryLogRepository.count()).isEqualTo(1);
        var logFields = new org.springframework.beans.DirectFieldAccessor(queryLogRepository.findAll().getFirst());
        assertThat(logFields.getPropertyValue("userId")).isEqualTo("ops_admin");
        assertThat(logFields.getPropertyValue("traceId")).isEqualTo("trc_orchestrator_rag_admin_role");
        assertThat(logFields.getPropertyValue("rerankerStatus")).isEqualTo("SKIPPED_NO_CANDIDATES");
        assertThat(sourceCitationRepository.count()).isZero();
    }

    @Test
    void ragQaWorkflowRejectsBearerUserSubjectAdminRoleConfusionBeforeQueryArtifacts() throws Exception {
        seedPrivateKnowledgeBase("kb_private_bob_subject_admin_runtime", "bob");

        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER")))
                        .header("X-User-Id", "bob")
                        .header("X-Trace-Id", "trc_orchestrator_rag_subject_admin_forbidden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RAG_QA",
                                  "learnerId": "admin",
                                  "payloadJson": "{\\"kbIds\\":[\\"kb_private_bob_subject_admin_runtime\\"],\\"question\\":\\"What does Bob private KB contain?\\",\\"topK\\":5}",
                                  "requestId": "req_orchestrator_rag_subject_admin_forbidden"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(queryLogRepository.count()).isZero();
        assertThat(sourceCitationRepository.count()).isZero();

        AgentTask task = agentTaskRepository
                .findByOwnerUserIdAndTaskTypeAndInputJsonContainingOrderByCreatedAtAsc(
                        "admin",
                        "RAG_QA",
                        "req_orchestrator_rag_subject_admin_forbidden"
                )
                .getFirst();
        assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
        assertThat(task.getTraceId()).isEqualTo("trc_orchestrator_rag_subject_admin_forbidden");
        assertThat(task.getOutputJson())
                .contains("FORBIDDEN")
                .doesNotContain("What does Bob private KB contain?");
        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(task.getId()))
                .hasSize(2)
                .satisfiesExactly(
                        trace -> {
                            assertThat(trace.getStepId()).isEqualTo("workflow_start");
                            assertThat(trace.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_RUNNING);
                        },
                        trace -> {
                            assertThat(trace.getStepId()).isEqualTo("step_runtime_failure");
                            assertThat(trace.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                            assertThat(trace.getSummary()).contains("RAG_QA", "FORBIDDEN");
                            assertThat(trace.getSummary()).doesNotContain("What does Bob private KB contain?");
                        }
                );
    }

    @Test
    void rejectsInvalidRagQaPayloadBeforeCreatingWorkflowTask() throws Exception {
        long taskCount = agentTaskRepository.count();

        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_invalid_rag_payload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RAG_QA",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"kbIds\\":[\\"  \\"],\\"question\\":\\"   \\"}",
                                  "requestId": "req_invalid_rag_payload"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(agentTaskRepository.count()).isEqualTo(taskCount);
        assertThat(queryLogRepository.count()).isZero();
        assertThat(sourceCitationRepository.count()).isZero();
    }

    @Test
    void rejectsMissingRagQaRequestIdBeforeCreatingWorkflowTask() throws Exception {
        long taskCount = agentTaskRepository.count();

        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_missing_rag_request_id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RAG_QA",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"kbIds\\":[\\"kb_sql\\"],\\"question\\":\\"Why does SQL JOIN duplicate rows?\\",\\"topK\\":5}"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(agentTaskRepository.count()).isEqualTo(taskCount);
        assertThat(queryLogRepository.count()).isZero();
        assertThat(sourceCitationRepository.count()).isZero();
    }

    @Test
    void persistsRagQaRuntimeFailureEvidenceWithoutWritingQueryArtifacts() throws Exception {
        seedPrivateKnowledgeBase("kb_private_bob", "bob");

        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_rag_forbidden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RAG_QA",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"kbIds\\":[\\"kb_private_bob\\"],\\"question\\":\\"Why does SQL JOIN duplicate rows?\\",\\"topK\\":5}",
                                  "requestId": "req_orchestrated_rag_forbidden"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(queryLogRepository.count()).isZero();
        assertThat(sourceCitationRepository.count()).isZero();

        AgentTask task = agentTaskRepository
                .findByOwnerUserIdAndTaskTypeAndInputJsonContainingOrderByCreatedAtAsc(
                        "alice",
                        "RAG_QA",
                        "req_orchestrated_rag_forbidden"
                )
                .getFirst();
        assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
        assertThat(task.getTraceId()).isEqualTo("trc_orchestrator_rag_forbidden");
        assertThat(task.getOutputJson())
                .contains("\"status\":\"FAILED\"", "FORBIDDEN")
                .doesNotContain("Why does SQL JOIN duplicate rows?");

        String workflowId = readJson(task.getInputJson()).path("workflowId").asText();
        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(task.getId()))
                .hasSize(2)
                .satisfiesExactly(
                        trace -> {
                            assertThat(trace.getStepId()).isEqualTo("workflow_start");
                            assertThat(trace.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_RUNNING);
                            assertThat(trace.getSequenceNo()).isEqualTo(1);
                        },
                        trace -> {
                            assertThat(trace.getStepId()).isEqualTo("step_runtime_failure");
                            assertThat(trace.getAgentName()).isEqualTo("Orchestrator");
                            assertThat(trace.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                            assertThat(trace.getSummary()).contains("RAG_QA", "FORBIDDEN");
                            assertThat(trace.getSummary()).doesNotContain("Why does SQL JOIN duplicate rows?");
                            assertThat(trace.getSequenceNo()).isEqualTo(2);
                        }
                );

        mockMvc.perform(get("/api/orchestrator/workflows/{workflowId}", workflowId)
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_rag_forbidden_query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowId").value(workflowId))
                .andExpect(jsonPath("$.data.workflowType").value("RAG_QA"))
                .andExpect(jsonPath("$.data.agentTaskId").value(task.getId()))
                .andExpect(jsonPath("$.data.traceId").value("trc_orchestrator_rag_forbidden"))
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_FAILED))
                .andExpect(jsonPath("$.data.steps.length()").value(2))
                .andExpect(jsonPath("$.data.recentFailedStep.stepId").value("step_runtime_failure"))
                .andExpect(jsonPath("$.data.recentFailedStep.inputDto").value("RagQaWorkflowRequest"))
                .andExpect(jsonPath("$.data.recentFailedStep.outputDto").value("WorkflowFailureEvidence"))
                .andExpect(jsonPath("$.data.recentFailedStep.failurePolicy").value("SANITIZE_AND_PERSIST_FAILED_TRACE"))
                .andExpect(jsonPath("$.data.recentFailedStep.retryPolicy").value("RESUBMIT_ORIGINAL_REQUEST"))
                .andExpect(jsonPath("$.data.recentFailedStep.retryable").value(false))
                .andExpect(jsonPath("$.data.traceSummary.failedSteps").value(1))
                .andExpect(jsonPath("$.data.traceSummary.lastStatus").value(AgentRuntimeConstants.STATUS_FAILED))
                .andExpect(jsonPath("$.data.nextActions[0]").value("INSPECT_TRACE"))
                .andExpect(jsonPath("$.data.nextActions[1]").value("RESUBMIT_ORIGINAL_REQUEST"));
    }

    @Test
    void persistsGenericRuntimeFailureEvidenceWithoutLeakingSensitiveInput() throws Exception {
        doThrow(new IllegalStateException("assessment engine unavailable with Sensitive learner answer"))
                .when(assessmentService)
                .submitAnswerWithTraceId(
                        eq("alice"),
                        any(AnswerSubmitRequest.class),
                        eq("trc_orchestrator_answer_runtime_failure")
                );

        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_answer_runtime_failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "ANSWER_SUBMISSION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"questionId\\":\\"q_sql_join\\",\\"answer\\":\\"Sensitive learner answer should stay out of trace.\\"}",
                                  "requestId": "req_answer_runtime_failure"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        AgentTask task = agentTaskRepository
                .findByOwnerUserIdAndTaskTypeAndInputJsonContainingOrderByCreatedAtAsc(
                        "alice",
                        "ANSWER_SUBMISSION",
                        "req_answer_runtime_failure"
                )
                .getFirst();
        assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
        assertThat(task.getTraceId()).isEqualTo("trc_orchestrator_answer_runtime_failure");
        assertThat(task.getOutputJson())
                .contains("\"status\":\"FAILED\"", "INTERNAL_ERROR", "\"recoverable\":true")
                .doesNotContain("Sensitive learner answer", "assessment engine unavailable");
        assertThat(task.getInputJson()).doesNotContain("Sensitive learner answer should stay out of trace.");

        String workflowId = readJson(task.getInputJson()).path("workflowId").asText();
        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(task.getId()))
                .hasSize(2)
                .satisfiesExactly(
                        trace -> {
                            assertThat(trace.getStepId()).isEqualTo("workflow_start");
                            assertThat(trace.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_RUNNING);
                            assertThat(trace.getSequenceNo()).isEqualTo(1);
                        },
                        trace -> {
                            assertThat(trace.getStepId()).isEqualTo("step_runtime_failure");
                            assertThat(trace.getAgentName()).isEqualTo("Orchestrator");
                            assertThat(trace.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                            assertThat(trace.getSummary()).contains("ANSWER_SUBMISSION", "INTERNAL_ERROR");
                            assertThat(trace.getSummary())
                                    .doesNotContain("Sensitive learner answer", "assessment engine unavailable");
                            assertThat(trace.getSequenceNo()).isEqualTo(2);
                        }
                );

        mockMvc.perform(get("/api/orchestrator/workflows/{workflowId}", workflowId)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowId").value(workflowId))
                .andExpect(jsonPath("$.data.workflowType").value("ANSWER_SUBMISSION"))
                .andExpect(jsonPath("$.data.agentTaskId").value(task.getId()))
                .andExpect(jsonPath("$.data.traceId").value("trc_orchestrator_answer_runtime_failure"))
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_FAILED))
                .andExpect(jsonPath("$.data.recentFailedStep.stepId").value("step_runtime_failure"))
                .andExpect(jsonPath("$.data.recentFailedStep.summary").value("Workflow ANSWER_SUBMISSION failed with INTERNAL_ERROR. Error: INTERNAL_ERROR"))
                .andExpect(jsonPath("$.data.recentFailedStep.inputDto").value("AnswerSubmitRequest"))
                .andExpect(jsonPath("$.data.recentFailedStep.outputDto").value("WorkflowFailureEvidence"))
                .andExpect(jsonPath("$.data.recentFailedStep.failurePolicy").value("SANITIZE_AND_PERSIST_FAILED_TRACE"))
                .andExpect(jsonPath("$.data.recentFailedStep.retryPolicy").value("RESUBMIT_ORIGINAL_REQUEST"))
                .andExpect(jsonPath("$.data.recentFailedStep.retryable").value(false))
                .andExpect(jsonPath("$.data.traceSummary.failedSteps").value(1))
                .andExpect(jsonPath("$.data.nextActions[1]").value("RESUBMIT_ORIGINAL_REQUEST"));
    }

    @Test
    void returnsExplicitNodeContractsForResourceGenerationWorkflow() throws Exception {
        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_resource_node_contract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RESOURCE_GENERATION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"goalId\\":\\"goal_spring_boot\\",\\"pathNodeId\\":\\"node_sql_join\\",\\"resourceTypes\\":[\\"LECTURE\\"]}",
                                  "requestId": "req_resource_node_contract"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowType").value("RESOURCE_GENERATION"))
                .andExpect(jsonPath("$.data.steps[0].stepId").value("workflow_start"))
                .andExpect(jsonPath("$.data.steps[0].inputDto").value("CreateWorkflowRequest"))
                .andExpect(jsonPath("$.data.steps[0].outputDto").value("OrchestratorWorkflowResponse"))
                .andExpect(jsonPath("$.data.steps[0].failurePolicy").value("VALIDATE_BEFORE_TASK_OR_PERSIST_RUNTIME_FAILURE"))
                .andExpect(jsonPath("$.data.steps[0].retryPolicy").value("RETRY_WORKFLOW_RESOURCE_GENERATION_ONLY"))
                .andExpect(jsonPath("$.data.steps[0].retryable").value(true))
                .andExpect(jsonPath("$.data.steps[1].stepId").value("step_planner"))
                .andExpect(jsonPath("$.data.steps[1].inputDto").value("ResourceGenerationRequest"))
                .andExpect(jsonPath("$.data.steps[1].outputDto").value("ResourceGenerationResponse"))
                .andExpect(jsonPath("$.data.steps[1].failurePolicy").value("PERSIST_FAILED_TRACE"))
                .andExpect(jsonPath("$.data.steps[1].retryPolicy").value("RETRY_WORKFLOW_RESOURCE_GENERATION_ONLY"))
                .andExpect(jsonPath("$.data.steps[1].retryable").value(true));
    }

    @Test
    void createsResourceGenerationWorkflowAndReusesWorkflowAgentContext() throws Exception {
        String body = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_resource")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RESOURCE_GENERATION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"goalId\\":\\"goal_spring_boot\\",\\"pathNodeId\\":\\"node_sql_join\\",\\"resourceTypes\\":[\\"LECTURE\\",\\"EXERCISE\\"]}",
                                  "requestId": "req_orchestrated_resource_1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.workflowId").isNotEmpty())
                .andExpect(jsonPath("$.data.workflowType").value("RESOURCE_GENERATION"))
                .andExpect(jsonPath("$.data.agentTaskId").isNotEmpty())
                .andExpect(jsonPath("$.data.traceId").value("trc_orchestrator_resource"))
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_WAITING_REVIEW))
                .andExpect(jsonPath("$.data.steps.length()").value(8))
                .andExpect(jsonPath("$.data.steps[0].stepId").value("workflow_start"))
                .andExpect(jsonPath("$.data.steps[0].agentName").value("Orchestrator"))
                .andExpect(jsonPath("$.data.steps[0].status").value(AgentRuntimeConstants.STATUS_RUNNING))
                .andExpect(jsonPath("$.data.steps[0].sequenceNo").value(1))
                .andExpect(jsonPath("$.data.steps[1].stepId").value("step_planner"))
                .andExpect(jsonPath("$.data.steps[1].sequenceNo").value(2))
                .andExpect(jsonPath("$.data.steps[7].stepId").value("step_safety"))
                .andExpect(jsonPath("$.data.steps[7].sequenceNo").value(8))
                .andExpect(jsonPath("$.data.recentFailedStep").doesNotExist())
                .andExpect(jsonPath("$.data.traceSummary.traceId").value("trc_orchestrator_resource"))
                .andExpect(jsonPath("$.data.traceSummary.totalSteps").value(8))
                .andExpect(jsonPath("$.data.traceSummary.failedSteps").value(0))
                .andExpect(jsonPath("$.data.traceSummary.lastStepId").value("step_safety"))
                .andExpect(jsonPath("$.data.traceSummary.lastStatus").value(AgentRuntimeConstants.STATUS_DONE))
                .andExpect(jsonPath("$.data.nextActions[0]").value("OPEN_REVIEW_QUEUE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(body).path("data");
        String workflowId = data.path("workflowId").asText();
        String agentTaskId = data.path("agentTaskId").asText();
        String traceId = data.path("traceId").asText();

        assertThat(workflowId).startsWith("wf_");
        assertThat(agentTaskRepository.findById(agentTaskId))
                .hasValueSatisfying(task -> {
                    assertThat(task.getOwnerUserId()).isEqualTo("alice");
                    assertThat(task.getTaskType()).isEqualTo(AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION);
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_WAITING_REVIEW);
                    assertThat(task.getTraceId()).isEqualTo(traceId);
                    assertThat(task.getInputJson())
                            .contains(workflowId, "RESOURCE_GENERATION", "req_orchestrated_resource_1", "goal_spring_boot");
                });

        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(agentTaskId))
                .hasSize(8)
                .allSatisfy(trace -> assertThat(trace.getTraceId()).isEqualTo(traceId))
                .extracting("sequenceNo")
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(agentTaskId).getFirst())
                .satisfies(trace -> {
                    assertThat(trace.getStepId()).isEqualTo("workflow_start");
                    assertThat(trace.getAgentName()).isEqualTo("Orchestrator");
                    assertThat(trace.getSummary()).contains(workflowId, "RESOURCE_GENERATION");
                });

        assertThat(resourceGenerationTaskRepository.findByLearnerIdAndRequestId("alice", "req_orchestrated_resource_1"))
                .hasValueSatisfying(task -> {
                    assertThat(task.getAgentTaskId()).isEqualTo(agentTaskId);
                    assertThat(task.getTraceId()).isEqualTo(traceId);
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_WAITING_REVIEW);
                    assertThat(task.getReviewStatus()).isEqualTo(AgentRuntimeConstants.REVIEW_PENDING_CRITIC);
                    assertThat(learningResourceRepository.countByGenerationTaskId(task.getId())).isEqualTo(5);
                    assertThat(resourceReviewRepository.countByGenerationTaskId(task.getId())).isEqualTo(5);
                });
        assertThat(modelCallLogRepository.countByTraceId(traceId)).isEqualTo(1);
        assertThat(tokenUsageLogRepository.countByTraceId(traceId)).isEqualTo(1);
    }

    @Test
    void resourceGenerationWorkflowRejectsBearerUserSubjectAdminRoleConfusionBeforeResourceSideEffects() throws Exception {
        seedCourse("course_orchestrator_subject_admin", "teacher");

        String responseBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER")))
                        .header("X-Trace-Id", "trc_orchestrator_subject_admin_forbidden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RESOURCE_GENERATION",
                                  "learnerId": "admin",
                                  "payloadJson": "{\\"goalId\\":\\"course_orchestrator_subject_admin\\",\\"pathNodeId\\":\\"node_sql_join\\",\\"resourceTypes\\":[\\"LECTURE\\"]}",
                                  "requestId": "req_orchestrator_subject_admin_forbidden"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody)
                .doesNotContain("course_orchestrator_subject_admin")
                .doesNotContain("req_orchestrator_subject_admin_forbidden");
        assertThat(resourceGenerationTaskRepository
                .findByLearnerIdAndRequestId("admin", "req_orchestrator_subject_admin_forbidden"))
                .isEmpty();
        assertNoResourceGenerationBusinessSideEffects();
        assertThat(agentTaskRepository.findAll())
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.getOwnerUserId()).isEqualTo("admin");
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                    assertThat(task.getTraceId()).isEqualTo("trc_orchestrator_subject_admin_forbidden");
                    assertThat(task.getOutputJson()).contains("FORBIDDEN");
                });
        String agentTaskId = agentTaskRepository.findAll().getFirst().getId();
        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(agentTaskId))
                .hasSize(2)
                .satisfiesExactly(
                        trace -> {
                            assertThat(trace.getStepId()).isEqualTo("workflow_start");
                            assertThat(trace.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_RUNNING);
                        },
                        trace -> {
                            assertThat(trace.getStepId()).isEqualTo("step_runtime_failure");
                            assertThat(trace.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                            assertThat(trace.getSummary()).contains("FORBIDDEN");
                            assertThat(trace.getSummary()).doesNotContain("course_orchestrator_subject_admin");
                        }
                );
    }

    @Test
    void resourceGenerationWorkflowRejectsBearerAdminForOtherLearnerBeforeWorkflowSideEffects() throws Exception {
        seedCourse("course_orchestrator_admin_other_learner", "teacher");

        String responseBody = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_admin_other_learner_forbidden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RESOURCE_GENERATION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"goalId\\":\\"course_orchestrator_admin_other_learner\\",\\"pathNodeId\\":\\"node_sql_join\\",\\"resourceTypes\\":[\\"LECTURE\\"]}",
                                  "requestId": "req_orchestrator_admin_other_learner_forbidden"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody)
                .doesNotContain("course_orchestrator_admin_other_learner")
                .doesNotContain("req_orchestrator_admin_other_learner_forbidden");
        assertNoOrchestratorResourceGenerationSideEffects();
    }

    @Test
    void resourceGenerationWorkflowAllowsBearerStudentOwnerWithActiveEnrollmentAndIgnoresSpoofedHeader() throws Exception {
        seedCourse("course_orchestrator_student_owner", "teacher");
        seedEnrollment("course_orchestrator_student_owner", "alice", "ACTIVE");

        String body = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "admin")
                        .header("X-Trace-Id", "trc_orchestrator_student_owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RESOURCE_GENERATION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"goalId\\":\\"course_orchestrator_student_owner\\",\\"pathNodeId\\":\\"node_sql_join\\",\\"resourceTypes\\":[\\"LECTURE\\"]}",
                                  "requestId": "req_orchestrator_student_owner"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.workflowType").value("RESOURCE_GENERATION"))
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_WAITING_REVIEW))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(body).path("data");
        String agentTaskId = data.path("agentTaskId").asText();
        assertThat(agentTaskRepository.findById(agentTaskId))
                .hasValueSatisfying(task -> assertThat(task.getOwnerUserId()).isEqualTo("alice"));
        assertThat(resourceGenerationTaskRepository.findByLearnerIdAndRequestId("alice", "req_orchestrator_student_owner"))
                .hasValueSatisfying(task -> {
                    assertThat(task.getLearnerId()).isEqualTo("alice");
                    assertThat(task.getCreatedBy()).isEqualTo("alice");
                    assertThat(task.getAgentTaskId()).isEqualTo(agentTaskId);
                });
        assertThat(agentTaskRepository.findAll())
                .noneMatch(task -> "admin".equals(task.getOwnerUserId()));
    }

    @Test
    void returnsWorkflowStatusContextAfterResourceGeneration() throws Exception {
        String body = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RESOURCE_GENERATION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"goalId\\":\\"goal_spring_boot\\",\\"pathNodeId\\":\\"node_sql_join\\",\\"resourceTypes\\":[\\"LECTURE\\"]}",
                                  "requestId": "req_resource_get"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(body).path("data");
        String workflowId = created.path("workflowId").asText();
        String agentTaskId = created.path("agentTaskId").asText();

        mockMvc.perform(get("/api/orchestrator/workflows/{workflowId}", workflowId)
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_get_query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.workflowId").value(workflowId))
                .andExpect(jsonPath("$.data.workflowType").value("RESOURCE_GENERATION"))
                .andExpect(jsonPath("$.data.agentTaskId").value(agentTaskId))
                .andExpect(jsonPath("$.data.traceId").value("trc_orchestrator_get"))
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_WAITING_REVIEW))
                .andExpect(jsonPath("$.data.steps.length()").value(8))
                .andExpect(jsonPath("$.data.steps[0].stepId").value("workflow_start"))
                .andExpect(jsonPath("$.data.steps[0].agentName").value("Orchestrator"))
                .andExpect(jsonPath("$.data.steps[0].sequenceNo").value(1))
                .andExpect(jsonPath("$.data.steps[1].stepId").value("step_planner"))
                .andExpect(jsonPath("$.data.steps[1].sequenceNo").value(2))
                .andExpect(jsonPath("$.data.steps[7].stepId").value("step_safety"))
                .andExpect(jsonPath("$.data.steps[7].sequenceNo").value(8))
                .andExpect(jsonPath("$.data.recentFailedStep").doesNotExist())
                .andExpect(jsonPath("$.data.traceSummary.agentTaskId").value(agentTaskId))
                .andExpect(jsonPath("$.data.traceSummary.traceId").value("trc_orchestrator_get"))
                .andExpect(jsonPath("$.data.traceSummary.totalSteps").value(8))
                .andExpect(jsonPath("$.data.traceSummary.failedSteps").value(0))
                .andExpect(jsonPath("$.data.traceSummary.lastStepId").value("step_safety"))
                .andExpect(jsonPath("$.data.traceSummary.lastStatus").value(AgentRuntimeConstants.STATUS_DONE))
                .andExpect(jsonPath("$.data.nextActions[0]").value("OPEN_REVIEW_QUEUE"));
        assertThat(resourceGenerationTaskRepository.findByLearnerIdAndRequestId("alice", "req_resource_get"))
                .hasValueSatisfying(task -> {
                    assertThat(task.getAgentTaskId()).isEqualTo(agentTaskId);
                    assertThat(task.getTraceId()).isEqualTo("trc_orchestrator_get");
                });
    }

    @Test
    void workflowStatusRejectsBearerNonOwnerForgedWorkflowIdWithoutLeakingMetadata() throws Exception {
        String body = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-Trace-Id", "trc_orchestrator_forged_get_owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RESOURCE_GENERATION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"goalId\\":\\"goal_spring_boot\\",\\"pathNodeId\\":\\"node_sql_join\\",\\"resourceTypes\\":[\\"LECTURE\\"]}",
                                  "requestId": "req_resource_forged_get_owner"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(body).path("data");
        String workflowId = created.path("workflowId").asText();
        String agentTaskId = created.path("agentTaskId").asText();
        long agentTaskCount = agentTaskRepository.count();
        long agentTraceCount = agentTraceRepository.count();

        String responseBody = mockMvc.perform(get("/api/orchestrator/workflows/{workflowId}", workflowId)
                        .header("Authorization", "Bearer " + jwt("bob", "Bob", List.of("STUDENT")))
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_forged_get_attacker"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody)
                .doesNotContain(workflowId)
                .doesNotContain(agentTaskId)
                .doesNotContain("trc_orchestrator_forged_get_owner")
                .doesNotContain("req_resource_forged_get_owner");
        assertThat(agentTaskRepository.count()).isEqualTo(agentTaskCount);
        assertThat(agentTraceRepository.count()).isEqualTo(agentTraceCount);
    }

    @Test
    void workflowRetryRejectsBearerNonOwnerForgedWorkflowIdWithoutSideEffects() throws Exception {
        doThrow(new IllegalStateException("provider unavailable"))
                .when(aiModelGateway)
                .generateStructured(org.mockito.ArgumentMatchers.any(AiModelGateway.ModelRequest.class));

        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-Trace-Id", "trc_orchestrator_forged_retry_owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RESOURCE_GENERATION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"goalId\\":\\"goal_spring_boot\\",\\"pathNodeId\\":\\"node_sql_join\\",\\"resourceTypes\\":[\\"LECTURE\\"]}",
                                  "requestId": "req_resource_forged_retry_owner"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        AgentTask originalTask = agentTaskRepository
                .findByOwnerUserIdAndTaskTypeAndInputJsonContainingOrderByCreatedAtAsc(
                        "alice",
                        AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                        "req_resource_forged_retry_owner"
                )
                .getFirst();
        String workflowId = readJson(originalTask.getInputJson()).path("workflowId").asText();
        long agentTaskCount = agentTaskRepository.count();
        long agentTraceCount = agentTraceRepository.count();
        long resourceTaskCount = resourceGenerationTaskRepository.count();
        long learningResourceCount = learningResourceRepository.count();
        long reviewCount = resourceReviewRepository.count();
        long modelCallCount = modelCallLogRepository.count();
        long tokenUsageCount = tokenUsageLogRepository.count();
        long citationCount = sourceCitationRepository.count();

        String responseBody = mockMvc.perform(post("/api/orchestrator/workflows/{workflowId}/retry", workflowId)
                        .header("Authorization", "Bearer " + jwt("bob", "Bob", List.of("STUDENT")))
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_forged_retry_attacker"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody)
                .doesNotContain(workflowId)
                .doesNotContain(originalTask.getId())
                .doesNotContain("trc_orchestrator_forged_retry_owner")
                .doesNotContain("req_resource_forged_retry_owner");
        assertThat(agentTaskRepository.count()).isEqualTo(agentTaskCount);
        assertThat(agentTraceRepository.count()).isEqualTo(agentTraceCount);
        assertThat(resourceGenerationTaskRepository.count()).isEqualTo(resourceTaskCount);
        assertThat(learningResourceRepository.count()).isEqualTo(learningResourceCount);
        assertThat(resourceReviewRepository.count()).isEqualTo(reviewCount);
        assertThat(modelCallLogRepository.count()).isEqualTo(modelCallCount);
        assertThat(tokenUsageLogRepository.count()).isEqualTo(tokenUsageCount);
        assertThat(sourceCitationRepository.count()).isEqualTo(citationCount);
    }

    @Test
    void persistsFailedResourceGenerationWorkflowEvidenceWhenModelFails() throws Exception {
        doThrow(new IllegalStateException("provider unavailable"))
                .when(aiModelGateway)
                .generateStructured(org.mockito.ArgumentMatchers.any(AiModelGateway.ModelRequest.class));

        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_failed_resource")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RESOURCE_GENERATION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"goalId\\":\\"goal_spring_boot\\",\\"pathNodeId\\":\\"node_sql_join\\",\\"resourceTypes\\":[\\"LECTURE\\"]}",
                                  "requestId": "req_orchestrated_failed_resource"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        var failedTask = resourceGenerationTaskRepository
                .findByLearnerIdAndRequestId("alice", "req_orchestrated_failed_resource");
        assertThat(failedTask)
                .hasValueSatisfying(task -> {
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                    assertThat(task.getTraceId()).isEqualTo("trc_orchestrator_failed_resource");
                    assertThat(agentTaskRepository.findById(task.getAgentTaskId()))
                            .hasValueSatisfying(agentTask -> {
                                assertThat(agentTask.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                                assertThat(agentTask.getInputJson()).contains("req_orchestrated_failed_resource");
                            });
                    assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(task.getAgentTaskId()))
                            .hasSize(2)
                            .satisfiesExactly(
                                    trace -> {
                                        assertThat(trace.getStepId()).isEqualTo("workflow_start");
                                        assertThat(trace.getSequenceNo()).isEqualTo(1);
                                        assertThat(trace.getTraceId()).isEqualTo("trc_orchestrator_failed_resource");
                                    },
                                    trace -> {
                                        assertThat(trace.getStepId()).isEqualTo("step_resource");
                                        assertThat(trace.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                                        assertThat(trace.getSequenceNo()).isEqualTo(2);
                                        assertThat(trace.getSummary()).contains("MODEL_PROVIDER_ERROR");
                                        assertThat(trace.getSummary()).doesNotContain("provider unavailable");
                                        assertThat(trace.getTraceId()).isEqualTo("trc_orchestrator_failed_resource");
                                    }
                            );
                });

        String workflowId = objectMapper.readTree(agentTaskRepository.findById(failedTask.orElseThrow().getAgentTaskId())
                        .orElseThrow()
                        .getInputJson())
                .path("workflowId")
                .asText();

        mockMvc.perform(get("/api/orchestrator/workflows/{workflowId}", workflowId)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workflowId").value(workflowId))
                .andExpect(jsonPath("$.data.agentTaskId").value(failedTask.orElseThrow().getAgentTaskId()))
                .andExpect(jsonPath("$.data.traceId").value("trc_orchestrator_failed_resource"))
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_FAILED))
                .andExpect(jsonPath("$.data.steps.length()").value(2))
                .andExpect(jsonPath("$.data.recentFailedStep.stepId").value("step_resource"))
                .andExpect(jsonPath("$.data.recentFailedStep.inputDto").value("ResourceGenerationRequest"))
                .andExpect(jsonPath("$.data.recentFailedStep.outputDto").value("ResourceGenerationResponse"))
                .andExpect(jsonPath("$.data.recentFailedStep.failurePolicy").value("PERSIST_FAILED_TRACE"))
                .andExpect(jsonPath("$.data.recentFailedStep.retryPolicy").value("RETRY_WORKFLOW_RESOURCE_GENERATION_ONLY"))
                .andExpect(jsonPath("$.data.recentFailedStep.retryable").value(true))
                .andExpect(jsonPath("$.data.traceSummary.failedSteps").value(1))
                .andExpect(jsonPath("$.data.nextActions[0]").value("INSPECT_TRACE"))
                .andExpect(jsonPath("$.data.nextActions[1]").value("RETRY_WORKFLOW"));
    }

    @Test
    void retriesFailedResourceGenerationWorkflowAsNewWorkflowForOwner() throws Exception {
        doThrow(new IllegalStateException("provider unavailable first attempt"))
                .doThrow(new IllegalStateException("provider unavailable second attempt"))
                .doCallRealMethod()
                .when(aiModelGateway)
                .generateStructured(org.mockito.ArgumentMatchers.any(AiModelGateway.ModelRequest.class));

        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_resource_retry_original")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RESOURCE_GENERATION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"goalId\\":\\"goal_spring_boot\\",\\"pathNodeId\\":\\"node_sql_join\\",\\"resourceTypes\\":[\\"LECTURE\\"]}",
                                  "requestId": "req_resource_retry_original"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        AgentTask originalTask = agentTaskRepository
                .findByOwnerUserIdAndTaskTypeAndInputJsonContainingOrderByCreatedAtAsc(
                        "alice",
                        AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                        "req_resource_retry_original"
                )
                .getFirst();
        String originalWorkflowId = readJson(originalTask.getInputJson()).path("workflowId").asText();
        assertThat(originalTask.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);

        String retryBody = mockMvc.perform(post("/api/orchestrator/workflows/{workflowId}/retry", originalWorkflowId)
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_resource_retry_success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.workflowId").isNotEmpty())
                .andExpect(jsonPath("$.data.workflowType").value("RESOURCE_GENERATION"))
                .andExpect(jsonPath("$.data.agentTaskId").isNotEmpty())
                .andExpect(jsonPath("$.data.traceId").value("trc_resource_retry_success"))
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_WAITING_REVIEW))
                .andExpect(jsonPath("$.data.steps.length()").value(8))
                .andExpect(jsonPath("$.data.steps[0].retryPolicy").value("RETRY_WORKFLOW_RESOURCE_GENERATION_ONLY"))
                .andExpect(jsonPath("$.data.steps[0].retryable").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode retried = objectMapper.readTree(retryBody).path("data");
        String retriedWorkflowId = retried.path("workflowId").asText();
        String retriedAgentTaskId = retried.path("agentTaskId").asText();
        assertThat(retried.path("retryOfWorkflowId").asText()).isEqualTo(originalWorkflowId);
        assertThat(retriedWorkflowId).isNotEqualTo(originalWorkflowId);
        assertThat(retriedAgentTaskId).isNotEqualTo(originalTask.getId());
        assertThat(agentTaskRepository.findById(originalTask.getId()))
                .hasValueSatisfying(task -> assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED));
        assertThat(agentTaskRepository.findById(retriedAgentTaskId))
                .hasValueSatisfying(task -> {
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_WAITING_REVIEW);
                    assertThat(task.getInputJson())
                            .contains("\"retryOfWorkflowId\":\"" + originalWorkflowId + "\"")
                            .doesNotContain("\"requestId\":\"req_resource_retry_original\"");
                });
        assertThat(resourceGenerationTaskRepository.findAll())
                .hasSize(2)
                .anySatisfy(task -> assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED))
                .anySatisfy(task -> {
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_WAITING_REVIEW);
                    assertThat(task.getTraceId()).isEqualTo("trc_resource_retry_success");
                });

        mockMvc.perform(post("/api/orchestrator/workflows/{workflowId}/retry", originalWorkflowId)
                        .header("X-User-Id", "bob")
                        .header("X-Trace-Id", "trc_resource_retry_bob"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void rejectsRetryForNonFailedWorkflow() throws Exception {
        String body = mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_resource_retry_non_failed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RESOURCE_GENERATION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"goalId\\":\\"goal_spring_boot\\",\\"pathNodeId\\":\\"node_sql_join\\",\\"resourceTypes\\":[\\"LECTURE\\"]}",
                                  "requestId": "req_resource_retry_non_failed"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String workflowId = objectMapper.readTree(body).path("data").path("workflowId").asText();

        mockMvc.perform(post("/api/orchestrator/workflows/{workflowId}/retry", workflowId)
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_resource_retry_non_failed_attempt"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Only FAILED workflows can be retried"));
    }

    @Test
    void rejectsInvalidResourceGenerationPayloadBeforeCreatingWorkflowTask() throws Exception {
        mockMvc.perform(post("/api/orchestrator/workflows")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_invalid_resource_payload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowType": "RESOURCE_GENERATION",
                                  "learnerId": "alice",
                                  "payloadJson": "{\\"goalId\\":\\"goal_spring_boot\\"}",
                                  "requestId": "req_invalid_resource_payload"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(resourceGenerationTaskRepository.findByLearnerIdAndRequestId("alice", "req_invalid_resource_payload"))
                .isEmpty();
    }

    @Test
    void returnsNotFoundEnvelopeForMissingWorkflow() throws Exception {
        mockMvc.perform(get("/api/orchestrator/workflows/{workflowId}", "wf_missing")
                        .header("X-User-Id", "alice")
                        .header("X-Trace-Id", "trc_orchestrator_missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Workflow not found"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private void seedIndexedChunk(String kbId, String documentId, String content) {
        KnowledgeBase kb = seedPrivateKnowledgeBase(kbId, "alice");
        KbDocument document = new KbDocument();
        document.setId(documentId);
        document.setKnowledgeBase(kb);
        document.setKbId(kbId);
        document.setName("database-course.md");
        document.setContentType("text/markdown");
        document.setSizeBytes((long) content.length());
        document.setStorageBucket("test");
        document.setStorageKey("test/" + documentId);
        document.setVersion(1);
        document.setParseStatus(DocumentStatus.INDEXED);
        document.setIndexStatus(DocumentStatus.INDEXED);
        document.setCreatedBy("alice");
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);

        KbDocChunk chunk = new KbDocChunk();
        chunk.setId("chunk_" + documentId);
        chunk.setKbId(kbId);
        chunk.setDocument(document);
        chunk.setDocumentId(documentId);
        chunk.setDocumentVersion(1);
        chunk.setChunkIndex(0);
        chunk.setContent(content);
        chunk.setPageNum(12);
        chunk.setSectionTitle("Multi table joins");
        chunk.setMetadataJson("{}");
        chunk.setCreatedAt(Instant.now());
        chunkRepository.save(chunk);
    }

    private KnowledgeBase seedPrivateKnowledgeBase(String id, String owner) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setName(id);
        kb.setDescription(id + " description");
        kb.setVisibility(Visibility.PRIVATE);
        kb.setOwnerUserId(owner);
        kb.setCreatedBy(owner);
        kb.setCreatedAt(Instant.now());
        kb.setUpdatedAt(Instant.now());
        return knowledgeBaseRepository.save(kb);
    }

    private void seedAgentTask(
            String taskId,
            String ownerUserId,
            String taskType,
            String status,
            String traceId,
            String inputJson
    ) {
        AgentTask task = new AgentTask();
        task.setId(taskId);
        task.setOwnerUserId(ownerUserId);
        task.setTaskType(taskType);
        task.setStatus(status);
        task.setTraceId(traceId);
        task.setInputJson(inputJson);
        task.setOutputJson("{\"status\":\"" + status + "\"}");
        task.setLatencyMs(0L);
        agentTaskRepository.saveAndFlush(task);
    }

    private void assertNoResourceGenerationBusinessSideEffects() {
        assertThat(resourceGenerationTaskRepository.count()).isZero();
        assertThat(learningResourceRepository.count()).isZero();
        assertThat(resourceReviewRepository.count()).isZero();
        assertThat(sourceCitationRepository.count()).isZero();
        assertThat(modelCallLogRepository.count()).isZero();
        assertThat(tokenUsageLogRepository.count()).isZero();
    }

    private void assertNoOrchestratorResourceGenerationSideEffects() {
        assertNoResourceGenerationBusinessSideEffects();
        assertThat(agentTaskRepository.count()).isZero();
        assertThat(agentTraceRepository.count()).isZero();
    }

    private void seedCourse(String courseId, String teacherId) {
        if (courseRepository.existsById(courseId)) {
            return;
        }
        Course course = new Course();
        course.setId(courseId);
        course.setTitle("Course " + courseId);
        course.setDescription("Course for orchestrator resource generation tests.");
        course.setTeacherId(teacherId);
        course.setStatus("PUBLISHED");
        courseRepository.save(course);
    }

    private void seedEnrollment(String courseId, String learnerId, String status) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setCourseId(courseId);
        enrollment.setLearnerId(learnerId);
        enrollment.setStatus(status);
        courseEnrollmentRepository.save(enrollment);
    }

    private String seedCourseKnowledgeAndEnrollment(
            String courseId,
            String teacherId,
            String learnerId,
            String enrollmentStatus,
            String knowledgePointTitle
    ) {
        seedCourse(courseId, teacherId);
        seedEnrollment(courseId, learnerId, enrollmentStatus);

        KnowledgePoint point = new KnowledgePoint();
        point.setCourseId(courseId);
        point.setChapterId("chapter_" + courseId);
        point.setTitle(knowledgePointTitle);
        point.setDescription("Orchestrator answer replay scope linked knowledge point.");
        KnowledgePoint savedPoint = knowledgePointRepository.save(point);
        return "q_" + savedPoint.getId().substring("kp_".length());
    }

    private static String jwt(String sub, String name, List<String> roles) throws Exception {
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String roleJson = roles.stream()
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(","));
        String payload = "{\"sub\":\"" + sub + "\",\"name\":\"" + name + "\",\"roles\":[" + roleJson
                + "],\"iss\":\"" + AUTH_ISSUER + "\",\"exp\":" + Instant.now().plusSeconds(3600).getEpochSecond() + "}";
        String signingInput = base64Url(header) + "." + base64Url(payload);
        return signingInput + "." + sign(signingInput);
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String signingInput) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(AUTH_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII)));
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new AssertionError("Expected valid JSON", exception);
        }
    }
}
