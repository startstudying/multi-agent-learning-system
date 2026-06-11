package com.learningos.analytics.api;

import com.learningos.agent.application.AgentRuntimeConstants;
import com.learningos.agent.domain.AgentTask;
import com.learningos.agent.domain.AgentTrace;
import com.learningos.agent.domain.LearningResource;
import com.learningos.agent.domain.ModelCallLog;
import com.learningos.agent.domain.ResourceGenerationTask;
import com.learningos.agent.domain.ResourceReview;
import com.learningos.agent.domain.TokenUsageLog;
import com.learningos.agent.repository.AgentTaskRepository;
import com.learningos.agent.repository.AgentTraceRepository;
import com.learningos.agent.repository.LearningResourceRepository;
import com.learningos.agent.repository.ModelCallLogRepository;
import com.learningos.agent.repository.ResourceGenerationTaskRepository;
import com.learningos.agent.repository.ResourceReviewRepository;
import com.learningos.assessment.domain.AnswerRecord;
import com.learningos.assessment.domain.WrongQuestion;
import com.learningos.assessment.repository.AnswerRecordRepository;
import com.learningos.assessment.repository.WrongQuestionRepository;
import com.learningos.agent.repository.TokenUsageLogRepository;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.domain.CourseEnrollment;
import com.learningos.knowledge.domain.KnowledgePoint;
import com.learningos.knowledge.repository.CourseEnrollmentRepository;
import com.learningos.knowledge.repository.CourseRepository;
import com.learningos.knowledge.repository.KnowledgePointRepository;
import com.learningos.learning.domain.LearningEvent;
import com.learningos.learning.domain.LearningPath;
import com.learningos.learning.domain.LearningPathNode;
import com.learningos.learning.domain.MasteryRecord;
import com.learningos.learning.repository.LearningEventRepository;
import com.learningos.learning.repository.LearningPathNodeRepository;
import com.learningos.learning.repository.LearningPathRepository;
import com.learningos.learning.repository.MasteryRecordRepository;
import com.learningos.rag.domain.KbQueryLog;
import com.learningos.rag.repository.KbQueryLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class AnalyticsControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;
    private final AnswerRecordRepository answerRecordRepository;
    private final WrongQuestionRepository wrongQuestionRepository;
    private final LearningEventRepository learningEventRepository;
    private final AgentTaskRepository agentTaskRepository;
    private final AgentTraceRepository agentTraceRepository;
    private final ModelCallLogRepository modelCallLogRepository;
    private final TokenUsageLogRepository tokenUsageLogRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathNodeRepository learningPathNodeRepository;
    private final MasteryRecordRepository masteryRecordRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final ResourceGenerationTaskRepository resourceGenerationTaskRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final ResourceReviewRepository resourceReviewRepository;
    private final KbQueryLogRepository kbQueryLogRepository;

    AnalyticsControllerTest(
            MockMvc mockMvc,
            AnswerRecordRepository answerRecordRepository,
            WrongQuestionRepository wrongQuestionRepository,
            LearningEventRepository learningEventRepository,
            AgentTaskRepository agentTaskRepository,
            AgentTraceRepository agentTraceRepository,
            ModelCallLogRepository modelCallLogRepository,
            TokenUsageLogRepository tokenUsageLogRepository,
            LearningPathRepository learningPathRepository,
            LearningPathNodeRepository learningPathNodeRepository,
            MasteryRecordRepository masteryRecordRepository,
            CourseRepository courseRepository,
            CourseEnrollmentRepository courseEnrollmentRepository,
            KnowledgePointRepository knowledgePointRepository,
            ResourceGenerationTaskRepository resourceGenerationTaskRepository,
            LearningResourceRepository learningResourceRepository,
            ResourceReviewRepository resourceReviewRepository,
            KbQueryLogRepository kbQueryLogRepository
    ) {
        this.mockMvc = mockMvc;
        this.answerRecordRepository = answerRecordRepository;
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.learningEventRepository = learningEventRepository;
        this.agentTaskRepository = agentTaskRepository;
        this.agentTraceRepository = agentTraceRepository;
        this.modelCallLogRepository = modelCallLogRepository;
        this.tokenUsageLogRepository = tokenUsageLogRepository;
        this.learningPathRepository = learningPathRepository;
        this.learningPathNodeRepository = learningPathNodeRepository;
        this.masteryRecordRepository = masteryRecordRepository;
        this.courseRepository = courseRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
        this.knowledgePointRepository = knowledgePointRepository;
        this.resourceGenerationTaskRepository = resourceGenerationTaskRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.resourceReviewRepository = resourceReviewRepository;
        this.kbQueryLogRepository = kbQueryLogRepository;
    }

    @Test
    void opsAlertsRequiresAdminAccess() throws Exception {
        mockMvc.perform(get("/api/analytics/ops/alerts").header("X-User-Id", "teacher"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/analytics/ops/alerts"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void opsAlertsReturnsDefaultThresholdsWhenNoSignalsExist() throws Exception {
        mockMvc.perform(get("/api/analytics/ops/alerts").header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.thresholds.slowQueryMs").value(1000))
                .andExpect(jsonPath("$.data.thresholds.slowModelMs").value(2000))
                .andExpect(jsonPath("$.data.thresholds.noSourceRateThreshold").value(0.2))
                .andExpect(jsonPath("$.data.thresholds.noSourceMinCount").value(3))
                .andExpect(jsonPath("$.data.thresholds.reviewBacklogHours").value(24))
                .andExpect(jsonPath("$.data.thresholds.reviewBacklogCount").value(10))
                .andExpect(jsonPath("$.data.alerts.length()").value(0));
    }

    @Test
    void opsAlertsReturnsFourSanitizedTriggeredAlerts() throws Exception {
        seedOpsAlertSignals();

        mockMvc.perform(get("/api/analytics/ops/alerts")
                        .header("X-User-Id", "admin")
                        .param("from", "2026-06-06T00:00:00Z")
                        .param("to", "2026-06-07T00:00:00Z")
                        .param("slowQueryMs", "1000")
                        .param("slowModelMs", "2000")
                        .param("noSourceRateThreshold", "0.5")
                        .param("noSourceMinCount", "3")
                        .param("reviewBacklogHours", "24")
                        .param("reviewBacklogCount", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alerts.length()").value(4))
                .andExpect(jsonPath("$.data.alerts[0].type").value("SLOW_RAG_QUERY"))
                .andExpect(jsonPath("$.data.alerts[0].metrics.slowCount").value(1))
                .andExpect(jsonPath("$.data.alerts[0].metrics.maxLatencyMs").value(1200))
                .andExpect(jsonPath("$.data.alerts[1].type").value("SLOW_MODEL_CALL"))
                .andExpect(jsonPath("$.data.alerts[1].metrics.slowCount").value(1))
                .andExpect(jsonPath("$.data.alerts[1].metrics.maxLatencyMs").value(2500))
                .andExpect(jsonPath("$.data.alerts[2].type").value("RAG_NO_SOURCE"))
                .andExpect(jsonPath("$.data.alerts[2].metrics.noSourceCount").value(3))
                .andExpect(jsonPath("$.data.alerts[2].metrics.totalRagCount").value(4))
                .andExpect(jsonPath("$.data.alerts[3].type").value("REVIEW_BACKLOG"))
                .andExpect(jsonPath("$.data.alerts[3].metrics.backlogCount").value(2))
                .andExpect(content().string(not(containsString("\"question\""))))
                .andExpect(content().string(not(containsString("\"responseJson\""))))
                .andExpect(content().string(not(containsString("\"sourcesJson\""))))
                .andExpect(content().string(not(containsString("\"errorMessage\""))))
                .andExpect(content().string(not(containsString("\"markdownContent\""))))
                .andExpect(content().string(not(containsString("\"citationCheck\""))))
                .andExpect(content().string(not(containsString("\"revisionSuggestion\""))))
                .andExpect(content().string(not(containsString("Sensitive raw RAG question"))))
                .andExpect(content().string(not(containsString("apiKey=sk-test"))))
                .andExpect(content().string(not(containsString("Sensitive draft content must not be exposed"))))
                .andExpect(content().string(not(containsString("Private review citation check"))));
    }

    @Test
    void opsAlertsRejectsInvalidParameters() throws Exception {
        mockMvc.perform(get("/api/analytics/ops/alerts")
                        .header("X-User-Id", "admin")
                        .param("from", "2026-06-07T00:00:00Z")
                        .param("to", "2026-06-06T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/analytics/ops/alerts")
                        .header("X-User-Id", "admin")
                        .param("slowQueryMs", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void tokenBudgetGovernanceGroupsCostStatsByUserCourseAgentTypeAndTimeWindow() throws Exception {
        seedTokenBudgetGovernanceSignals();

        mockMvc.perform(get("/api/analytics/token-budget/governance")
                        .header("X-User-Id", "admin")
                        .param("from", "2026-06-06T00:00:00Z")
                        .param("to", "2026-06-07T00:00:00Z")
                        .param("tokenBudget", "10000")
                        .param("degradeThreshold", "0.8")
                        .param("manualConfirmationThreshold", "1.0")
                        .param("highCostTokenThreshold", "6000")
                        .param("highCostUsdThreshold", "0.05")
                        .param("anomalyLatencyMsThreshold", "1500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.costStats.promptTokens").value(5300))
                .andExpect(jsonPath("$.data.costStats.completionTokens").value(3200))
                .andExpect(jsonPath("$.data.costStats.totalTokens").value(8500))
                .andExpect(jsonPath("$.data.costStats.estimatedCost").value(0.08))
                .andExpect(jsonPath("$.data.costStats.modelCallCount").value(2))
                .andExpect(jsonPath("$.data.costStats.byUser[0].userId").value("alice"))
                .andExpect(jsonPath("$.data.costStats.byUser[0].totalTokens").value(7000))
                .andExpect(jsonPath("$.data.costStats.byUser[1].userId").value("bob"))
                .andExpect(jsonPath("$.data.costStats.byUser[1].totalTokens").value(1500))
                .andExpect(jsonPath("$.data.costStats.byCourse[0].courseId").value("course_backend"))
                .andExpect(jsonPath("$.data.costStats.byCourse[0].totalTokens").value(7000))
                .andExpect(jsonPath("$.data.costStats.byCourse[1].courseId").value("UNKNOWN_COURSE"))
                .andExpect(jsonPath("$.data.costStats.byCourse[1].totalTokens").value(1500))
                .andExpect(jsonPath("$.data.costStats.byAgentType[0].agentType").value(AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION))
                .andExpect(jsonPath("$.data.costStats.byAgentType[0].totalTokens").value(7000))
                .andExpect(jsonPath("$.data.costStats.byAgentType[1].agentType").value("RAG_QA"))
                .andExpect(jsonPath("$.data.costStats.byAgentType[1].totalTokens").value(1500))
                .andExpect(jsonPath("$.data.costStats.byTimeWindow[0].windowStart").value("2026-06-06T00:00:00Z"))
                .andExpect(jsonPath("$.data.costStats.byTimeWindow[0].windowEnd").value("2026-06-07T00:00:00Z"))
                .andExpect(jsonPath("$.data.costStats.byTimeWindow[0].totalTokens").value(8500))
                .andExpect(jsonPath("$.data.budgetDecision.status").value("NEAR_BUDGET"))
                .andExpect(jsonPath("$.data.budgetDecision.action").value("DOWNGRADE_TO_DETERMINISTIC_OR_CACHED_RESPONSE"))
                .andExpect(jsonPath("$.data.budgetDecision.requiresManualConfirmation").value(false))
                .andExpect(jsonPath("$.data.budgetDecision.fallbackStrategy").value("DETERMINISTIC_OR_CACHED_RESPONSE"))
                .andExpect(jsonPath("$.data.budgetDecision.reasonCodes[0]").value("DEGRADE_THRESHOLD_EXCEEDED"));
    }

    @Test
    void tokenBudgetGovernanceRequiresManualConfirmationWhenBudgetIsExceeded() throws Exception {
        seedTokenBudgetGovernanceSignals();

        mockMvc.perform(get("/api/analytics/token-budget/governance")
                        .header("X-User-Id", "admin")
                        .param("from", "2026-06-06T00:00:00Z")
                        .param("to", "2026-06-07T00:00:00Z")
                        .param("tokenBudget", "8000")
                        .param("degradeThreshold", "0.8")
                        .param("manualConfirmationThreshold", "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.budgetDecision.budgetTokens").value(8000))
                .andExpect(jsonPath("$.data.budgetDecision.usedTokens").value(8500))
                .andExpect(jsonPath("$.data.budgetDecision.remainingTokens").value(0))
                .andExpect(jsonPath("$.data.budgetDecision.status").value("OVER_BUDGET"))
                .andExpect(jsonPath("$.data.budgetDecision.action").value("REQUIRE_MANUAL_CONFIRMATION"))
                .andExpect(jsonPath("$.data.budgetDecision.requiresManualConfirmation").value(true))
                .andExpect(jsonPath("$.data.budgetDecision.fallbackStrategy").value("MANUAL_CONFIRMATION_REQUIRED"))
                .andExpect(jsonPath("$.data.budgetDecision.reasonCodes[0]").value("MANUAL_CONFIRMATION_THRESHOLD_EXCEEDED"));
    }

    @Test
    void tokenBudgetGovernanceFlagsHighCostTasksAndAbnormalModelCalls() throws Exception {
        seedTokenBudgetGovernanceSignals();

        mockMvc.perform(get("/api/analytics/token-budget/governance")
                        .header("X-User-Id", "admin")
                        .param("from", "2026-06-06T00:00:00Z")
                        .param("to", "2026-06-07T00:00:00Z")
                        .param("highCostTokenThreshold", "6000")
                        .param("highCostUsdThreshold", "0.05")
                        .param("anomalyLatencyMsThreshold", "1500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.highCostTasks.length()").value(1))
                .andExpect(jsonPath("$.data.highCostTasks[0].agentTaskId").value("agt_budget_high"))
                .andExpect(jsonPath("$.data.highCostTasks[0].userId").value("alice"))
                .andExpect(jsonPath("$.data.highCostTasks[0].courseId").value("course_backend"))
                .andExpect(jsonPath("$.data.highCostTasks[0].agentType").value(AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION))
                .andExpect(jsonPath("$.data.highCostTasks[0].totalTokens").value(7000))
                .andExpect(jsonPath("$.data.highCostTasks[0].estimatedCost").value(0.07))
                .andExpect(jsonPath("$.data.highCostTasks[0].warningLevel").value("WARNING"))
                .andExpect(jsonPath("$.data.highCostTasks[0].reasonCodes[0]").value("HIGH_TOKEN_USAGE"))
                .andExpect(jsonPath("$.data.highCostTasks[0].reasonCodes[1]").value("HIGH_ESTIMATED_COST"))
                .andExpect(jsonPath("$.data.abnormalModelCalls.length()").value(2))
                .andExpect(jsonPath("$.data.abnormalModelCalls[0].agentTaskId").value("agt_budget_failed"))
                .andExpect(jsonPath("$.data.abnormalModelCalls[0].model").value("gpt-broken"))
                .andExpect(jsonPath("$.data.abnormalModelCalls[0].status").value(AgentRuntimeConstants.MODEL_CALL_FAILED))
                .andExpect(jsonPath("$.data.abnormalModelCalls[0].reasonCodes[0]").value("MODEL_CALL_FAILED"))
                .andExpect(jsonPath("$.data.abnormalModelCalls[1].agentTaskId").value("agt_budget_high"))
                .andExpect(jsonPath("$.data.abnormalModelCalls[1].reasonCodes[0]").value("MODEL_CALL_HIGH_LATENCY"));
    }

    @Test
    void overviewAggregatesGovernanceAndLearningCounts() throws Exception {
        mockMvc.perform(post("/api/resources/generation-tasks")
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
                .andExpect(status().isOk());
        tokenUsageLogRepository.findAll().forEach(log -> {
            log.setEstimatedCost(0.015);
            tokenUsageLogRepository.save(log);
        });
        seedLearningSignals();

        mockMvc.perform(get("/api/analytics/overview").header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.agentTaskCount").value(1))
                .andExpect(jsonPath("$.data.modelCallCount").value(1))
                .andExpect(jsonPath("$.data.tokenUsage.promptTokens").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.tokenUsage.completionTokens").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.tokenUsage.totalTokens").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.tokenUsage.estimatedCost").value(0.015))
                .andExpect(jsonPath("$.data.tokenUsage.byAgentTask[0].agentTaskId").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenUsage.byAgentTask[0].promptTokens").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.tokenUsage.byAgentTask[0].completionTokens").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.tokenUsage.byAgentTask[0].totalTokens").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.tokenUsage.byAgentTask[0].estimatedCost").value(0.015))
                .andExpect(jsonPath("$.data.tokenUsage.byModel[0].model").value("deterministic-demo-model"))
                .andExpect(jsonPath("$.data.tokenUsage.byModel[0].totalTokens").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.tokenUsage.byModel[0].estimatedCost").value(0.015))
                .andExpect(jsonPath("$.data.tokenUsage.budget.budgetTokens").value(10000))
                .andExpect(jsonPath("$.data.tokenUsage.budget.usedTokens").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.tokenUsage.budget.remainingTokens").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.tokenUsage.budget.status").value("WITHIN_BUDGET"))
                .andExpect(jsonPath("$.data.tokenUsage.budget.fallbackStrategy").value("ALLOW_MODEL_GATEWAY"))
                .andExpect(jsonPath("$.data.tokenUsage.byUser[0].userId").value("alice"))
                .andExpect(jsonPath("$.data.tokenUsage.byUser[0].totalTokens").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.tokenUsage.byAgentName[0].agentName").value("PlannerAgent"))
                .andExpect(jsonPath("$.data.tokenUsage.byAgentName[0].estimatedCost").value(0.015))
                .andExpect(jsonPath("$.data.answerRecordCount").value(1))
                .andExpect(jsonPath("$.data.wrongQuestionCount").value(1))
                .andExpect(jsonPath("$.data.learningEventCount").value(1))
                .andExpect(jsonPath("$.data.resourceReviewStatusCounts.PENDING_CRITIC").value(5))
                .andExpect(jsonPath("$.data.agentSummary.totalTasks").value(1))
                .andExpect(jsonPath("$.data.agentSummary.successCount").value(0))
                .andExpect(jsonPath("$.data.agentSummary.failureCount").value(0))
                .andExpect(jsonPath("$.data.agentSummary.successRate").value(0.0))
                .andExpect(jsonPath("$.data.agentSummary.failureRate").value(0.0))
                .andExpect(jsonPath("$.data.agentSummary.averageLatencyMs").value(265.0))
                .andExpect(jsonPath("$.data.agentSummary.tokenCost").value(0.015))
                .andExpect(jsonPath("$.data.agentSummary.ragHitRate").value(0.0));
    }

    @Test
    void overviewRejectsNonAdminAccess() throws Exception {
        mockMvc.perform(get("/api/analytics/overview").header("X-User-Id", "teacher"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/analytics/overview").header("X-User-Id", "alice"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void overviewUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        mockMvc.perform(get("/api/analytics/overview")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        mockMvc.perform(get("/api/analytics/overview")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void analyticsAdminOnlyEndpointsRejectBearerUserSubjectAdminRoleConfusion() throws Exception {
        mockMvc.perform(get("/api/analytics/overview")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/analytics/token-budget/governance")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/analytics/ops/alerts")
                        .header("Authorization", "Bearer " + jwt("admin", "Subject Admin", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void tokenBudgetGovernanceUsesBearerAdminRoleAndRejectsSpoofedStudent() throws Exception {
        mockMvc.perform(get("/api/analytics/token-budget/governance")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));

        mockMvc.perform(get("/api/analytics/token-budget/governance")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void tokenBudgetGovernanceRejectsHeaderOnlyNonAdminAccess() throws Exception {
        mockMvc.perform(get("/api/analytics/token-budget/governance")
                        .header("X-User-Id", "teacher"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/analytics/token-budget/governance")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void studentSummaryAggregatesProgressMasteryWrongCausesAndRecommendations() throws Exception {
        seedStudentAnalyticsSignals();

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.learnerId").value("alice"))
                .andExpect(jsonPath("$.data.progress.totalNodes").value(3))
                .andExpect(jsonPath("$.data.progress.doneNodes").value(1))
                .andExpect(jsonPath("$.data.progress.activeNodes").value(1))
                .andExpect(jsonPath("$.data.progress.lockedNodes").value(1))
                .andExpect(jsonPath("$.data.progress.completionRate").value(0.3333333333333333))
                .andExpect(jsonPath("$.data.currentMastery[0].knowledgePointId").value("kp_foundation"))
                .andExpect(jsonPath("$.data.currentMastery[0].mastery").value(0.86))
                .andExpect(jsonPath("$.data.currentMastery[1].knowledgePointId").value("kp_sql_join"))
                .andExpect(jsonPath("$.data.currentMastery[1].mastery").value(0.58))
                .andExpect(jsonPath("$.data.currentMastery[1].sourceType").value("ASSESSMENT"))
                .andExpect(jsonPath("$.data.masteryTrend[0].knowledgePointId").value("kp_sql_join"))
                .andExpect(jsonPath("$.data.masteryTrend[0].mastery").value(0.42))
                .andExpect(jsonPath("$.data.masteryTrend[1].knowledgePointId").value("kp_sql_join"))
                .andExpect(jsonPath("$.data.masteryTrend[1].mastery").value(0.58))
                .andExpect(jsonPath("$.data.recentWrongCauses[0].knowledgePointId").value("kp_sql_join"))
                .andExpect(jsonPath("$.data.recentWrongCauses[0].questionId").value("q_sql_join"))
                .andExpect(jsonPath("$.data.recentWrongCauses[0].score").value(0.4))
                .andExpect(jsonPath("$.data.recentWrongCauses[0].causeAnalysis").value("Missed one-to-many cardinality."))
                .andExpect(jsonPath("$.data.recommendedNextSteps[0].type").value("PATH_NODE"))
                .andExpect(jsonPath("$.data.recommendedNextSteps[0].knowledgePointId").value("kp_sql_join"))
                .andExpect(jsonPath("$.data.recommendedNextSteps[0].title").value("SQL JOIN remediation"));
    }

    @Test
    void studentSummaryRejectsCrossLearnerAccess() throws Exception {
        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "bob")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherStudentSummaryRequiresCourseId() throws Exception {
        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("X-User-Id", "teacher"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void teacherCanReadCourseScopedStudentSummaryForActiveEnrolledLearner() throws Exception {
        seedCourseScopedStudentSummarySignals();

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("X-User-Id", "teacher")
                        .param("courseId", "course_backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.learnerId").value("alice"))
                .andExpect(jsonPath("$.data.progress.totalNodes").value(1))
                .andExpect(jsonPath("$.data.progress.activeNodes").value(1))
                .andExpect(jsonPath("$.data.currentMastery.length()").value(1))
                .andExpect(jsonPath("$.data.currentMastery[0].knowledgePointId").value("kp_backend_join"))
                .andExpect(jsonPath("$.data.masteryTrend.length()").value(2))
                .andExpect(jsonPath("$.data.masteryTrend[0].knowledgePointId").value("kp_backend_join"))
                .andExpect(jsonPath("$.data.masteryTrend[1].knowledgePointId").value("kp_backend_join"))
                .andExpect(jsonPath("$.data.recentWrongCauses.length()").value(1))
                .andExpect(jsonPath("$.data.recentWrongCauses[0].knowledgePointId").value("kp_backend_join"))
                .andExpect(jsonPath("$.data.recommendedNextSteps[0].knowledgePointId").value("kp_backend_join"))
                .andExpect(content().string(not(containsString("kp_foreign_math"))))
                .andExpect(content().string(not(containsString("Foreign algebra weakness"))));
    }

    @Test
    void teacherCannotReadStudentSummaryForForeignCourseOrUnenrolledLearner() throws Exception {
        seedCourseScopedStudentSummarySignals();

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("X-User-Id", "other_teacher")
                        .param("courseId", "course_backend"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "charlie")
                        .header("X-User-Id", "teacher")
                        .param("courseId", "course_backend"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void studentCourseScopedSummaryRejectsUnenrolledCourse() throws Exception {
        seedCourseScopedStudentSummarySignals();

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("X-User-Id", "alice")
                        .param("courseId", "course_foreign"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanReadGlobalAndCourseScopedStudentSummary() throws Exception {
        seedCourseScopedStudentSummarySignals();

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.progress.totalNodes").value(2));

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("X-User-Id", "admin")
                        .param("courseId", "course_backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.progress.totalNodes").value(1))
                .andExpect(content().string(not(containsString("kp_foreign_math"))));

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("X-User-Id", "admin")
                        .param("courseId", "missing_course"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void studentSummaryUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        seedCourseScopedStudentSummarySignals();

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice")
                        .param("courseId", "course_backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.learnerId").value("alice"))
                .andExpect(jsonPath("$.data.progress.totalNodes").value(1))
                .andExpect(jsonPath("$.data.currentMastery[0].knowledgePointId").value("kp_backend_join"))
                .andExpect(content().string(not(containsString("kp_foreign_math"))));
    }

    @Test
    void studentSummaryBearerAdminMissingCourseRemainsNotFound() throws Exception {
        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice")
                        .param("courseId", "missing_course"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void studentSummaryBearerStudentWithSpoofedAdminHeaderRemainsOwnerOnly() throws Exception {
        seedCourseScopedStudentSummarySignals();

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "admin")
                        .param("courseId", "course_backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.learnerId").value("alice"))
                .andExpect(jsonPath("$.data.progress.totalNodes").value(1))
                .andExpect(jsonPath("$.data.currentMastery[0].knowledgePointId").value("kp_backend_join"))
                .andExpect(content().string(not(containsString("kp_foreign_math"))));

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "bob")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "admin")
                        .param("courseId", "course_backend"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(content().string(not(containsString("course_backend"))));

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "admin")
                        .param("courseId", "course_foreign"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(content().string(not(containsString("course_foreign"))));
    }

    @Test
    void bearerTeacherCanReadCourseScopedStudentSummaryForOwnCourseWithoutTeacherIdPrefix() throws Exception {
        seedCourse("course_instructor_summary", "Instructor course", "instructor_1");
        seedEnrollment("course_instructor_summary", "alice", "ACTIVE");
        seedKnowledgePoint("kp_instructor_summary", "course_instructor_summary", "Instructor topic", 0.4);
        seedLearningPath("path_alice_instructor_summary", "alice", "course_instructor_summary");
        seedClassPathNode("node_alice_instructor_summary", "path_alice_instructor_summary", "alice",
                "kp_instructor_summary", "Instructor topic", "ACTIVE", 0.5, 1,
                "Instructor remediation.");

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Instructor", List.of("TEACHER")))
                        .param("courseId", "course_instructor_summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.learnerId").value("alice"))
                .andExpect(jsonPath("$.data.progress.totalNodes").value(1))
                .andExpect(jsonPath("$.data.currentMastery[0].knowledgePointId").value("kp_instructor_summary"));
    }

    @Test
    void bearerTeacherStudentSummaryRejectsDroppedLearnerInOwnCourseWithoutLeakingScope() throws Exception {
        seedCourse("course_dropped_summary_scope", "Dropped summary course", "instructor_1");
        seedEnrollment("course_dropped_summary_scope", "charlie", "DROPPED");
        seedKnowledgePoint("kp_dropped_summary", "course_dropped_summary_scope", "Dropped summary topic", 0.4);
        seedLearningPath("path_charlie_dropped_summary", "charlie", "course_dropped_summary_scope");
        seedClassPathNode("node_charlie_dropped_summary", "path_charlie_dropped_summary", "charlie",
                "kp_dropped_summary", "Dropped summary topic", "ACTIVE", 0.25, 1,
                "Dropped learner path signal must not leak.");
        seedWrongQuestion("wrong_charlie_dropped_summary", "charlie", "q_charlie_dropped_summary",
                "kp_dropped_summary", 0.1, "Dropped learner wrong cause must not leak.");
        seedResourceTask("task_charlie_dropped_summary", "charlie", "course_dropped_summary_scope",
                "node_charlie_dropped_summary", AgentRuntimeConstants.STATUS_DONE,
                AgentRuntimeConstants.REVIEW_PUBLISHED, 100);

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "charlie")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Instructor", List.of("TEACHER")))
                        .param("courseId", "course_dropped_summary_scope"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(content().string(not(containsString("course_dropped_summary_scope"))))
                .andExpect(content().string(not(containsString("kp_dropped_summary"))))
                .andExpect(content().string(not(containsString("path_charlie_dropped_summary"))))
                .andExpect(content().string(not(containsString("Dropped learner wrong cause must not leak"))))
                .andExpect(content().string(not(containsString("task_charlie_dropped_summary"))));
    }

    @Test
    void bearerUserSubjectTeacherPrefixCannotReadCourseScopedStudentSummaryAsTeacher() throws Exception {
        seedCourse("course_teacher_prefix_summary", "Teacher prefix course", "teacher_1");
        seedKnowledgePoint("kp_teacher_prefix_summary", "course_teacher_prefix_summary", "Teacher prefix topic", 0.4);
        seedLearningPath("path_teacher_prefix_summary", "teacher_1", "course_teacher_prefix_summary");
        seedClassPathNode("node_teacher_prefix_summary", "path_teacher_prefix_summary", "teacher_1",
                "kp_teacher_prefix_summary", "Teacher prefix topic", "ACTIVE", 0.5, 1,
                "This must not be visible through subject-name role confusion.");

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "teacher_1")
                        .header("Authorization", "Bearer " + jwt("teacher_1", "Teacher Prefix", List.of("USER")))
                        .param("courseId", "course_teacher_prefix_summary"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void bearerTeacherCannotDistinguishMissingCourseFromForbiddenStudentSummaryCourse() throws Exception {
        seedCourseScopedStudentSummarySignals();

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Instructor", List.of("TEACHER")))
                        .param("courseId", "missing_course"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(content().string(not(containsString("missing_course"))));

        mockMvc.perform(get("/api/analytics/students/{learnerId}/summary", "alice")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Instructor", List.of("TEACHER")))
                        .param("courseId", "course_foreign"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(content().string(not(containsString("course_foreign"))));
    }

    @Test
    void teacherClassSummaryAggregatesWeakKnowledgeWrongCausesResourceCompletionAndPendingReviews() throws Exception {
        seedTeacherClassAnalyticsSignals();

        mockMvc.perform(get("/api/analytics/classes/{courseId}/summary", "course_backend")
                        .header("X-User-Id", "teacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.courseId").value("course_backend"))
                .andExpect(jsonPath("$.data.teacherId").value("teacher"))
                .andExpect(jsonPath("$.data.learnerCount").value(2))
                .andExpect(jsonPath("$.data.weakKnowledgePoints[0].knowledgePointId").value("kp_sql_join"))
                .andExpect(jsonPath("$.data.weakKnowledgePoints[0].title").value("SQL JOIN"))
                .andExpect(jsonPath("$.data.weakKnowledgePoints[0].averageMastery").value(0.5))
                .andExpect(jsonPath("$.data.weakKnowledgePoints[0].wrongQuestionCount").value(2))
                .andExpect(jsonPath("$.data.weakKnowledgePoints[0].affectedLearnerCount").value(2))
                .andExpect(jsonPath("$.data.weakKnowledgePoints[0].topCause").value("Missed join cardinality."))
                .andExpect(jsonPath("$.data.wrongCauseDistribution[0].knowledgePointId").value("kp_sql_join"))
                .andExpect(jsonPath("$.data.wrongCauseDistribution[0].causeAnalysis").value("Missed join cardinality."))
                .andExpect(jsonPath("$.data.wrongCauseDistribution[0].count").value(2))
                .andExpect(jsonPath("$.data.resourceCompletion.totalTasks").value(3))
                .andExpect(jsonPath("$.data.resourceCompletion.doneTasks").value(1))
                .andExpect(jsonPath("$.data.resourceCompletion.waitingReviewTasks").value(1))
                .andExpect(jsonPath("$.data.resourceCompletion.failedTasks").value(1))
                .andExpect(jsonPath("$.data.resourceCompletion.averageProgressPercent").value(60.0))
                .andExpect(jsonPath("$.data.resourceCompletion.completionRate").value(0.3333333333333333))
                .andExpect(jsonPath("$.data.pendingReviews.length()").value(1))
                .andExpect(jsonPath("$.data.pendingReviews[0].reviewId").value("review_waiting"))
                .andExpect(jsonPath("$.data.pendingReviews[0].resourceId").value("res_waiting"))
                .andExpect(jsonPath("$.data.pendingReviews[0].generationTaskId").value("task_waiting"))
                .andExpect(jsonPath("$.data.pendingReviews[0].status").value("PENDING_CRITIC"))
                .andExpect(jsonPath("$.data.pendingReviews[0].reviewerType").value("CriticAgent"))
                .andExpect(jsonPath("$.data.pendingReviews[0].resourceTitle").value("SQL JOIN remediation"))
                .andExpect(jsonPath("$.data.pendingReviews[0].resourceType").value("EXERCISE"))
                .andExpect(jsonPath("$.data.pendingReviews[0].markdownContent").doesNotExist());
    }

    @Test
    void teacherClassSummaryPendingReviewsRedactsForeignCourseReviews() throws Exception {
        seedTeacherClassAnalyticsSignals();
        seedCourse("course_foreign_reviews", "Foreign review course", "other_teacher");
        seedEnrollment("course_foreign_reviews", "mallory", "ACTIVE");
        seedKnowledgePoint("kp_foreign_reviews", "course_foreign_reviews", "Foreign review topic", 0.4);
        seedLearningPath("path_mallory_foreign_reviews", "mallory", "course_foreign_reviews");
        seedClassPathNode("node_mallory_foreign_reviews", "path_mallory_foreign_reviews", "mallory",
                "kp_foreign_reviews", "Foreign review topic", "ACTIVE", 0.3, 1,
                "Foreign review path must not leak.");
        seedResourceTask("task_foreign_review", "mallory", "course_foreign_reviews", "node_mallory_foreign_reviews",
                AgentRuntimeConstants.STATUS_WAITING_REVIEW, AgentRuntimeConstants.REVIEW_PENDING_CRITIC, 50);
        seedLearningResource("res_foreign_review", "task_foreign_review", "mallory", "EXERCISE",
                "Foreign review resource", AgentRuntimeConstants.REVIEW_PENDING_CRITIC);
        seedResourceReview("review_foreign_review", "task_foreign_review", "res_foreign_review", "CriticAgent",
                AgentRuntimeConstants.REVIEW_PENDING_CRITIC);

        String body = mockMvc.perform(get("/api/analytics/classes/{courseId}/summary", "course_backend")
                        .header("Authorization", "Bearer " + jwt("teacher", "Teacher", List.of("TEACHER")))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.courseId").value("course_backend"))
                .andExpect(jsonPath("$.data.pendingReviews.length()").value(1))
                .andExpect(jsonPath("$.data.pendingReviews[0].reviewId").value("review_waiting"))
                .andExpect(jsonPath("$.data.pendingReviews[0].generationTaskId").value("task_waiting"))
                .andExpect(jsonPath("$.data.pendingReviews[0].markdownContent").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain("review_foreign_review")
                .doesNotContain("task_foreign_review")
                .doesNotContain("res_foreign_review")
                .doesNotContain("course_foreign_reviews")
                .doesNotContain("Foreign review resource");
    }

    @Test
    void teacherClassSummaryRejectsForeignTeacherAndStudent() throws Exception {
        seedTeacherClassAnalyticsSignals();

        mockMvc.perform(get("/api/analytics/classes/{courseId}/summary", "course_backend")
                        .header("X-User-Id", "other_teacher"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/analytics/classes/{courseId}/summary", "course_backend")
                        .header("X-User-Id", "alice"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanReadTeacherClassSummary() throws Exception {
        seedTeacherClassAnalyticsSignals();

        mockMvc.perform(get("/api/analytics/classes/{courseId}/summary", "course_backend")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.courseId").value("course_backend"))
                .andExpect(jsonPath("$.data.learnerCount").value(2));
    }

    @Test
    void teacherClassSummaryUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
        seedTeacherClassAnalyticsSignals();

        mockMvc.perform(get("/api/analytics/classes/{courseId}/summary", "course_backend")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.courseId").value("course_backend"))
                .andExpect(jsonPath("$.data.learnerCount").value(2));
    }

    @Test
    void teacherClassSummaryAllowsBearerTeacherWhenSubjectOwnsCourse() throws Exception {
        seedTeacherClassAnalyticsSignals();

        mockMvc.perform(get("/api/analytics/classes/{courseId}/summary", "course_backend")
                        .header("Authorization", "Bearer " + jwt("teacher", "Teacher", List.of("TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.courseId").value("course_backend"))
                .andExpect(jsonPath("$.data.teacherId").value("teacher"));
    }

    @Test
    void teacherClassSummaryRejectsBearerStudentWithSpoofedAdminHeader() throws Exception {
        seedTeacherClassAnalyticsSignals();

        mockMvc.perform(get("/api/analytics/classes/{courseId}/summary", "course_backend")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT")))
                        .header("X-User-Id", "admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void bearerUserSubjectTeacherPrefixCannotReadClassSummaryForSubjectOwnedCourse() throws Exception {
        seedCourse("course_user_subject_teacher_class_summary", "Subject teacher class summary", "teacher_1");
        seedKnowledgePoint("kp_user_subject_teacher_class_summary", "course_user_subject_teacher_class_summary",
                "Subject teacher class topic", 0.4);
        seedEnrollment("course_user_subject_teacher_class_summary", "alice", "ACTIVE");

        String body = mockMvc.perform(get("/api/analytics/classes/{courseId}/summary",
                        "course_user_subject_teacher_class_summary")
                        .header("Authorization", "Bearer " + jwt("teacher_1", "Subject Teacher", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain("course_user_subject_teacher_class_summary")
                .doesNotContain("Subject teacher class topic");
    }

    @Test
    void teacherClassSummaryReturnsEmptyAggregatesForCourseWithoutLearnerSignals() throws Exception {
        seedCourse("course_empty", "Empty course", "teacher");
        seedKnowledgePoint("kp_empty", "course_empty", "No learner signal", 0.2);

        mockMvc.perform(get("/api/analytics/classes/{courseId}/summary", "course_empty")
                        .header("X-User-Id", "teacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.courseId").value("course_empty"))
                .andExpect(jsonPath("$.data.learnerCount").value(0))
                .andExpect(jsonPath("$.data.weakKnowledgePoints.length()").value(0))
                .andExpect(jsonPath("$.data.wrongCauseDistribution.length()").value(0))
                .andExpect(jsonPath("$.data.resourceCompletion.totalTasks").value(0))
                .andExpect(jsonPath("$.data.resourceCompletion.completionRate").value(0.0))
                .andExpect(jsonPath("$.data.pendingReviews.length()").value(0));
    }

    @Test
    void teacherClassSummaryLearnerSetComesFromActiveEnrollmentNotLegacyLearningPaths() throws Exception {
        seedCourse("course_enrollment_scope", "Enrollment scope course", "teacher");
        seedKnowledgePoint("kp_enrollment_scope", "course_enrollment_scope", "Enrollment scoped knowledge", 0.4);
        seedEnrollment("course_enrollment_scope", "alice", "ACTIVE");
        seedEnrollment("course_enrollment_scope", "bob", "DROPPED");
        seedLearningPath("path_alice_enrollment_scope", "alice", "course_enrollment_scope");
        seedLearningPath("path_bob_legacy_scope", "bob", "course_enrollment_scope");
        seedClassPathNode("node_alice_scope", "path_alice_enrollment_scope", "alice", "kp_enrollment_scope",
                "Enrollment scoped knowledge", "ACTIVE", 0.45, 1, "Active enrolled learner signal.");
        seedClassPathNode("node_bob_legacy_scope", "path_bob_legacy_scope", "bob", "kp_enrollment_scope",
                "Enrollment scoped knowledge", "ACTIVE", 0.20, 1, "Legacy path must not define membership.");
        seedWrongQuestion("wrong_alice_scope", "alice", "q_alice_scope", "kp_enrollment_scope", 0.4,
                "Active enrolled learner wrong cause.");
        seedWrongQuestion("wrong_bob_legacy_scope", "bob", "q_bob_scope", "kp_enrollment_scope", 0.1,
                "Dropped learner wrong cause must be ignored.");
        seedResourceTask("task_alice_scope", "alice", "course_enrollment_scope", "node_alice_scope",
                AgentRuntimeConstants.STATUS_DONE, AgentRuntimeConstants.REVIEW_PUBLISHED, 100);
        seedResourceTask("task_bob_legacy_scope", "bob", "course_enrollment_scope", "node_bob_legacy_scope",
                AgentRuntimeConstants.STATUS_FAILED, AgentRuntimeConstants.REVIEW_REJECTED, 20);

        mockMvc.perform(get("/api/analytics/classes/{courseId}/summary", "course_enrollment_scope")
                        .header("X-User-Id", "teacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learnerCount").value(1))
                .andExpect(jsonPath("$.data.weakKnowledgePoints[0].affectedLearnerCount").value(1))
                .andExpect(jsonPath("$.data.weakKnowledgePoints[0].wrongQuestionCount").value(1))
                .andExpect(jsonPath("$.data.wrongCauseDistribution.length()").value(1))
                .andExpect(jsonPath("$.data.wrongCauseDistribution[0].count").value(1))
                .andExpect(jsonPath("$.data.resourceCompletion.totalTasks").value(1))
                .andExpect(jsonPath("$.data.resourceCompletion.failedTasks").value(0));
    }

    @Test
    void bearerTeacherClassSummaryIgnoresLegacySignalsForDroppedAndNeverEnrolledLearners() throws Exception {
        seedCourse("course_bearer_membership_scope", "Bearer membership scope course", "instructor_1");
        seedKnowledgePoint("kp_bearer_membership_scope", "course_bearer_membership_scope",
                "Bearer membership topic", 0.4);
        seedEnrollment("course_bearer_membership_scope", "alice", "ACTIVE");
        seedEnrollment("course_bearer_membership_scope", "bob", "DROPPED");
        seedLearningPath("path_alice_bearer_scope", "alice", "course_bearer_membership_scope");
        seedLearningPath("path_bob_bearer_scope", "bob", "course_bearer_membership_scope");
        seedLearningPath("path_eve_bearer_scope", "eve", "course_bearer_membership_scope");
        seedClassPathNode("node_alice_bearer_scope", "path_alice_bearer_scope", "alice",
                "kp_bearer_membership_scope", "Bearer membership topic", "ACTIVE", 0.45, 1,
                "Active learner signal.");
        seedClassPathNode("node_bob_bearer_scope", "path_bob_bearer_scope", "bob",
                "kp_bearer_membership_scope", "Bearer membership topic", "ACTIVE", 0.20, 1,
                "Dropped learner signal must be ignored.");
        seedClassPathNode("node_eve_bearer_scope", "path_eve_bearer_scope", "eve",
                "kp_bearer_membership_scope", "Bearer membership topic", "ACTIVE", 0.10, 1,
                "Never enrolled learner signal must be ignored.");
        seedWrongQuestion("wrong_alice_bearer_scope", "alice", "q_alice_bearer_scope",
                "kp_bearer_membership_scope", 0.4, "Active learner cause.");
        seedWrongQuestion("wrong_bob_bearer_scope", "bob", "q_bob_bearer_scope",
                "kp_bearer_membership_scope", 0.1, "Dropped learner cause must be ignored.");
        seedWrongQuestion("wrong_eve_bearer_scope", "eve", "q_eve_bearer_scope",
                "kp_bearer_membership_scope", 0.1, "Never enrolled learner cause must be ignored.");
        seedResourceTask("task_alice_bearer_scope", "alice", "course_bearer_membership_scope",
                "node_alice_bearer_scope", AgentRuntimeConstants.STATUS_DONE,
                AgentRuntimeConstants.REVIEW_PUBLISHED, 100);
        seedResourceTask("task_bob_bearer_scope", "bob", "course_bearer_membership_scope",
                "node_bob_bearer_scope", AgentRuntimeConstants.STATUS_FAILED,
                AgentRuntimeConstants.REVIEW_REJECTED, 20);
        seedResourceTask("task_eve_bearer_scope", "eve", "course_bearer_membership_scope",
                "node_eve_bearer_scope", AgentRuntimeConstants.STATUS_FAILED,
                AgentRuntimeConstants.REVIEW_REJECTED, 10);

        String body = mockMvc.perform(get("/api/analytics/classes/{courseId}/summary",
                        "course_bearer_membership_scope")
                        .header("Authorization", "Bearer " + jwt("instructor_1", "Instructor", List.of("TEACHER")))
                        .header("X-User-Id", "teacher_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.learnerCount").value(1))
                .andExpect(jsonPath("$.data.weakKnowledgePoints[0].affectedLearnerCount").value(1))
                .andExpect(jsonPath("$.data.weakKnowledgePoints[0].wrongQuestionCount").value(1))
                .andExpect(jsonPath("$.data.wrongCauseDistribution.length()").value(1))
                .andExpect(jsonPath("$.data.wrongCauseDistribution[0].count").value(1))
                .andExpect(jsonPath("$.data.resourceCompletion.totalTasks").value(1))
                .andExpect(jsonPath("$.data.resourceCompletion.failedTasks").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain("Dropped learner cause must be ignored")
                .doesNotContain("Never enrolled learner cause must be ignored")
                .doesNotContain("task_bob_bearer_scope")
                .doesNotContain("task_eve_bearer_scope");
    }

    @Test
    void teacherClassSummaryReturnsForbiddenForNonAdminMissingAndForeignCourse() throws Exception {
        seedTeacherClassAnalyticsSignals();

        mockMvc.perform(get("/api/analytics/classes/{courseId}/summary", "missing_course")
                        .header("Authorization", "Bearer " + jwt("teacher", "Teacher", List.of("TEACHER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/analytics/classes/{courseId}/summary", "course_backend")
                        .header("Authorization", "Bearer " + jwt("other_teacher", "Other Teacher", List.of("TEACHER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void teacherClassSummaryAdminMissingCourseRemainsNotFound() throws Exception {
        mockMvc.perform(get("/api/analytics/classes/{courseId}/summary", "missing_course")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .header("X-User-Id", "alice"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private void seedLearningSignals() {
        AnswerRecord answer = new AnswerRecord();
        answer.setLearnerId("alice");
        answer.setQuestionId("q_sql_join");
        answer.setAnswer("One-to-many joins duplicate parent rows.");
        answer.setSafetyStatus("APPROVED");
        answer.setTraceId("trc_answer");
        answerRecordRepository.save(answer);

        WrongQuestion wrongQuestion = new WrongQuestion();
        wrongQuestion.setLearnerId("alice");
        wrongQuestion.setQuestionId("q_sql_join");
        wrongQuestion.setAnswerId(answer.getId());
        wrongQuestion.setGradingResultId("gr_1");
        wrongQuestion.setKnowledgePointId("kp_sql_join");
        wrongQuestion.setScore(0.4);
        wrongQuestion.setCauseAnalysis("Missed one-to-many cardinality.");
        wrongQuestion.setResourcePushStrategy("Push JOIN remediation resource.");
        wrongQuestion.setTraceId("trc_answer");
        wrongQuestionRepository.save(wrongQuestion);

        LearningEvent event = new LearningEvent();
        event.setLearnerId("alice");
        event.setEventType("TUTOR_ASKED");
        event.setSubjectId("session_1");
        event.setSummary("Learner asked about JOIN duplication.");
        event.setPayloadJson("{\"question\":\"Why do joins duplicate rows?\"}");
        event.setTraceId("trc_tutor");
        learningEventRepository.save(event);
    }

    private void seedOpsAlertSignals() {
        seedKbQueryLog("qry_alert_slow", "trc_slow_query", 1200L, 2,
                "Sensitive raw RAG question with secret token",
                "{\"answer\":\"Sensitive generated answer\"}",
                "[{\"source\":\"private chunk\"}]",
                "2026-06-06T02:00:00Z");
        seedKbQueryLog("qry_alert_no_source_1", "trc_no_source_1", 100L, 0,
                "Sensitive raw RAG question 1",
                "{\"answer\":\"NO_SOURCE_REFUSAL\"}",
                "[]",
                "2026-06-06T03:00:00Z");
        seedKbQueryLog("qry_alert_no_source_2", "trc_no_source_2", 110L, 0,
                "Sensitive raw RAG question 2",
                "{\"answer\":\"NO_SOURCE_REFUSAL\"}",
                "[]",
                "2026-06-06T03:05:00Z");
        seedKbQueryLog("qry_alert_no_source_3", "trc_no_source_3", 120L, 0,
                "Sensitive raw RAG question 3",
                "{\"answer\":\"NO_SOURCE_REFUSAL\"}",
                "[]",
                "2026-06-06T03:10:00Z");

        seedBudgetTask(
                "agt_ops_slow_model",
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_DONE,
                "trc_ops_slow_model",
                "2026-06-06T04:00:00Z",
                2500L
        );
        ModelCallLog modelCall = new ModelCallLog();
        modelCall.setId("mcl_ops_slow");
        modelCall.setTraceId("trc_ops_slow_model");
        modelCall.setAgentTaskId("agt_ops_slow_model");
        modelCall.setModel("gpt-alert-demo");
        modelCall.setStatus(AgentRuntimeConstants.MODEL_CALL_SUCCESS);
        modelCall.setLatencyMs(2500L);
        modelCall.setEstimatedCost(0.01);
        modelCall.setErrorMessage("provider timeout apiKey=sk-test jdbc:mysql://secret");
        DirectFieldAccessor modelFields = new DirectFieldAccessor(modelCall);
        modelFields.setPropertyValue("createdAt", Instant.parse("2026-06-06T04:00:00Z"));
        modelCallLogRepository.save(modelCall);

        seedResourceTask("task_ops_backlog_1", "alice", "course_backend", "node_ops_1",
                AgentRuntimeConstants.STATUS_WAITING_REVIEW, AgentRuntimeConstants.REVIEW_PENDING_CRITIC, 60);
        seedLearningResource("res_ops_backlog_1", "task_ops_backlog_1", "alice", "EXERCISE",
                "Ops review resource 1", AgentRuntimeConstants.REVIEW_PENDING_CRITIC);
        seedBacklogReview("review_ops_backlog_1", "task_ops_backlog_1", "res_ops_backlog_1",
                "2026-06-05T00:00:00Z");

        seedResourceTask("task_ops_backlog_2", "alice", "course_backend", "node_ops_2",
                AgentRuntimeConstants.STATUS_WAITING_REVIEW, AgentRuntimeConstants.REVIEW_REVISION_REQUESTED, 60);
        seedLearningResource("res_ops_backlog_2", "task_ops_backlog_2", "alice", "LECTURE",
                "Ops review resource 2", AgentRuntimeConstants.REVIEW_REVISION_REQUESTED);
        seedBacklogReview("review_ops_backlog_2", "task_ops_backlog_2", "res_ops_backlog_2",
                "2026-06-05T01:00:00Z");
    }

    private void seedKbQueryLog(
            String id,
            String traceId,
            Long latencyMs,
            Integer retrievalCount,
            String question,
            String responseJson,
            String sourcesJson,
            String createdAt
    ) {
        KbQueryLog log = new KbQueryLog();
        DirectFieldAccessor fields = new DirectFieldAccessor(log);
        fields.setPropertyValue("id", id);
        fields.setPropertyValue("traceId", traceId);
        fields.setPropertyValue("userId", "alice");
        fields.setPropertyValue("kbIdsJson", "[\"kb_ops\"]");
        fields.setPropertyValue("question", question);
        fields.setPropertyValue("requestId", id + "_request");
        fields.setPropertyValue("requestHash", id + "_hash");
        fields.setPropertyValue("responseJson", responseJson);
        fields.setPropertyValue("retrievalCount", retrievalCount);
        fields.setPropertyValue("rerankerStatus", "SKIPPED");
        fields.setPropertyValue("sourcesJson", sourcesJson);
        fields.setPropertyValue("latencyMs", latencyMs);
        fields.setPropertyValue("createdAt", Instant.parse(createdAt));
        kbQueryLogRepository.save(log);
    }

    private void seedBacklogReview(String id, String generationTaskId, String resourceId, String createdAt) {
        ResourceReview review = new ResourceReview();
        review.setId(id);
        review.setGenerationTaskId(generationTaskId);
        review.setResourceId(resourceId);
        review.setReviewerType("CriticAgent");
        review.setStatus(AgentRuntimeConstants.REVIEW_PENDING_CRITIC);
        review.setSummary("Awaiting review.");
        review.setReason("Private review reason must not be exposed.");
        review.setCitationCheck("Private review citation check must not be exposed.");
        review.setSafetyCheck("Private review safety check must not be exposed.");
        review.setRevisionSuggestion("Private revisionSuggestion must not be exposed.");
        review.setTraceId("trace_" + generationTaskId);
        DirectFieldAccessor fields = new DirectFieldAccessor(review);
        fields.setPropertyValue("createdAt", Instant.parse(createdAt));
        resourceReviewRepository.save(review);
    }

    private void seedTokenBudgetGovernanceSignals() {
        seedBudgetTask(
                "agt_budget_high",
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_DONE,
                "trc_budget_high",
                "2026-06-06T02:00:00Z",
                2400L
        );
        seedBudgetTask(
                "agt_budget_failed",
                "bob",
                "RAG_QA",
                AgentRuntimeConstants.STATUS_FAILED,
                "trc_budget_failed",
                "2026-06-06T03:00:00Z",
                40L
        );
        seedBudgetTask(
                "agt_budget_old",
                "alice",
                AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION,
                AgentRuntimeConstants.STATUS_DONE,
                "trc_budget_old",
                "2026-06-05T03:00:00Z",
                30L
        );
        seedBudgetResourceTask("rgt_budget_high", "agt_budget_high", "alice", "course_backend", "node_budget_high");
        seedBudgetTrace("atr_budget_high", "agt_budget_high", "ResourceAgent", "trc_budget_high");
        seedBudgetTrace("atr_budget_failed", "agt_budget_failed", "CourseRagAgent", "trc_budget_failed");
        seedModelAndTokenUsage(
                "mcl_budget_high",
                "tul_budget_high",
                "agt_budget_high",
                "trc_budget_high",
                "deterministic-demo-model",
                AgentRuntimeConstants.MODEL_CALL_SUCCESS,
                4500,
                2500,
                0.07,
                2400L,
                "2026-06-06T02:00:00Z"
        );
        seedModelAndTokenUsage(
                "mcl_budget_failed",
                "tul_budget_failed",
                "agt_budget_failed",
                "trc_budget_failed",
                "gpt-broken",
                AgentRuntimeConstants.MODEL_CALL_FAILED,
                800,
                700,
                0.01,
                40L,
                "2026-06-06T03:00:00Z"
        );
        seedModelAndTokenUsage(
                "mcl_budget_old",
                "tul_budget_old",
                "agt_budget_old",
                "trc_budget_old",
                "deterministic-demo-model",
                AgentRuntimeConstants.MODEL_CALL_SUCCESS,
                9000,
                1000,
                0.30,
                30L,
                "2026-06-05T03:00:00Z"
        );
    }

    private void seedBudgetTask(
            String id,
            String ownerUserId,
            String taskType,
            String status,
            String traceId,
            String createdAt,
            Long latencyMs
    ) {
        AgentTask task = new AgentTask();
        task.setId(id);
        task.setOwnerUserId(ownerUserId);
        task.setTaskType(taskType);
        task.setStatus(status);
        task.setInputJson("{}");
        task.setOutputJson(AgentRuntimeConstants.STATUS_FAILED.equals(status)
                ? "{\"errorMessage\":\"MODEL_PROVIDER_ERROR\"}"
                : "{\"summary\":\"done\"}");
        task.setTraceId(traceId);
        task.setLatencyMs(latencyMs);
        DirectFieldAccessor fields = new DirectFieldAccessor(task);
        fields.setPropertyValue("createdAt", Instant.parse(createdAt));
        fields.setPropertyValue("updatedAt", Instant.parse(createdAt));
        agentTaskRepository.save(task);
    }

    private void seedBudgetTrace(String id, String agentTaskId, String agentName, String traceId) {
        AgentTrace trace = new AgentTrace();
        trace.setId(id);
        trace.setAgentTaskId(agentTaskId);
        trace.setStepId("step_budget");
        trace.setAgentName(agentName);
        trace.setStatus(AgentRuntimeConstants.STATUS_DONE);
        trace.setSummary("Budget governance seed trace.");
        trace.setLatencyMs(1L);
        trace.setModel(AgentRuntimeConstants.MODEL_DETERMINISTIC_DEMO);
        trace.setPromptVersion(AgentRuntimeConstants.PROMPT_RESOURCE_V1);
        trace.setTraceId(traceId);
        trace.setSequenceNo(1);
        agentTraceRepository.save(trace);
    }

    private void seedBudgetResourceTask(
            String id,
            String agentTaskId,
            String learnerId,
            String goalId,
            String pathNodeId
    ) {
        ResourceGenerationTask task = new ResourceGenerationTask();
        task.setId(id);
        task.setLearnerId(learnerId);
        task.setGoalId(goalId);
        task.setPathNodeId(pathNodeId);
        task.setAgentTaskId(agentTaskId);
        task.setStatus(AgentRuntimeConstants.STATUS_DONE);
        task.setReviewStatus(AgentRuntimeConstants.REVIEW_PUBLISHED);
        task.setProgressPercent(100);
        task.setSafetyStatus(AgentRuntimeConstants.SAFETY_NEEDS_REVIEW);
        task.setTraceId("trc_budget_high");
        task.setCreatedBy(learnerId);
        resourceGenerationTaskRepository.save(task);
    }

    private void seedModelAndTokenUsage(
            String modelCallId,
            String tokenUsageId,
            String agentTaskId,
            String traceId,
            String model,
            String status,
            int promptTokens,
            int completionTokens,
            double estimatedCost,
            Long latencyMs,
            String createdAt
    ) {
        ModelCallLog modelCall = new ModelCallLog();
        modelCall.setId(modelCallId);
        modelCall.setTraceId(traceId);
        modelCall.setAgentTaskId(agentTaskId);
        modelCall.setModel(model);
        modelCall.setStatus(status);
        modelCall.setLatencyMs(latencyMs);
        modelCall.setErrorMessage(AgentRuntimeConstants.MODEL_CALL_FAILED.equals(status)
                ? "MODEL_PROVIDER_ERROR"
                : null);
        modelCall.setEstimatedCost(estimatedCost);
        DirectFieldAccessor modelFields = new DirectFieldAccessor(modelCall);
        modelFields.setPropertyValue("createdAt", Instant.parse(createdAt));
        modelCallLogRepository.save(modelCall);

        TokenUsageLog tokenUsage = new TokenUsageLog();
        tokenUsage.setId(tokenUsageId);
        tokenUsage.setTraceId(traceId);
        tokenUsage.setAgentTaskId(agentTaskId);
        tokenUsage.setPromptTokens(promptTokens);
        tokenUsage.setCompletionTokens(completionTokens);
        tokenUsage.setTotalTokens(promptTokens + completionTokens);
        tokenUsage.setEstimatedCost(estimatedCost);
        DirectFieldAccessor tokenFields = new DirectFieldAccessor(tokenUsage);
        tokenFields.setPropertyValue("createdAt", Instant.parse(createdAt));
        tokenUsageLogRepository.save(tokenUsage);
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

    private void seedStudentAnalyticsSignals() {
        LearningPath path = new LearningPath();
        path.setId("path_alice_backend");
        path.setLearnerId("alice");
        path.setGoalId("goal_backend");
        path.setStatus("ACTIVE");
        path.setReasonSummary("Backend learning path.");
        path.setTraceId("trc_path");
        learningPathRepository.save(path);

        seedPathNode(path.getId(), "node_foundation", "kp_foundation", "Backend foundation", "DONE", 0.86, 1,
                "Foundation completed.");
        seedPathNode(path.getId(), "node_sql_join", "kp_sql_join", "SQL JOIN remediation", "ACTIVE", 0.58, 2,
                "Practice SQL JOIN remediation.");
        seedPathNode(path.getId(), "node_transaction", "kp_transaction", "Transaction boundary", "LOCKED", 0.2, 3,
                "Unlock after SQL JOIN remediation.");

        seedMastery("mst_foundation", "kp_foundation", 0.86, "ASSESSMENT", "answer_foundation",
                "Foundation mastery confirmed.", Instant.parse("2026-06-05T01:00:00Z"));
        seedMastery("mst_sql_old", "kp_sql_join", 0.42, "ASSESSMENT", "answer_sql_old",
                "Initial SQL JOIN weakness.", Instant.parse("2026-06-05T02:00:00Z"));
        seedMastery("mst_sql_new", "kp_sql_join", 0.58, "ASSESSMENT", "answer_sql_new",
                "SQL JOIN mastery improved after feedback.", Instant.parse("2026-06-05T03:00:00Z"));

        AnswerRecord answer = new AnswerRecord();
        answer.setLearnerId("alice");
        answer.setQuestionId("q_sql_join");
        answer.setAnswer("JOIN duplicates happen from one-to-many relationships.");
        answer.setSafetyStatus("APPROVED");
        answer.setTraceId("trc_answer_summary");
        answerRecordRepository.save(answer);

        WrongQuestion wrongQuestion = new WrongQuestion();
        wrongQuestion.setLearnerId("alice");
        wrongQuestion.setQuestionId("q_sql_join");
        wrongQuestion.setAnswerId(answer.getId());
        wrongQuestion.setGradingResultId("gr_summary");
        wrongQuestion.setKnowledgePointId("kp_sql_join");
        wrongQuestion.setScore(0.4);
        wrongQuestion.setCauseAnalysis("Missed one-to-many cardinality.");
        wrongQuestion.setResourcePushStrategy("Push JOIN remediation resource.");
        wrongQuestion.setTraceId("trc_answer_summary");
        wrongQuestionRepository.save(wrongQuestion);
    }

    private void seedCourseScopedStudentSummarySignals() {
        seedCourse("course_backend", "Backend course", "teacher");
        seedCourse("course_foreign", "Foreign course", "other_teacher");
        seedEnrollment("course_backend", "alice", "ACTIVE");
        seedEnrollment("course_backend", "charlie", "DROPPED");
        seedKnowledgePoint("kp_backend_join", "course_backend", "Backend JOIN", 0.4);
        seedKnowledgePoint("kp_foreign_math", "course_foreign", "Foreign algebra", 0.5);

        seedLearningPath("path_alice_backend_summary", "alice", "course_backend");
        seedClassPathNode("node_alice_backend_summary", "path_alice_backend_summary", "alice",
                "kp_backend_join", "Backend JOIN", "ACTIVE", 0.55, 1,
                "Backend remediation.");
        seedLearningPath("path_alice_foreign_summary", "alice", "course_foreign");
        seedClassPathNode("node_alice_foreign_summary", "path_alice_foreign_summary", "alice",
                "kp_foreign_math", "Foreign algebra", "ACTIVE", 0.15, 1,
                "Foreign algebra weakness must not leak.");

        seedMasteryForLearner("mst_backend_old", "alice", "kp_backend_join", 0.35,
                "ASSESSMENT", "answer_backend_old", "Initial backend weakness.",
                Instant.parse("2026-06-05T01:00:00Z"));
        seedMasteryForLearner("mst_backend_new", "alice", "kp_backend_join", 0.55,
                "ASSESSMENT", "answer_backend_new", "Backend mastery improved.",
                Instant.parse("2026-06-05T02:00:00Z"));
        seedMasteryForLearner("mst_foreign", "alice", "kp_foreign_math", 0.15,
                "ASSESSMENT", "answer_foreign", "Foreign algebra weakness.",
                Instant.parse("2026-06-05T03:00:00Z"));

        seedWrongQuestion("wrong_backend_summary", "alice", "q_backend_join", "kp_backend_join", 0.4,
                "Backend JOIN cause.");
        seedWrongQuestion("wrong_foreign_summary", "alice", "q_foreign_math", "kp_foreign_math", 0.1,
                "Foreign algebra weakness.");
    }

    private void seedTeacherClassAnalyticsSignals() {
        seedCourse("course_backend", "Backend course", "teacher");
        seedEnrollment("course_backend", "alice", "ACTIVE");
        seedEnrollment("course_backend", "bob", "ACTIVE");
        seedKnowledgePoint("kp_sql_join", "course_backend", "SQL JOIN", 0.4);
        seedKnowledgePoint("kp_foundation", "course_backend", "Backend foundation", 0.2);
        seedKnowledgePoint("kp_transaction", "course_backend", "Transaction boundary", 0.7);

        seedLearningPath("path_alice_course_backend", "alice", "course_backend");
        seedLearningPath("path_bob_course_backend", "bob", "course_backend");
        seedClassPathNode("node_alice_join", "path_alice_course_backend", "alice", "kp_sql_join",
                "SQL JOIN", "ACTIVE", 0.45, 1, "Practice join cardinality.");
        seedClassPathNode("node_bob_join", "path_bob_course_backend", "bob", "kp_sql_join",
                "SQL JOIN", "ACTIVE", 0.55, 1, "Practice join cardinality.");
        seedClassPathNode("node_alice_foundation", "path_alice_course_backend", "alice", "kp_foundation",
                "Backend foundation", "DONE", 0.9, 2, "Foundation completed.");

        seedWrongQuestion("wrong_alice_join", "alice", "q_join_alice", "kp_sql_join", 0.4,
                "Missed join cardinality.");
        seedWrongQuestion("wrong_bob_join", "bob", "q_join_bob", "kp_sql_join", 0.5,
                "Missed join cardinality.");

        seedResourceTask("task_done", "alice", "course_backend", "node_alice_foundation",
                AgentRuntimeConstants.STATUS_DONE, AgentRuntimeConstants.REVIEW_PUBLISHED, 100);
        seedResourceTask("task_waiting", "alice", "course_backend", "node_alice_join",
                AgentRuntimeConstants.STATUS_WAITING_REVIEW, AgentRuntimeConstants.REVIEW_PENDING_CRITIC, 60);
        seedResourceTask("task_failed", "bob", "course_backend", "node_bob_join",
                AgentRuntimeConstants.STATUS_FAILED, AgentRuntimeConstants.REVIEW_REJECTED, 20);

        seedLearningResource("res_waiting", "task_waiting", "alice", "EXERCISE",
                "SQL JOIN remediation", AgentRuntimeConstants.REVIEW_PENDING_CRITIC);
        seedResourceReview("review_waiting", "task_waiting", "res_waiting", "CriticAgent",
                AgentRuntimeConstants.REVIEW_PENDING_CRITIC);
    }

    private void seedCourse(String id, String title, String teacherId) {
        Course course = new Course();
        course.setId(id);
        course.setTitle(title);
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

    private void seedKnowledgePoint(String id, String courseId, String title, double difficulty) {
        KnowledgePoint point = new KnowledgePoint();
        DirectFieldAccessor fields = new DirectFieldAccessor(point);
        fields.setPropertyValue("id", id);
        point.setCourseId(courseId);
        point.setTitle(title);
        point.setDifficulty(difficulty);
        knowledgePointRepository.save(point);
    }

    private void seedLearningPath(String id, String learnerId, String goalId) {
        LearningPath path = new LearningPath();
        path.setId(id);
        path.setLearnerId(learnerId);
        path.setGoalId(goalId);
        path.setStatus("ACTIVE");
        path.setReasonSummary("Course path.");
        path.setTraceId("trc_" + id);
        learningPathRepository.save(path);
    }

    private void seedClassPathNode(
            String id,
            String pathId,
            String learnerId,
            String knowledgePointId,
            String title,
            String status,
            double mastery,
            int sequenceNo,
            String reasonSummary
    ) {
        LearningPathNode node = new LearningPathNode();
        node.setId(id);
        node.setPathId(pathId);
        node.setLearnerId(learnerId);
        node.setKnowledgePointId(knowledgePointId);
        node.setTitle(title);
        node.setStatus(status);
        node.setMastery(mastery);
        node.setReasonSummary(reasonSummary);
        node.setSequenceNo(sequenceNo);
        learningPathNodeRepository.save(node);
    }

    private void seedWrongQuestion(
            String id,
            String learnerId,
            String questionId,
            String knowledgePointId,
            double score,
            String causeAnalysis
    ) {
        AnswerRecord answer = new AnswerRecord();
        answer.setId("answer_" + id);
        answer.setLearnerId(learnerId);
        answer.setQuestionId(questionId);
        answer.setAnswer("Seed answer for analytics.");
        answer.setSafetyStatus("APPROVED");
        answer.setTraceId("trc_" + id);
        answerRecordRepository.save(answer);

        WrongQuestion wrongQuestion = new WrongQuestion();
        wrongQuestion.setId(id);
        wrongQuestion.setLearnerId(learnerId);
        wrongQuestion.setQuestionId(questionId);
        wrongQuestion.setAnswerId(answer.getId());
        wrongQuestion.setGradingResultId("grading_" + id);
        wrongQuestion.setKnowledgePointId(knowledgePointId);
        wrongQuestion.setScore(score);
        wrongQuestion.setCauseAnalysis(causeAnalysis);
        wrongQuestion.setResourcePushStrategy("Push remediation.");
        wrongQuestion.setTraceId("trc_" + id);
        wrongQuestionRepository.save(wrongQuestion);
    }

    private void seedResourceTask(
            String id,
            String learnerId,
            String goalId,
            String pathNodeId,
            String status,
            String reviewStatus,
            int progressPercent
    ) {
        ResourceGenerationTask task = new ResourceGenerationTask();
        task.setId(id);
        task.setLearnerId(learnerId);
        task.setGoalId(goalId);
        task.setPathNodeId(pathNodeId);
        task.setAgentTaskId("agent_" + id);
        task.setStatus(status);
        task.setReviewStatus(reviewStatus);
        task.setProgressPercent(progressPercent);
        task.setSafetyStatus(AgentRuntimeConstants.SAFETY_NEEDS_REVIEW);
        task.setTraceId("trace_" + id);
        task.setCreatedBy(learnerId);
        resourceGenerationTaskRepository.save(task);
    }

    private void seedLearningResource(
            String id,
            String generationTaskId,
            String learnerId,
            String resourceType,
            String title,
            String reviewStatus
    ) {
        LearningResource resource = new LearningResource();
        resource.setId(id);
        resource.setGenerationTaskId(generationTaskId);
        resource.setLearnerId(learnerId);
        resource.setResourceType(resourceType);
        resource.setModality("MARKDOWN");
        resource.setTitle(title);
        resource.setReviewStatus(reviewStatus);
        resource.setCitationSummary("Course citation.");
        resource.setMarkdownContent("Sensitive draft content must not be exposed by analytics.");
        resource.setSafetyStatus(AgentRuntimeConstants.SAFETY_NEEDS_REVIEW);
        learningResourceRepository.save(resource);
    }

    private void seedResourceReview(
            String id,
            String generationTaskId,
            String resourceId,
            String reviewerType,
            String status
    ) {
        ResourceReview review = new ResourceReview();
        review.setId(id);
        review.setGenerationTaskId(generationTaskId);
        review.setResourceId(resourceId);
        review.setReviewerType(reviewerType);
        review.setStatus(status);
        review.setSummary("Awaiting review.");
        review.setTraceId("trace_" + generationTaskId);
        resourceReviewRepository.save(review);
    }

    private void seedPathNode(
            String pathId,
            String id,
            String knowledgePointId,
            String title,
            String status,
            double mastery,
            int sequenceNo,
            String reasonSummary
    ) {
        LearningPathNode node = new LearningPathNode();
        node.setId(id);
        node.setPathId(pathId);
        node.setLearnerId("alice");
        node.setKnowledgePointId(knowledgePointId);
        node.setTitle(title);
        node.setStatus(status);
        node.setMastery(mastery);
        node.setReasonSummary(reasonSummary);
        node.setSequenceNo(sequenceNo);
        learningPathNodeRepository.save(node);
    }

    private void seedMastery(
            String id,
            String knowledgePointId,
            double mastery,
            String sourceType,
            String sourceId,
            String reasonSummary,
            Instant recordedAt
    ) {
        MasteryRecord record = new MasteryRecord();
        record.setId(id);
        record.setLearnerId("alice");
        record.setKnowledgePointId(knowledgePointId);
        record.setMastery(mastery);
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        record.setReasonSummary(reasonSummary);
        record.setTraceId("trc_mastery_summary");
        DirectFieldAccessor fields = new DirectFieldAccessor(record);
        fields.setPropertyValue("createdAt", recordedAt);
        fields.setPropertyValue("updatedAt", recordedAt);
        masteryRecordRepository.save(record);
    }

    private void seedMasteryForLearner(
            String id,
            String learnerId,
            String knowledgePointId,
            double mastery,
            String sourceType,
            String sourceId,
            String reasonSummary,
            Instant recordedAt
    ) {
        MasteryRecord record = new MasteryRecord();
        record.setId(id);
        record.setLearnerId(learnerId);
        record.setKnowledgePointId(knowledgePointId);
        record.setMastery(mastery);
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        record.setReasonSummary(reasonSummary);
        record.setTraceId("trc_" + id);
        DirectFieldAccessor fields = new DirectFieldAccessor(record);
        fields.setPropertyValue("createdAt", recordedAt);
        fields.setPropertyValue("updatedAt", recordedAt);
        masteryRecordRepository.save(record);
    }
}
