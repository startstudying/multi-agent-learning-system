package com.learningos.agent.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.agent.repository.AgentTaskRepository;
import com.learningos.agent.repository.AgentTraceRepository;
import com.learningos.agent.repository.LearningResourceRepository;
import com.learningos.agent.repository.ModelCallLogRepository;
import com.learningos.agent.repository.ResourceGenerationTaskRepository;
import com.learningos.agent.repository.ResourceReviewRepository;
import com.learningos.agent.repository.TokenUsageLogRepository;
import com.learningos.agent.application.AgentRuntimeConstants;
import com.learningos.agent.application.AiModelGateway;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.domain.CourseEnrollment;
import com.learningos.knowledge.repository.CourseEnrollmentRepository;
import com.learningos.knowledge.repository.CourseRepository;
import com.learningos.learning.domain.LearnerProfile;
import com.learningos.learning.repository.LearnerProfileRepository;
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
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
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
class ResourceGenerationControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final ResourceGenerationTaskRepository resourceGenerationTaskRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final ResourceReviewRepository resourceReviewRepository;
    private final AgentTaskRepository agentTaskRepository;
    private final AgentTraceRepository agentTraceRepository;
    private final ModelCallLogRepository modelCallLogRepository;
    private final TokenUsageLogRepository tokenUsageLogRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final SourceCitationRepository sourceCitationRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    @MockitoSpyBean
    private AiModelGateway aiModelGateway;

    ResourceGenerationControllerTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            ResourceGenerationTaskRepository resourceGenerationTaskRepository,
            LearningResourceRepository learningResourceRepository,
            ResourceReviewRepository resourceReviewRepository,
            AgentTaskRepository agentTaskRepository,
            AgentTraceRepository agentTraceRepository,
            ModelCallLogRepository modelCallLogRepository,
            TokenUsageLogRepository tokenUsageLogRepository,
            LearnerProfileRepository learnerProfileRepository,
            SourceCitationRepository sourceCitationRepository,
            CourseRepository courseRepository,
            CourseEnrollmentRepository courseEnrollmentRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.resourceGenerationTaskRepository = resourceGenerationTaskRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.resourceReviewRepository = resourceReviewRepository;
        this.agentTaskRepository = agentTaskRepository;
        this.agentTraceRepository = agentTraceRepository;
        this.modelCallLogRepository = modelCallLogRepository;
        this.tokenUsageLogRepository = tokenUsageLogRepository;
        this.learnerProfileRepository = learnerProfileRepository;
        this.sourceCitationRepository = sourceCitationRepository;
        this.courseRepository = courseRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
    }

    @Test
    void createsResourceGenerationTaskAndExposesAgentTrace() throws Exception {
        String taskBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE", "EXERCISE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").isNotEmpty())
                .andExpect(jsonPath("$.data.agentTaskId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("WAITING_REVIEW"))
                .andExpect(jsonPath("$.data.reviewStatus").value("PENDING_CRITIC"))
                .andExpect(jsonPath("$.data.progressPercent").value(60))
                .andExpect(jsonPath("$.data.safetyStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.profileSnapshot").isNotEmpty())
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("baseline_level")))
                .andExpect(jsonPath("$.data.resources[0].markdownContent").doesNotExist())
                .andExpect(jsonPath("$.data.traceId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(taskBody).path("data").path("taskId").asText();
        String agentTaskId = objectMapper.readTree(taskBody).path("data").path("agentTaskId").asText();
        assertThat(taskId).isNotBlank();
        assertThat(resourceGenerationTaskRepository.findById(taskId))
                .hasValueSatisfying(task -> assertThat(task.getProfileSnapshot()).contains("baseline_level"));
        assertThat(agentTaskRepository.findById(agentTaskId)).isPresent();
        assertThat(learningResourceRepository.countByGenerationTaskId(taskId)).isEqualTo(5);
        assertThat(resourceReviewRepository.countByGenerationTaskId(taskId)).isEqualTo(5);
        assertThat(agentTraceRepository.countByAgentTaskId(agentTaskId)).isEqualTo(7);
        assertThat(modelCallLogRepository.countByTraceId(objectMapper.readTree(taskBody).path("data").path("traceId").asText()))
                .isGreaterThanOrEqualTo(1);
        assertThat(modelCallLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getPromptCode()).isEqualTo("resource-generation");
                    assertThat(log.getPromptVersion()).isEqualTo(AgentRuntimeConstants.PROMPT_RESOURCE_V1);
                    assertThat(log.getTemperature()).isEqualTo(0.0);
                    assertThat(log.getStructuredOutputSchema())
                            .contains("ResourceGenerationOutputSchema", "resources", "title", "citationSummary");
                });
        assertThat(tokenUsageLogRepository.countByTraceId(objectMapper.readTree(taskBody).path("data").path("traceId").asText()))
                .isGreaterThanOrEqualTo(1);

        mockMvc.perform(get("/api/resources/generation-tasks/{taskId}", taskId).header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.profileSnapshot").value(objectMapper.readTree(taskBody).path("data").path("profileSnapshot").asText()))
                .andExpect(jsonPath("$.data.resources.length()").value(5))
                .andExpect(jsonPath("$.data.resources[0].reviewStatus").value("PENDING_CRITIC"))
                .andExpect(jsonPath("$.data.resources[0].modality").isNotEmpty())
                .andExpect(jsonPath("$.data.resources[0].markdownContent").doesNotExist())
                .andExpect(jsonPath("$.data.resources[0].safetyStatus").value("NEEDS_REVIEW"));

        mockMvc.perform(get("/api/agent/tasks/{taskId}/trace", agentTaskId).header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(agentTaskId))
                .andExpect(jsonPath("$.data.status").value("WAITING_REVIEW"))
                .andExpect(jsonPath("$.data.steps.length()").value(7))
                .andExpect(jsonPath("$.data.steps[0].agentName").value("PlannerAgent"))
                .andExpect(jsonPath("$.data.steps[1].agentName").value("TeacherAgent"))
                .andExpect(jsonPath("$.data.steps[2].agentName").value("ResourceAgent"))
                .andExpect(jsonPath("$.data.steps[3].agentName").value("QuestionAgent"))
                .andExpect(jsonPath("$.data.steps[4].agentName").value("CriticAgent"))
                .andExpect(jsonPath("$.data.steps[4].status").value("PENDING"))
                .andExpect(jsonPath("$.data.steps[5].agentName").value("TutorAgent"))
                .andExpect(jsonPath("$.data.steps[5].status").value("WAITING"))
                .andExpect(jsonPath("$.data.steps[6].agentName").value("SafetyAgent"))
                .andExpect(jsonPath("$.data.steps[0].model").value("deterministic-demo-model"))
                .andExpect(jsonPath("$.data.steps[0].promptVersion").value("agent-resource-v1"));
    }

    @Test
    void resourceGenerationSnapshotUsesPersistedLearnerProfile() throws Exception {
        LearnerProfile profile = new LearnerProfile();
        profile.setLearnerId("profiled_alice");
        profile.setTarget("Master Spring Boot data access");
        profile.setWeakPointsJson("[\"SQL JOIN reasoning\"]");
        profile.setPreferencesJson("[\"code labs\"]");
        profile.setDimensionsJson("""
                {
                  "dimensions": [],
                  "baseline_level": "Java basics with weak SQL joins",
                  "learning_goal": "Master Spring Boot data access",
                  "weak_point": ["SQL JOIN reasoning"],
                  "preference": ["code labs"],
                  "pace_and_feedback": "Short practice loops with direct feedback",
                  "recent_error_pattern": "Confuses JOIN cardinality in quiz answers",
                  "teacher_note": "Assign repository exercises before transactions",
                  "sources": ["CONVERSATION", "ASSESSMENT", "TEACHER_NOTE"]
                }
                """);
        profile.setUpdatePolicy("LEARN_AS_YOU_GO");
        profile.setTraceId("trace_profiled_alice");
        learnerProfileRepository.save(profile);

        String responseBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "profiled_alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "profiled_alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE", "EXERCISE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("Java basics with weak SQL joins")))
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("SQL JOIN reasoning")))
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("code labs")))
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("Confuses JOIN cardinality")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(responseBody).path("data").path("taskId").asText();
        assertThat(resourceGenerationTaskRepository.findById(taskId))
                .hasValueSatisfying(task -> assertThat(task.getProfileSnapshot())
                        .contains("Java basics with weak SQL joins", "code labs", "Confuses JOIN cardinality"));
    }

    @Test
    void resourceGenerationSnapshotKeepsHistoricalProfileFieldAliases() throws Exception {
        LearnerProfile profile = new LearnerProfile();
        profile.setLearnerId("legacy_resource_alice");
        profile.setTarget("Legacy resource goal");
        profile.setWeakPointsJson("[\"Legacy SQL weak point\"]");
        profile.setPreferencesJson("[\"Legacy diagram preference\"]");
        profile.setDimensionsJson("""
                {
                  "dimensions": [
                    {
                      "name": "knowledge_base",
                      "value": "Legacy Java basics",
                      "confidence": 0.82,
                      "evidence": "historical baseline",
                      "sourceType": "CONVERSATION"
                    },
                    {
                      "name": "resource_preference",
                      "value": "Legacy code labs",
                      "confidence": 0.79,
                      "evidence": "historical resource preference",
                      "sourceType": "CONVERSATION"
                    },
                    {
                      "name": "error_pattern",
                      "value": "Legacy JOIN mistakes",
                      "confidence": 0.84,
                      "evidence": "historical assessment",
                      "sourceType": "ASSESSMENT"
                    }
                  ],
                  "sources": ["CONVERSATION", "ASSESSMENT"]
                }
                """);
        profile.setUpdatePolicy("LEARN_AS_YOU_GO");
        profile.setTraceId("trace_legacy_resource");
        learnerProfileRepository.save(profile);

        mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "legacy_resource_alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "legacy_resource_alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("Legacy Java basics")))
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("Legacy code labs")))
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("Legacy SQL weak point")))
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("Legacy diagram preference")))
                .andExpect(jsonPath("$.data.profileSnapshot").value(org.hamcrest.Matchers.containsString("Legacy JOIN mistakes")));
    }

    @Test
    void resourceGenerationPersistsSourceCitationsAndCriticCitationCheck() throws Exception {
        String responseBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE", "EXERCISE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources[0].citationSummary").value(org.hamcrest.Matchers.containsString("COURSE_RAG")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(responseBody).path("data").path("taskId").asText();
        String traceId = objectMapper.readTree(responseBody).path("data").path("traceId").asText();

        assertThat(sourceCitationRepository.countByTraceId(traceId)).isGreaterThan(0);
        assertThat(sourceCitationRepository.findByTraceIdOrderByCreatedAtAsc(traceId))
                .allSatisfy(citation -> {
                    assertThat(citation.getDocumentId()).isNotBlank();
                    assertThat(citation.getDocumentName()).isNotBlank();
                    assertThat(citation.getExcerpt()).isNotBlank();
                    assertThat(citation.getScore()).isGreaterThan(0.0);
                });
        assertThat(resourceReviewRepository.findByGenerationTaskIdOrderByCreatedAtAsc(taskId))
                .allSatisfy(review -> assertThat(review.getCitationCheck())
                        .contains("persisted source citation", "fabricated source"));
    }

    @Test
    void noSourceGeneratedResourcesRequireReviewAndRejectApproval() throws Exception {
        seedCourse("goal_no_source", "teacher");
        seedEnrollment("goal_no_source", "alice", "ACTIVE");

        String responseBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_no_source",
                                  "pathNodeId": "node_no_source",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("PENDING_CRITIC"))
                .andExpect(jsonPath("$.data.resources[0].citationSummary").value(org.hamcrest.Matchers.containsString("NO_SOURCE")))
                .andExpect(jsonPath("$.data.resources[0].safetyStatus").value("NEEDS_REVIEW"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(responseBody).path("data").path("taskId").asText();
        String traceId = objectMapper.readTree(responseBody).path("data").path("traceId").asText();
        assertThat(sourceCitationRepository.countByTraceId(traceId)).isZero();

        var review = resourceReviewRepository.findByGenerationTaskIdOrderByCreatedAtAsc(taskId).getFirst();
        assertThat(review.getCitationCheck()).contains("NO_SOURCE", "manual review");

        mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", review.getId())
                        .header("X-User-Id", "teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "summary": "Approve despite missing citations."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        mockMvc.perform(get("/api/agent/resource-generation/tasks/{taskId}/learner-resources", taskId)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsExistingTaskForRepeatedLearnerRequestIdWithoutDuplicatingResourcesOrTrace() throws Exception {
        String requestBody = """
                {
                  "learnerId": "alice",
                  "goalId": "goal_spring_boot",
                  "pathNodeId": "node_sql_join",
                  "resourceTypes": ["LECTURE", "EXERCISE"],
                  "requestId": "req_resource_generation_once"
                }
                """;

        String firstBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").isNotEmpty())
                .andExpect(jsonPath("$.data.resources.length()").value(5))
                .andExpect(jsonPath("$.data.resources[0].markdownContent").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstTaskId = objectMapper.readTree(firstBody).path("data").path("taskId").asText();
        String firstAgentTaskId = objectMapper.readTree(firstBody).path("data").path("agentTaskId").asText();
        String firstTraceId = objectMapper.readTree(firstBody).path("data").path("traceId").asText();
        assertThat(resourceGenerationTaskRepository.count()).isEqualTo(1);
        assertThat(learningResourceRepository.count()).isEqualTo(5);
        assertThat(learningResourceRepository.countByGenerationTaskId(firstTaskId)).isEqualTo(5);
        assertThat(resourceReviewRepository.countByGenerationTaskId(firstTaskId)).isEqualTo(5);
        assertThat(agentTraceRepository.countByAgentTaskId(firstAgentTaskId)).isEqualTo(7);
        assertThat(modelCallLogRepository.countByTraceId(firstTraceId)).isEqualTo(1);
        assertThat(tokenUsageLogRepository.countByTraceId(firstTraceId)).isEqualTo(1);

        String secondBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(firstTaskId))
                .andExpect(jsonPath("$.data.agentTaskId").value(firstAgentTaskId))
                .andExpect(jsonPath("$.data.profileSnapshot").value(objectMapper.readTree(firstBody).path("data").path("profileSnapshot").asText()))
                .andExpect(jsonPath("$.data.traceId").value(firstTraceId))
                .andExpect(jsonPath("$.data.resources.length()").value(5))
                .andExpect(jsonPath("$.data.resources[0].markdownContent").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(secondBody).path("data").path("taskId").asText()).isEqualTo(firstTaskId);
        assertThat(resourceGenerationTaskRepository.count()).isEqualTo(1);
        assertThat(learningResourceRepository.count()).isEqualTo(5);
        assertThat(learningResourceRepository.countByGenerationTaskId(firstTaskId)).isEqualTo(5);
        assertThat(resourceReviewRepository.countByGenerationTaskId(firstTaskId)).isEqualTo(5);
        assertThat(agentTraceRepository.countByAgentTaskId(firstAgentTaskId)).isEqualTo(7);
        assertThat(modelCallLogRepository.countByTraceId(firstTraceId)).isEqualTo(1);
        assertThat(tokenUsageLogRepository.countByTraceId(firstTraceId)).isEqualTo(1);
    }

    @Test
    void rejectsCrossUserTaskAndTraceAccess() throws Exception {
        String taskBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE", "EXERCISE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(taskBody).path("data").path("taskId").asText();
        String agentTaskId = objectMapper.readTree(taskBody).path("data").path("agentTaskId").asText();

        mockMvc.perform(get("/api/resources/generation-tasks/{taskId}", taskId).header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(taskId));
        mockMvc.perform(get("/api/agent/tasks/{taskId}/trace", agentTaskId).header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(agentTaskId));

        mockMvc.perform(get("/api/resources/generation-tasks/{taskId}", taskId).header("X-User-Id", "bob"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/agent/tasks/{taskId}/trace", agentTaskId).header("X-User-Id", "bob"))
                .andExpect(status().isForbidden());
    }

    @Test
    void taskAndTraceDetailsDoNotRevealMissingVersusForeignObjectsToNonAdmin() throws Exception {
        String taskBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE", "EXERCISE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(taskBody).path("data").path("taskId").asText();
        String agentTaskId = objectMapper.readTree(taskBody).path("data").path("agentTaskId").asText();

        String foreignTaskBody = mockMvc.perform(get("/api/resources/generation-tasks/{taskId}", taskId)
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String missingTaskBody = mockMvc.perform(get("/api/resources/generation-tasks/{taskId}", "rgt_missing_object_scope")
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String foreignTraceBody = mockMvc.perform(get("/api/agent/tasks/{taskId}/trace", agentTaskId)
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String missingTraceBody = mockMvc.perform(get("/api/agent/tasks/{taskId}/trace", "agt_missing_object_scope")
                        .header("X-User-Id", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(foreignTaskBody).doesNotContain(taskId);
        assertThat(missingTaskBody).doesNotContain("rgt_missing_object_scope");
        assertThat(foreignTraceBody).doesNotContain(agentTaskId);
        assertThat(missingTraceBody).doesNotContain("agt_missing_object_scope");
    }

    @Test
    void resourceGenerationDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        String taskBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(taskBody).path("data").path("taskId").asText();

        mockMvc.perform(get("/api/resources/generation-tasks/{taskId}", taskId)
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(taskId));
    }

    @Test
    void resourceGenerationDetailRejectsBearerUserSubjectAdminRoleConfusion() throws Exception {
        String taskBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(taskBody).path("data").path("taskId").asText();

        String responseBody = mockMvc.perform(get("/api/resources/generation-tasks/{taskId}", taskId)
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain(taskId);
    }

    @Test
    void resourceGenerationDetailBearerAdminMissingTaskReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/resources/generation-tasks/{taskId}", "rgt_missing_roles_first")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void learnerResourcesRejectsBearerUserSubjectAdminMissingTaskAsForbidden() throws Exception {
        String responseBody = mockMvc.perform(get("/api/agent/resource-generation/tasks/{taskId}/learner-resources", "rgt_missing_learner_resources_roles_first")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("rgt_missing_learner_resources_roles_first");
    }

    @Test
    void learnerResourcesUsesBearerOwnerAndIgnoresSpoofedUserIdHeaderAfterRelease() throws Exception {
        seedCourse("course_resource_owner_release", "teacher");
        seedEnrollment("course_resource_owner_release", "alice", "ACTIVE");

        String taskBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "course_resource_owner_release",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(taskBody).path("data").path("taskId").asText();
        approveAllReviewsForTask(taskId);

        mockMvc.perform(get("/api/agent/resource-generation/tasks/{taskId}/learner-resources", taskId)
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.resources.length()").value(5))
                .andExpect(jsonPath("$.data.resources[0].resourceId").isNotEmpty())
                .andExpect(jsonPath("$.data.resources[0].markdownContent").isNotEmpty())
                .andExpect(jsonPath("$.data.resources[0].reviewStatus").doesNotExist())
                .andExpect(jsonPath("$.data.traceId").doesNotExist())
                .andExpect(jsonPath("$.data.agentTaskId").doesNotExist());
    }

    @Test
    void learnerResourcesRejectsBearerAdminForForeignLearnerEvenAfterRelease() throws Exception {
        seedCourse("course_resource_admin_foreign_release", "teacher");
        seedEnrollment("course_resource_admin_foreign_release", "alice", "ACTIVE");

        String taskBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "course_resource_admin_foreign_release",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(taskBody).path("data").path("taskId").asText();
        approveAllReviewsForTask(taskId);

        String responseBody = mockMvc.perform(get("/api/agent/resource-generation/tasks/{taskId}/learner-resources", taskId)
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody)
                .doesNotContain(taskId)
                .doesNotContain("markdownContent")
                .doesNotContain("traceId");
    }

    @Test
    void learnerResourcesBearerStudentCannotDistinguishMissingTaskFromForeignTask() throws Exception {
        seedCourse("course_resource_foreign_oracle", "teacher");
        seedEnrollment("course_resource_foreign_oracle", "alice", "ACTIVE");

        String taskBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "course_resource_foreign_oracle",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(taskBody).path("data").path("taskId").asText();
        String token = jwt("bob", "Bob", List.of("STUDENT"));

        String foreignBody = mockMvc.perform(get("/api/agent/resource-generation/tasks/{taskId}/learner-resources", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String missingBody = mockMvc.perform(get("/api/agent/resource-generation/tasks/{taskId}/learner-resources", "rgt_missing_bearer_student_oracle")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(foreignBody)
                .doesNotContain(taskId)
                .doesNotContain("resourceId")
                .doesNotContain("agentTaskId");
        assertThat(missingBody)
                .doesNotContain("rgt_missing_bearer_student_oracle")
                .doesNotContain(taskId)
                .doesNotContain("resourceId")
                .doesNotContain("agentTaskId");
    }

    @Test
    void learnerResourcesRemainHiddenUntilGovernanceAllowsRelease() throws Exception {
        seedCourse("goal_spring_boot", "teacher");
        seedEnrollment("goal_spring_boot", "alice", "ACTIVE");

        String taskBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE", "EXERCISE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(taskBody).path("data").path("taskId").asText();

        mockMvc.perform(get("/api/agent/resource-generation/tasks/{taskId}/learner-resources", taskId)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/resources/generation-tasks/{taskId}", taskId).header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources.length()").value(5))
                .andExpect(jsonPath("$.data.resources[0].reviewStatus").value("PENDING_CRITIC"))
                .andExpect(jsonPath("$.data.resources[0].markdownContent").doesNotExist());

        for (var review : resourceReviewRepository.findAll().stream()
                .filter(review -> taskId.equals(review.getGenerationTaskId()))
                .toList()) {
            mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", review.getId())
                            .header("X-User-Id", "teacher")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "APPROVED",
                                      "summary": "Accurate and ready for learner release."
                                    }
                                    """))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/agent/resource-generation/tasks/{taskId}/learner-resources", taskId)
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.resources.length()").value(5))
                .andExpect(jsonPath("$.data.resources[0].resourceId").isNotEmpty())
                .andExpect(jsonPath("$.data.resources[0].markdownContent").isNotEmpty())
                .andExpect(jsonPath("$.data.resources[0].reviewStatus").doesNotExist())
                .andExpect(jsonPath("$.data.traceId").doesNotExist())
                .andExpect(jsonPath("$.data.agentTaskId").doesNotExist());

        mockMvc.perform(get("/api/resources/generation-tasks/{taskId}", taskId).header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resources[0].markdownContent").isNotEmpty());
    }

    @Test
    void courseBoundResourceGenerationRequiresActiveEnrollmentButTemplateGoalsStayCompatible() throws Exception {
        seedCourse("course_resource_enrollment", "teacher");

        mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "course_resource_enrollment",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(resourceGenerationTaskRepository.findAll())
                .noneMatch(task -> "course_resource_enrollment".equals(task.getGoalId()));
        assertThat(learningResourceRepository.count()).isZero();
        assertThat(resourceReviewRepository.count()).isZero();

        seedEnrollment("course_resource_enrollment", "alice", "ACTIVE");
        mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "course_resource_enrollment",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_REVIEW"));

        mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "template_alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "template_alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_REVIEW"));
    }

    @Test
    void courseBoundResourceGenerationCreateRejectsBearerAdminForOtherLearnerWithoutSideEffects() throws Exception {
        seedCourse("course_resource_admin_other_learner", "teacher");

        String responseBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "course_resource_admin_other_learner",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("course_resource_admin_other_learner");
        assertThat(resourceGenerationTaskRepository.findAll())
                .noneMatch(task -> "course_resource_admin_other_learner".equals(task.getGoalId()));
        assertNoResourceGenerationSideEffects();
    }

    @Test
    void courseBoundResourceGenerationCreateAllowsBearerOwnerWithActiveEnrollmentDespiteSpoofedHeader() throws Exception {
        seedCourse("course_resource_owner_spoofed_header", "teacher");
        seedEnrollment("course_resource_owner_spoofed_header", "alice", "ACTIVE");

        String responseBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "course_resource_owner_spoofed_header",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("WAITING_REVIEW"))
                .andExpect(jsonPath("$.data.resources[0].markdownContent").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = objectMapper.readTree(responseBody).path("data").path("taskId").asText();
        assertThat(resourceGenerationTaskRepository.findById(taskId))
                .hasValueSatisfying(task -> {
                    assertThat(task.getLearnerId()).isEqualTo("alice");
                    assertThat(task.getGoalId()).isEqualTo("course_resource_owner_spoofed_header");
                    assertThat(task.getCreatedBy()).isEqualTo("alice");
                });
    }

    @Test
    void courseBoundResourceGenerationCreateRejectsBearerOwnerWithDroppedEnrollmentWithoutSideEffects() throws Exception {
        seedCourse("course_resource_owner_dropped_enrollment", "teacher");
        seedEnrollment("course_resource_owner_dropped_enrollment", "alice", "DROPPED");

        String responseBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "course_resource_owner_dropped_enrollment",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("course_resource_owner_dropped_enrollment");
        assertThat(resourceGenerationTaskRepository.findAll())
                .noneMatch(task -> "course_resource_owner_dropped_enrollment".equals(task.getGoalId()));
        assertNoResourceGenerationSideEffects();
    }

    @Test
    void courseBoundResourceGenerationCreateRejectsBearerOwnerWithNoEnrollmentWithoutSideEffects() throws Exception {
        seedCourse("course_resource_owner_no_enrollment", "teacher");

        String responseBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "course_resource_owner_no_enrollment",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("course_resource_owner_no_enrollment");
        assertThat(resourceGenerationTaskRepository.findAll())
                .noneMatch(task -> "course_resource_owner_no_enrollment".equals(task.getGoalId()));
        assertNoResourceGenerationSideEffects();
    }

    @Test
    void courseBoundResourceGenerationCreateRejectsBearerTeacherForStudentInOwnCourseWithoutSideEffects() throws Exception {
        seedCourse("course_resource_teacher_own_denied", "teacher_owner");
        seedEnrollment("course_resource_teacher_own_denied", "alice", "ACTIVE");

        String responseBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("Authorization", "Bearer " + jwt("teacher_owner", "Teacher Owner", List.of("TEACHER")))
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "course_resource_teacher_own_denied",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("course_resource_teacher_own_denied");
        assertThat(resourceGenerationTaskRepository.findAll())
                .noneMatch(task -> "course_resource_teacher_own_denied".equals(task.getGoalId()));
        assertNoResourceGenerationSideEffects();
    }

    @Test
    void courseBoundResourceGenerationCreateRejectsBearerUserSubjectAdminRoleConfusionBeforeSideEffects() throws Exception {
        seedCourse("course_resource_subject_admin", "teacher");

        String responseBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "admin",
                                  "goalId": "course_resource_subject_admin",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("course_resource_subject_admin");
        assertThat(resourceGenerationTaskRepository.findAll())
                .noneMatch(task -> "course_resource_subject_admin".equals(task.getGoalId()));
        assertNoResourceGenerationSideEffects();
    }

    @Test
    void rejectsMismatchedLearnerOnCreate() throws Exception {
        mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "bob",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE", "EXERCISE"]
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUnsafeGenerationPromptFields() throws Exception {
        mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "cheat on exam",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void persistsFailedAgentTaskWhenModelGenerationFails() throws Exception {
        doThrow(new IllegalStateException("provider unavailable"))
                .when(aiModelGateway)
                .generateStructured(org.mockito.ArgumentMatchers.any(AiModelGateway.ModelRequest.class));

        mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"],
                                  "requestId": "req_failed_resource_generation"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        assertThat(resourceGenerationTaskRepository.findByLearnerIdAndRequestId("alice", "req_failed_resource_generation"))
                .hasValueSatisfying(task -> {
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                    assertThat(task.getAgentTaskId()).isNotBlank();
                    assertThat(agentTaskRepository.findById(task.getAgentTaskId()))
                            .hasValueSatisfying(agentTask -> {
                                assertThat(agentTask.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                                assertThat(agentTask.getOutputJson()).contains("MODEL_PROVIDER_ERROR", "\"recoverable\":true");
                                assertThat(agentTask.getOutputJson()).doesNotContain("provider unavailable");
                            });
                    assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(task.getAgentTaskId()))
                            .singleElement()
                            .satisfies(trace -> {
                                assertThat(trace.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                                assertThat(trace.getSummary()).contains("Model call failed", "MODEL_PROVIDER_ERROR");
                                assertThat(trace.getSummary()).doesNotContain("provider unavailable");
                            });
                });
        assertThat(modelCallLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getStatus()).isEqualTo(AgentRuntimeConstants.MODEL_CALL_FAILED);
                    assertThat(log.getErrorMessage()).isEqualTo("MODEL_PROVIDER_ERROR");
                    assertThat(log.getPromptCode()).isEqualTo("resource-generation");
                    assertThat(log.getPromptVersion()).isEqualTo(AgentRuntimeConstants.PROMPT_RESOURCE_V1);
                    assertThat(log.getTemperature()).isEqualTo(0.0);
                    assertThat(log.getStructuredOutputSchema()).contains("ResourceGenerationOutputSchema");
                });
    }

    @Test
    void persistsGatewayModelLatencyAndTokenUsageForSuccessfulResourceGeneration() throws Exception {
        doReturn(modelResponseWithLatency(
                "openai",
                "gpt-test-resource",
                "raw content with sk-live-secret should not be persisted",
                new AiModelGateway.TokenUsage(321, 123, 444, 0.034),
                321L
        ))
                .when(aiModelGateway)
                .generateStructured(org.mockito.ArgumentMatchers.any(AiModelGateway.ModelRequest.class));

        String responseBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"],
                                  "requestId": "req_gateway_success_evidence"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_WAITING_REVIEW))
                .andExpect(jsonPath("$.data.resources[0].markdownContent").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("sk-live-secret", "raw content");
        String traceId = objectMapper.readTree(responseBody).path("data").path("traceId").asText();
        assertThat(modelCallLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getTraceId()).isEqualTo(traceId);
                    assertThat(log.getProvider()).isEqualTo("openai");
                    assertThat(log.getModel()).isEqualTo("gpt-test-resource");
                    assertThat(log.getLatencyMs()).isEqualTo(321L);
                    assertThat(log.getEstimatedCost()).isEqualTo(0.034);
                    assertThat(log.getErrorMessage()).isNull();
                });
        assertThat(tokenUsageLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getPromptTokens()).isEqualTo(321);
                    assertThat(log.getCompletionTokens()).isEqualTo(123);
                    assertThat(log.getTotalTokens()).isEqualTo(444);
                    assertThat(log.getEstimatedCost()).isEqualTo(0.034);
                });
    }

    @Test
    void persistsSafeRecoverableFailureWhenStructuredOutputIsMissingRequiredFields() throws Exception {
        doReturn(new AiModelGateway.ModelResponse(
                "openai",
                "gpt-test-resource",
                AgentRuntimeConstants.MODEL_CALL_SUCCESS,
                "malformed structured output with sk-live-secret should not be persisted",
                Map.of("resources", List.of(Map.of("title", "Missing required fields"))),
                new AiModelGateway.TokenUsage(12, 6, 18, 0.01)
        ))
                .when(aiModelGateway)
                .generateStructured(org.mockito.ArgumentMatchers.any(AiModelGateway.ModelRequest.class));

        String responseBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"],
                                  "requestId": "req_malformed_structured_output"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("sk-live-secret", "malformed structured output", "Missing required fields");
        assertThat(resourceGenerationTaskRepository.findByLearnerIdAndRequestId("alice", "req_malformed_structured_output"))
                .hasValueSatisfying(task -> {
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                    assertThat(task.getLastError()).isEqualTo("MODEL_CALL_FAILED");
                    assertThat(task.getRecoverable()).isTrue();
                    assertThat(agentTaskRepository.findById(task.getAgentTaskId()))
                            .hasValueSatisfying(agentTask -> {
                                assertThat(agentTask.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                                assertThat(agentTask.getOutputJson())
                                        .contains("STRUCTURED_OUTPUT_INVALID")
                                        .doesNotContain("sk-live-secret", "Missing required fields");
                            });
                    assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(task.getAgentTaskId()))
                            .singleElement()
                            .satisfies(trace -> assertThat(trace.getSummary())
                                    .contains("STRUCTURED_OUTPUT_INVALID")
                                    .doesNotContain("sk-live-secret", "Missing required fields"));
                });
        assertThat(modelCallLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getStatus()).isEqualTo(AgentRuntimeConstants.MODEL_CALL_FAILED);
                    assertThat(log.getErrorMessage()).isEqualTo("STRUCTURED_OUTPUT_INVALID");
                    assertThat(log.getStructuredOutputSchema()).contains("ResourceGenerationOutputSchema");
                });
        assertThat(learningResourceRepository.count()).isZero();
        assertThat(resourceReviewRepository.count()).isZero();
    }

    @Test
    void persistsRetryMetadataWhenResourceGenerationFailsRecoverably() throws Exception {
        doThrow(new IllegalStateException("provider unavailable"))
                .when(aiModelGateway)
                .generateStructured(org.mockito.ArgumentMatchers.any(AiModelGateway.ModelRequest.class));

        Instant beforeRequest = Instant.now();

        mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"],
                                  "requestId": "req_failed_retry_metadata"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        String taskId = resourceGenerationTaskRepository
                .findByLearnerIdAndRequestId("alice", "req_failed_retry_metadata")
                .map(task -> {
                    assertThat(task.getStatus()).isEqualTo(AgentRuntimeConstants.STATUS_FAILED);
                    assertThat(task.getProgressPercent()).isZero();
                    assertThat(task.getRetryCount()).isEqualTo(1);
                    assertThat(task.getNextRetryAt()).isNotNull();
                    assertThat(task.getNextRetryAt()).isAfter(beforeRequest);
                    assertThat(task.getLastError()).isEqualTo("MODEL_CALL_FAILED");
                    assertThat(task.getRecoverable()).isTrue();
                    return task.getId();
                })
                .orElseThrow();

        mockMvc.perform(get("/api/resources/generation-tasks/{taskId}", taskId).header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(AgentRuntimeConstants.STATUS_FAILED))
                .andExpect(jsonPath("$.data.progressPercent").value(0))
                .andExpect(jsonPath("$.data.retryCount").value(1))
                .andExpect(jsonPath("$.data.nextRetryAt").isNotEmpty())
                .andExpect(jsonPath("$.data.lastError").value("MODEL_CALL_FAILED"))
                .andExpect(jsonPath("$.data.recoverable").value(true));
    }

    @Test
    void cancelsAgentTaskAndRejectsRepeatedCancellation() throws Exception {
        String taskBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "alice",
                                  "goalId": "goal_spring_boot",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE", "EXERCISE"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String agentTaskId = objectMapper.readTree(taskBody).path("data").path("agentTaskId").asText();

        mockMvc.perform(post("/api/agent/tasks/{taskId}/cancel", agentTaskId)
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "User cancelled duplicated generation."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(agentTaskId))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.steps[7].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.steps[7].summary").value("User cancelled duplicated generation."));

        mockMvc.perform(post("/api/agent/tasks/{taskId}/cancel", agentTaskId)
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Cancel again."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        mockMvc.perform(post("/api/agent/tasks/{taskId}/cancel", agentTaskId)
                        .header("X-User-Id", "bob")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Unauthorized cancel."
                                }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void bearerOwnerCanCancelTaskDespiteSpoofedUserIdHeader() throws Exception {
        String agentTaskId = createAgentTaskForCancelMatrix("alice", "goal_cancel_bearer_owner");

        mockMvc.perform(post("/api/agent/tasks/{taskId}/cancel", agentTaskId)
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "bob")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Bearer owner cancellation."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(agentTaskId))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertThat(agentTaskRepository.findById(agentTaskId))
                .hasValueSatisfying(task -> assertThat(task.getStatus()).isEqualTo("CANCELLED"));
    }

    @Test
    void bearerUserSubjectAdminCannotCancelForeignTaskAndDoesNotMutateTask() throws Exception {
        String agentTaskId = createAgentTaskForCancelMatrix("alice", "goal_cancel_subject_admin");
        String beforeStatus = agentTaskRepository.findById(agentTaskId).orElseThrow().getStatus();
        long beforeTraceCount = agentTraceRepository.countByAgentTaskId(agentTaskId);

        String responseBody = mockMvc.perform(post("/api/agent/tasks/{taskId}/cancel", agentTaskId)
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER")))
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Subject admin must not cancel foreign task."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain(agentTaskId);
        assertAgentTaskUnchanged(agentTaskId, beforeStatus, beforeTraceCount);
    }

    @Test
    void bearerUserSubjectTeacherPrefixCannotCancelForeignTaskAndDoesNotMutateTask() throws Exception {
        String agentTaskId = createAgentTaskForCancelMatrix("alice", "goal_cancel_subject_teacher");
        String beforeStatus = agentTaskRepository.findById(agentTaskId).orElseThrow().getStatus();
        long beforeTraceCount = agentTraceRepository.countByAgentTaskId(agentTaskId);

        String responseBody = mockMvc.perform(post("/api/agent/tasks/{taskId}/cancel", agentTaskId)
                        .header("Authorization", "Bearer " + jwt("teacher_1", "Subject Teacher", List.of("USER")))
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Subject teacher must not cancel foreign task."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain(agentTaskId);
        assertAgentTaskUnchanged(agentTaskId, beforeStatus, beforeTraceCount);
    }

    @Test
    void bearerNonOwnerWithSpoofedOwnerHeaderCannotCancelTaskAndDoesNotAppendTrace() throws Exception {
        String agentTaskId = createAgentTaskForCancelMatrix("alice", "goal_cancel_spoofed_owner");
        String beforeStatus = agentTaskRepository.findById(agentTaskId).orElseThrow().getStatus();
        long beforeTraceCount = agentTraceRepository.countByAgentTaskId(agentTaskId);

        String responseBody = mockMvc.perform(post("/api/agent/tasks/{taskId}/cancel", agentTaskId)
                        .header("Authorization", "Bearer " + jwt("bob", "Bob", List.of("STUDENT")))
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Spoofed owner header must not cancel task."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain(agentTaskId);
        assertAgentTaskUnchanged(agentTaskId, beforeStatus, beforeTraceCount);
    }

    private void assertNoResourceGenerationSideEffects() {
        assertThat(resourceGenerationTaskRepository.count()).isZero();
        assertThat(learningResourceRepository.count()).isZero();
        assertThat(resourceReviewRepository.count()).isZero();
        assertThat(agentTaskRepository.count()).isZero();
        assertThat(agentTraceRepository.count()).isZero();
        assertThat(modelCallLogRepository.count()).isZero();
        assertThat(tokenUsageLogRepository.count()).isZero();
        assertThat(sourceCitationRepository.count()).isZero();
    }

    private String createAgentTaskForCancelMatrix(String ownerUserId, String goalId) throws Exception {
        String taskBody = mockMvc.perform(post("/api/resources/generation-tasks")
                        .header("X-User-Id", ownerUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "learnerId": "%s",
                                  "goalId": "%s",
                                  "pathNodeId": "node_sql_join",
                                  "resourceTypes": ["LECTURE"]
                                }
                                """.formatted(ownerUserId, goalId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(taskBody).path("data").path("agentTaskId").asText();
    }

    private void assertAgentTaskUnchanged(String agentTaskId, String expectedStatus, long expectedTraceCount) {
        assertThat(agentTaskRepository.findById(agentTaskId))
                .hasValueSatisfying(task -> assertThat(task.getStatus()).isEqualTo(expectedStatus));
        assertThat(agentTraceRepository.countByAgentTaskId(agentTaskId)).isEqualTo(expectedTraceCount);
        assertThat(agentTraceRepository.findByAgentTaskIdOrderBySequenceNoAsc(agentTaskId))
                .noneMatch(trace -> "task_cancelled".equals(trace.getStepId()));
    }

    private void approveAllReviewsForTask(String taskId) throws Exception {
        for (var review : resourceReviewRepository.findAll().stream()
                .filter(review -> taskId.equals(review.getGenerationTaskId()))
                .toList()) {
            mockMvc.perform(post("/api/reviews/resources/{reviewId}/decision", review.getId())
                            .header("X-User-Id", "teacher")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "APPROVED",
                                      "summary": "Accurate and ready for learner release."
                                    }
                                    """))
                    .andExpect(status().isOk());
        }
    }

    private void seedCourse(String courseId, String teacherId) {
        if (courseRepository.existsById(courseId)) {
            return;
        }
        Course course = new Course();
        course.setId(courseId);
        course.setTitle("Course " + courseId);
        course.setDescription("Course for resource generation review tests.");
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

    private AiModelGateway.ModelResponse modelResponseWithLatency(
            String provider,
            String model,
            String content,
            AiModelGateway.TokenUsage tokenUsage,
            long latencyMs
    ) throws Exception {
        try {
            Constructor<AiModelGateway.ModelResponse> constructor = AiModelGateway.ModelResponse.class.getConstructor(
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    Map.class,
                    AiModelGateway.TokenUsage.class,
                    long.class
            );
            return constructor.newInstance(
                    provider,
                    model,
                    AgentRuntimeConstants.MODEL_CALL_SUCCESS,
                    content,
                    validStructuredOutput(),
                    tokenUsage,
                    latencyMs
            );
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("ModelResponse should expose gateway latencyMs", exception);
        }
    }

    private Map<String, Object> validStructuredOutput() {
        return Map.of("resources", List.of(Map.of(
                "title", "SQL JOIN remediation",
                "type", "LECTURE",
                "modality", "MARKDOWN",
                "markdownContent", "### SQL JOIN remediation",
                "citationSummary", "COURSE_RAG: 1 persisted source citation.",
                "safetyStatus", AgentRuntimeConstants.SAFETY_NEEDS_REVIEW
        )));
    }
}
