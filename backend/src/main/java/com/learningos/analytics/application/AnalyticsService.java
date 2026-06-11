package com.learningos.analytics.application;

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
import com.learningos.agent.repository.TokenUsageLogRepository;
import com.learningos.assessment.domain.WrongQuestion;
import com.learningos.assessment.repository.AnswerRecordRepository;
import com.learningos.assessment.repository.WrongQuestionRepository;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.knowledge.application.CourseAccessService;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.domain.KnowledgePoint;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final long DEFAULT_TOKEN_BUDGET = 10_000L;
    private static final double DEFAULT_DEGRADE_THRESHOLD = 0.8;
    private static final double DEFAULT_MANUAL_CONFIRMATION_THRESHOLD = 1.0;
    private static final long DEFAULT_HIGH_COST_TOKEN_THRESHOLD = 8_000L;
    private static final double DEFAULT_HIGH_COST_USD_THRESHOLD = 1.0;
    private static final long DEFAULT_ANOMALY_LATENCY_MS_THRESHOLD = 2_000L;
    private static final long DEFAULT_ALERT_WINDOW_HOURS = 24L;
    private static final long DEFAULT_SLOW_QUERY_MS = 1_000L;
    private static final long DEFAULT_SLOW_MODEL_MS = 2_000L;
    private static final double DEFAULT_NO_SOURCE_RATE_THRESHOLD = 0.2;
    private static final long DEFAULT_NO_SOURCE_MIN_COUNT = 3L;
    private static final long DEFAULT_REVIEW_BACKLOG_HOURS = 24L;
    private static final long DEFAULT_REVIEW_BACKLOG_COUNT = 10L;
    private static final int MAX_RECENT_WRONG_CAUSES = 5;
    private static final int MAX_NEXT_STEPS = 3;
    private static final double WEAK_MASTERY_THRESHOLD = 0.6;

    private final AgentTaskRepository agentTaskRepository;
    private final AgentTraceRepository agentTraceRepository;
    private final ModelCallLogRepository modelCallLogRepository;
    private final TokenUsageLogRepository tokenUsageLogRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final WrongQuestionRepository wrongQuestionRepository;
    private final LearningEventRepository learningEventRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathNodeRepository learningPathNodeRepository;
    private final MasteryRecordRepository masteryRecordRepository;
    private final ResourceReviewRepository resourceReviewRepository;
    private final KbQueryLogRepository kbQueryLogRepository;
    private final CourseRepository courseRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final ResourceGenerationTaskRepository resourceGenerationTaskRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final CourseAccessService courseAccessService;
    private final OpsAlertPersistenceService opsAlertPersistenceService;

    @Autowired
    public AnalyticsService(
            AgentTaskRepository agentTaskRepository,
            AgentTraceRepository agentTraceRepository,
            ModelCallLogRepository modelCallLogRepository,
            TokenUsageLogRepository tokenUsageLogRepository,
            AnswerRecordRepository answerRecordRepository,
            WrongQuestionRepository wrongQuestionRepository,
            LearningEventRepository learningEventRepository,
            LearningPathRepository learningPathRepository,
            LearningPathNodeRepository learningPathNodeRepository,
            MasteryRecordRepository masteryRecordRepository,
            ResourceReviewRepository resourceReviewRepository,
            KbQueryLogRepository kbQueryLogRepository,
            CourseRepository courseRepository,
            KnowledgePointRepository knowledgePointRepository,
            ResourceGenerationTaskRepository resourceGenerationTaskRepository,
            LearningResourceRepository learningResourceRepository,
            CourseAccessService courseAccessService,
            org.springframework.beans.factory.ObjectProvider<OpsAlertPersistenceService> opsAlertPersistenceServiceProvider
    ) {
        this.agentTaskRepository = agentTaskRepository;
        this.agentTraceRepository = agentTraceRepository;
        this.modelCallLogRepository = modelCallLogRepository;
        this.tokenUsageLogRepository = tokenUsageLogRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.wrongQuestionRepository = wrongQuestionRepository;
        this.learningEventRepository = learningEventRepository;
        this.learningPathRepository = learningPathRepository;
        this.learningPathNodeRepository = learningPathNodeRepository;
        this.masteryRecordRepository = masteryRecordRepository;
        this.resourceReviewRepository = resourceReviewRepository;
        this.kbQueryLogRepository = kbQueryLogRepository;
        this.courseRepository = courseRepository;
        this.knowledgePointRepository = knowledgePointRepository;
        this.resourceGenerationTaskRepository = resourceGenerationTaskRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.courseAccessService = courseAccessService;
        this.opsAlertPersistenceService = opsAlertPersistenceServiceProvider.getIfAvailable();
    }

    public AnalyticsService(
            AgentTaskRepository agentTaskRepository,
            AgentTraceRepository agentTraceRepository,
            ModelCallLogRepository modelCallLogRepository,
            TokenUsageLogRepository tokenUsageLogRepository,
            AnswerRecordRepository answerRecordRepository,
            WrongQuestionRepository wrongQuestionRepository,
            LearningEventRepository learningEventRepository,
            LearningPathRepository learningPathRepository,
            LearningPathNodeRepository learningPathNodeRepository,
            MasteryRecordRepository masteryRecordRepository,
            ResourceReviewRepository resourceReviewRepository,
            KbQueryLogRepository kbQueryLogRepository,
            CourseRepository courseRepository,
            KnowledgePointRepository knowledgePointRepository,
            ResourceGenerationTaskRepository resourceGenerationTaskRepository,
            LearningResourceRepository learningResourceRepository
    ) {
        this(
                agentTaskRepository,
                agentTraceRepository,
                modelCallLogRepository,
                tokenUsageLogRepository,
                answerRecordRepository,
                wrongQuestionRepository,
                learningEventRepository,
                learningPathRepository,
                learningPathNodeRepository,
                masteryRecordRepository,
                resourceReviewRepository,
                kbQueryLogRepository,
                courseRepository,
                knowledgePointRepository,
                resourceGenerationTaskRepository,
                learningResourceRepository,
                null,
                emptyProvider()
        );
    }

    private static <T> org.springframework.beans.factory.ObjectProvider<T> emptyProvider() {
        return new org.springframework.beans.factory.ObjectProvider<>() {
            @Override
            public T getObject() {
                return null;
            }

            @Override
            public T getIfAvailable() {
                return null;
            }

            @Override
            public T getIfAvailable(java.util.function.Supplier<T> defaultImplementationSupplier) {
                return null;
            }

            @Override
            public T getIfUnique() {
                return null;
            }

            @Override
            public T getObject(Object... args) {
                return null;
            }

            @Override
            public void ifAvailable(java.util.function.Consumer<T> dependencyConsumer) {
            }

            @Override
            public void ifUnique(java.util.function.Consumer<T> dependencyConsumer) {
            }
        };
    }

    @Transactional(readOnly = true)
    public AnalyticsOverview overview() {
        Map<String, Long> resourceReviewStatusCounts = new LinkedHashMap<>();
        for (ResourceReview review : resourceReviewRepository.findAll()) {
            resourceReviewStatusCounts.merge(review.getStatus(), 1L, Long::sum);
        }

        List<AgentTask> agentTasks = agentTaskRepository.findAll();
        List<AgentTrace> agentTraces = agentTraceRepository.findAll();
        List<ModelCallLog> modelCallLogs = modelCallLogRepository.findAll();
        List<TokenUsageLog> tokenUsageLogs = tokenUsageLogRepository.findAll();

        Map<String, AgentTask> taskById = agentTasks.stream()
                .collect(Collectors.toMap(
                        AgentTask::getId,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
        Map<String, String> modelByAgentTaskId = modelCallLogs.stream()
                .collect(Collectors.toMap(
                        ModelCallLog::getAgentTaskId,
                        ModelCallLog::getModel,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
        Map<String, String> agentNameByTaskId = firstAgentNameByTaskId(agentTraces);
        long totalTokens = tokenUsageLogRepository.sumTotalTokens();
        double estimatedCost = tokenUsageLogRepository.sumEstimatedCost();

        return new AnalyticsOverview(
                agentTasks.size(),
                modelCallLogs.size(),
                new TokenUsageTotals(
                        tokenUsageLogRepository.sumPromptTokens(),
                        tokenUsageLogRepository.sumCompletionTokens(),
                        totalTokens,
                        estimatedCost,
                        tokenUsageLogRepository.summarizeByAgentTask().stream()
                                .map(summary -> new AgentTaskTokenUsageTotals(
                                        summary.getAgentTaskId(),
                                        summary.getPromptTokens(),
                                        summary.getCompletionTokens(),
                                        summary.getTotalTokens(),
                                        summary.getEstimatedCost()
                                ))
                                .toList(),
                        summarizeByModel(tokenUsageLogs, modelByAgentTaskId),
                        summarizeByUser(tokenUsageLogs, taskById),
                        summarizeByAgentName(tokenUsageLogs, agentNameByTaskId),
                        budgetStatus(totalTokens)
                ),
                answerRecordRepository.count(),
                wrongQuestionRepository.count(),
                learningEventRepository.count(),
                resourceReviewStatusCounts,
                agentSummary(agentTasks, estimatedCost)
        );
    }

    @Transactional(readOnly = true)
    public StudentAnalyticsSummary studentSummary(String learnerId) {
        return studentSummary(learnerId, null);
    }

    @Transactional(readOnly = true)
    public StudentAnalyticsSummary studentSummary(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String learnerId,
            String courseId
    ) {
        String normalizedCourseId = normalize(courseId);
        if (normalizedCourseId == null) {
            if (currentUserTeacher && !currentUserAdmin) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "courseId is required for teacher student analytics");
            }
            if (!currentUserAdmin && !currentUserId.equals(learnerId)) {
                throw new ApiException(ErrorCode.FORBIDDEN, "Learner analytics access denied");
            }
            return studentSummary(learnerId, null);
        }

        Course course = requireStudentSummaryCourseAccess(
                currentUserId,
                currentUserAdmin,
                currentUserTeacher,
                learnerId,
                normalizedCourseId
        );
        return studentSummary(learnerId, course.getId());
    }

    private StudentAnalyticsSummary studentSummary(String learnerId, String courseId) {
        Set<String> courseKnowledgePointIds = courseId == null ? null : courseKnowledgePointIds(courseId);
        Set<String> scopedPathIds = scopedPathIds(learnerId, courseId);
        List<LearningPathNode> nodes = learningPathNodeRepository.findAll().stream()
                .filter(node -> learnerId.equals(node.getLearnerId()))
                .filter(node -> courseId == null || scopedPathIds.contains(node.getPathId()))
                .filter(node -> courseId == null || courseKnowledgePointIds.contains(node.getKnowledgePointId()))
                .sorted(Comparator.comparing(LearningPathNode::getSequenceNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LearningPathNode::getId, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<MasteryRecord> masteryRecords = masteryRecordRepository.findAll().stream()
                .filter(record -> learnerId.equals(record.getLearnerId()))
                .filter(record -> courseId == null || courseKnowledgePointIds.contains(record.getKnowledgePointId()))
                .toList();
        List<WrongQuestion> recentWrongCauses = recentWrongCauses(learnerId, courseKnowledgePointIds);

        return new StudentAnalyticsSummary(
                learnerId,
                progress(nodes),
                currentMastery(nodes, masteryRecords),
                masteryTrend(masteryRecords, recentWrongCauses),
                recentWrongCauses.stream()
                        .map(this::toRecentWrongCause)
                        .toList(),
                recommendedNextSteps(nodes, recentWrongCauses)
        );
    }

    private Course requireStudentSummaryCourseAccess(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String learnerId,
            String courseId
    ) {
        if (!currentUserAdmin && !currentUserTeacher && !currentUserId.equals(learnerId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learner analytics access denied");
        }
        Course course = requireCourseReadForStudentSummary(currentUserId, currentUserAdmin, currentUserTeacher, courseId);
        if (currentUserTeacher && !currentUserAdmin
                && !courseAccessService.listActiveLearnerIds(course.getId()).contains(learnerId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learner analytics access denied");
        }
        if (!currentUserAdmin && !currentUserTeacher && !currentUserId.equals(learnerId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learner analytics access denied");
        }
        return course;
    }

    private Course requireCourseReadForStudentSummary(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String courseId
    ) {
        if (courseAccessService != null) {
            try {
                return courseAccessService.requireCourseRead(
                        currentUserId,
                        currentUserAdmin,
                        currentUserTeacher,
                        courseId
                );
            } catch (ApiException exception) {
                if (currentUserTeacher && !currentUserAdmin && exception.getErrorCode() == ErrorCode.NOT_FOUND) {
                    throw new ApiException(ErrorCode.FORBIDDEN, "Course access denied");
                }
                throw exception;
            }
        }
        return courseRepository.findById(courseId)
                .orElseThrow(() -> currentUserAdmin
                        ? new ApiException(ErrorCode.NOT_FOUND, "Course not found")
                        : new ApiException(ErrorCode.FORBIDDEN, "Course access denied"));
    }

    private Set<String> courseKnowledgePointIds(String courseId) {
        if (courseId == null) {
            return Set.of();
        }
        return knowledgePointRepository.findByCourseIdOrderByCreatedAtAsc(courseId).stream()
                .map(KnowledgePoint::getId)
                .collect(Collectors.toSet());
    }

    private Set<String> scopedPathIds(String learnerId, String courseId) {
        if (courseId == null) {
            return Set.of();
        }
        return learningPathRepository.findAll().stream()
                .filter(path -> learnerId.equals(path.getLearnerId()))
                .filter(path -> courseId.equals(path.getGoalId()))
                .map(LearningPath::getId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public TeacherClassAnalyticsSummary teacherClassSummary(
            String courseId,
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> scopedClassCourseMissing(currentUserAdmin));
        requireTeacherClassAccess(currentUserId, currentUserAdmin, currentUserTeacher, course);

        List<KnowledgePoint> courseKnowledgePoints = knowledgePointRepository.findByCourseIdOrderByCreatedAtAsc(courseId);
        Set<String> courseKnowledgePointIds = courseKnowledgePoints.stream()
                .map(KnowledgePoint::getId)
                .collect(Collectors.toSet());
        Map<String, String> knowledgeTitleById = courseKnowledgePoints.stream()
                .collect(Collectors.toMap(
                        KnowledgePoint::getId,
                        KnowledgePoint::getTitle,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        Set<String> classLearnerIds = classLearnerIds(courseId);
        List<LearningPath> classPaths = learningPathRepository.findAll().stream()
                .filter(path -> courseId.equals(path.getGoalId()))
                .filter(path -> classLearnerIds.contains(path.getLearnerId()))
                .toList();
        Set<String> classPathIds = classPaths.stream()
                .map(LearningPath::getId)
                .collect(Collectors.toSet());

        List<LearningPathNode> classNodes = learningPathNodeRepository.findAll().stream()
                .filter(node -> classPathIds.contains(node.getPathId()))
                .filter(node -> classLearnerIds.contains(node.getLearnerId()))
                .filter(node -> courseKnowledgePointIds.contains(node.getKnowledgePointId()))
                .toList();
        List<WrongQuestion> classWrongQuestions = wrongQuestionRepository.findAll().stream()
                .filter(wrongQuestion -> classLearnerIds.contains(wrongQuestion.getLearnerId()))
                .filter(wrongQuestion -> courseKnowledgePointIds.contains(wrongQuestion.getKnowledgePointId()))
                .toList();
        List<ResourceGenerationTask> classResourceTasks = resourceGenerationTaskRepository.findAll().stream()
                .filter(task -> courseId.equals(task.getGoalId()))
                .filter(task -> classLearnerIds.contains(task.getLearnerId()))
                .toList();

        return new TeacherClassAnalyticsSummary(
                courseId,
                course.getTeacherId(),
                classLearnerIds.size(),
                weakKnowledgePoints(classNodes, classWrongQuestions, knowledgeTitleById),
                wrongCauseDistribution(classWrongQuestions),
                resourceCompletion(classResourceTasks),
                pendingReviews(classResourceTasks)
        );
    }

    @Transactional(readOnly = true)
    public TokenBudgetGovernanceSummary tokenBudgetGovernance(
            boolean currentUserAdmin,
            Instant from,
            Instant to,
            Long tokenBudget,
            Double degradeThreshold,
            Double manualConfirmationThreshold,
            Long highCostTokenThreshold,
            Double highCostUsdThreshold,
            Long anomalyLatencyMsThreshold
    ) {
        if (!currentUserAdmin) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Token budget governance requires admin access");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "from must be before to");
        }

        BudgetRule budgetRule = new BudgetRule(
                positiveLongOrDefault(tokenBudget, DEFAULT_TOKEN_BUDGET),
                positiveDoubleOrDefault(degradeThreshold, DEFAULT_DEGRADE_THRESHOLD),
                positiveDoubleOrDefault(manualConfirmationThreshold, DEFAULT_MANUAL_CONFIRMATION_THRESHOLD),
                positiveLongOrDefault(highCostTokenThreshold, DEFAULT_HIGH_COST_TOKEN_THRESHOLD),
                positiveDoubleOrDefault(highCostUsdThreshold, DEFAULT_HIGH_COST_USD_THRESHOLD),
                positiveLongOrDefault(anomalyLatencyMsThreshold, DEFAULT_ANOMALY_LATENCY_MS_THRESHOLD)
        );
        if (budgetRule.degradeThreshold() > budgetRule.manualConfirmationThreshold()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "degradeThreshold must be less than or equal to manualConfirmationThreshold");
        }

        List<AgentTask> agentTasks = agentTaskRepository.findAll();
        Map<String, AgentTask> taskById = agentTasks.stream()
                .collect(Collectors.toMap(
                        AgentTask::getId,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
        Map<String, ResourceGenerationTask> resourceTaskByAgentTaskId = resourceTaskByAgentTaskId();
        List<TokenUsageLog> tokenUsageLogs = tokenUsageLogRepository.findAll().stream()
                .filter(log -> inWindow(instantField(log, "createdAt"), from, to))
                .toList();
        List<ModelCallLog> modelCallLogs = modelCallLogRepository.findAll().stream()
                .filter(log -> inWindow(instantField(log, "createdAt"), from, to))
                .toList();

        TokenAccumulator totals = new TokenAccumulator();
        tokenUsageLogs.forEach(totals::add);

        CostStats costStats = new CostStats(
                totals.promptTokens,
                totals.completionTokens,
                totals.totalTokens,
                totals.estimatedCost,
                modelCallLogs.size(),
                governanceByUser(tokenUsageLogs, taskById),
                governanceByCourse(tokenUsageLogs, resourceTaskByAgentTaskId),
                governanceByAgentType(tokenUsageLogs, taskById),
                List.of(new TimeWindowTokenUsageTotals(
                        from,
                        to,
                        totals.promptTokens,
                        totals.completionTokens,
                        totals.totalTokens,
                        totals.estimatedCost
                ))
        );

        return new TokenBudgetGovernanceSummary(
                costStats,
                budgetDecision(totals.totalTokens, budgetRule),
                highCostTaskWarnings(tokenUsageLogs, taskById, resourceTaskByAgentTaskId, budgetRule),
                abnormalModelCalls(modelCallLogs, taskById, resourceTaskByAgentTaskId, budgetRule)
        );
    }

    @Transactional(readOnly = true)
    public OpsAlertSummary opsAlerts(
            Instant from,
            Instant to,
            Long slowQueryMs,
            Long slowModelMs,
            Double noSourceRateThreshold,
            Long noSourceMinCount,
            Long reviewBacklogHours,
            Long reviewBacklogCount
    ) {
        Instant windowEnd = to == null ? Instant.now() : to;
        Instant windowStart = from == null ? windowEnd.minus(Duration.ofHours(DEFAULT_ALERT_WINDOW_HOURS)) : from;
        if (!windowStart.isBefore(windowEnd)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "from must be before to");
        }

        OpsAlertThresholds thresholds = new OpsAlertThresholds(
                requirePositive(slowQueryMs, DEFAULT_SLOW_QUERY_MS, "slowQueryMs"),
                requirePositive(slowModelMs, DEFAULT_SLOW_MODEL_MS, "slowModelMs"),
                requireRate(noSourceRateThreshold, DEFAULT_NO_SOURCE_RATE_THRESHOLD, "noSourceRateThreshold"),
                requirePositive(noSourceMinCount, DEFAULT_NO_SOURCE_MIN_COUNT, "noSourceMinCount"),
                requirePositive(reviewBacklogHours, DEFAULT_REVIEW_BACKLOG_HOURS, "reviewBacklogHours"),
                requirePositive(reviewBacklogCount, DEFAULT_REVIEW_BACKLOG_COUNT, "reviewBacklogCount")
        );

        List<KbQueryLog> queryLogs = kbQueryLogRepository.findAll().stream()
                .filter(log -> inWindow(instantField(log, "createdAt"), windowStart, windowEnd))
                .toList();
        List<ModelCallLog> modelCallLogs = modelCallLogRepository.findAll().stream()
                .filter(log -> inWindow(instantField(log, "createdAt"), windowStart, windowEnd))
                .toList();
        List<ResourceReview> reviews = resourceReviewRepository.findAll().stream()
                .filter(review -> !instantField(review, "createdAt").isAfter(windowEnd))
                .toList();

        List<OpsAlertItem> alerts = new ArrayList<>();
        slowRagQueryAlert(queryLogs, thresholds).ifPresent(alerts::add);
        slowModelCallAlert(modelCallLogs, thresholds).ifPresent(alerts::add);
        ragNoSourceAlert(queryLogs, thresholds).ifPresent(alerts::add);
        reviewBacklogAlert(reviews, windowEnd, thresholds).ifPresent(alerts::add);

        List<OpsAlertItem> persistedAlerts = persistTriggeredAlerts(alerts, windowStart, windowEnd);
        return new OpsAlertSummary(windowStart, windowEnd, thresholds, persistedAlerts);
    }

    @Transactional(readOnly = true)
    public List<PersistedOpsAlertRecord> recentPersistedAlerts() {
        if (opsAlertPersistenceService == null) {
            return List.of();
        }
        return opsAlertPersistenceService.recentRecords();
    }

    @Transactional
    public PersistedOpsAlertRecord acknowledgeOpsAlert(String alertId, String acknowledgedBy) {
        if (opsAlertPersistenceService == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Ops alert persistence disabled");
        }
        return opsAlertPersistenceService.acknowledge(alertId, acknowledgedBy);
    }

    private List<OpsAlertItem> persistTriggeredAlerts(
            List<OpsAlertItem> alerts,
            Instant windowStart,
            Instant windowEnd
    ) {
        if (opsAlertPersistenceService == null) {
            return alerts;
        }
        return alerts.stream()
                .map(alert -> opsAlertPersistenceService.persistTriggeredAlert(alert, windowStart, windowEnd))
                .toList();
    }

    public record TokenUsageTotals(
            long promptTokens,
            long completionTokens,
            long totalTokens,
            double estimatedCost,
            List<AgentTaskTokenUsageTotals> byAgentTask,
            List<ModelTokenUsageTotals> byModel,
            List<UserTokenUsageTotals> byUser,
            List<AgentNameTokenUsageTotals> byAgentName,
            TokenBudgetStatus budget
    ) {
    }

    public record AgentTaskTokenUsageTotals(
            String agentTaskId,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            double estimatedCost
    ) {
    }

    public record ModelTokenUsageTotals(
            String model,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            double estimatedCost
    ) {
    }

    public record UserTokenUsageTotals(
            String userId,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            double estimatedCost
    ) {
    }

    public record AgentNameTokenUsageTotals(
            String agentName,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            double estimatedCost
    ) {
    }

    public record TokenBudgetStatus(
            long budgetTokens,
            long usedTokens,
            long remainingTokens,
            String status,
            String fallbackStrategy
    ) {
    }

    public record AgentSummary(
            long totalTasks,
            long successCount,
            long failureCount,
            double successRate,
            double failureRate,
            double averageLatencyMs,
            double tokenCost,
            double ragHitRate
    ) {
    }

    public record AnalyticsOverview(
            long agentTaskCount,
            long modelCallCount,
            TokenUsageTotals tokenUsage,
            long answerRecordCount,
            long wrongQuestionCount,
            long learningEventCount,
            Map<String, Long> resourceReviewStatusCounts,
            AgentSummary agentSummary
    ) {
    }

    public record StudentAnalyticsSummary(
            String learnerId,
            StudentProgress progress,
            List<CurrentMastery> currentMastery,
            List<MasteryTrendPoint> masteryTrend,
            List<RecentWrongCause> recentWrongCauses,
            List<RecommendedNextStep> recommendedNextSteps
    ) {
    }

    public record StudentProgress(
            long totalNodes,
            long doneNodes,
            long activeNodes,
            long lockedNodes,
            double completionRate
    ) {
    }

    public record CurrentMastery(
            String knowledgePointId,
            double mastery,
            String sourceType,
            String sourceId,
            String reasonSummary
    ) {
    }

    public record MasteryTrendPoint(
            String knowledgePointId,
            double mastery,
            String sourceType,
            String sourceId,
            String reasonSummary
    ) {
    }

    public record RecentWrongCause(
            String knowledgePointId,
            String questionId,
            double score,
            String causeAnalysis,
            String resourcePushStrategy,
            String traceId
    ) {
    }

    public record RecommendedNextStep(
            String type,
            String knowledgePointId,
            String title,
            String reason
    ) {
    }

    public record TeacherClassAnalyticsSummary(
            String courseId,
            String teacherId,
            int learnerCount,
            List<WeakKnowledgePoint> weakKnowledgePoints,
            List<WrongCauseDistribution> wrongCauseDistribution,
            ResourceCompletion resourceCompletion,
            List<PendingReviewSummary> pendingReviews
    ) {
    }

    public record WeakKnowledgePoint(
            String knowledgePointId,
            String title,
            double averageMastery,
            long wrongQuestionCount,
            long affectedLearnerCount,
            String topCause
    ) {
    }

    public record WrongCauseDistribution(
            String knowledgePointId,
            String causeAnalysis,
            long count
    ) {
    }

    public record ResourceCompletion(
            long totalTasks,
            long doneTasks,
            long waitingReviewTasks,
            long failedTasks,
            double averageProgressPercent,
            double completionRate
    ) {
    }

    public record PendingReviewSummary(
            String reviewId,
            String resourceId,
            String generationTaskId,
            String status,
            String reviewerType,
            String resourceTitle,
            String resourceType
    ) {
    }

    public record TokenBudgetGovernanceSummary(
            CostStats costStats,
            BudgetDecision budgetDecision,
            List<HighCostTaskWarning> highCostTasks,
            List<AbnormalModelCall> abnormalModelCalls
    ) {
    }

    public record CostStats(
            long promptTokens,
            long completionTokens,
            long totalTokens,
            double estimatedCost,
            long modelCallCount,
            List<UserTokenUsageTotals> byUser,
            List<CourseTokenUsageTotals> byCourse,
            List<AgentTypeTokenUsageTotals> byAgentType,
            List<TimeWindowTokenUsageTotals> byTimeWindow
    ) {
    }

    public record CourseTokenUsageTotals(
            String courseId,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            double estimatedCost
    ) {
    }

    public record AgentTypeTokenUsageTotals(
            String agentType,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            double estimatedCost
    ) {
    }

    public record TimeWindowTokenUsageTotals(
            Instant windowStart,
            Instant windowEnd,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            double estimatedCost
    ) {
    }

    public record BudgetDecision(
            long budgetTokens,
            long usedTokens,
            long remainingTokens,
            double usageRatio,
            String status,
            String action,
            boolean requiresManualConfirmation,
            String fallbackStrategy,
            List<String> reasonCodes
    ) {
    }

    public record HighCostTaskWarning(
            String agentTaskId,
            String traceId,
            String userId,
            String courseId,
            String agentType,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            double estimatedCost,
            String warningLevel,
            List<String> reasonCodes
    ) {
    }

    public record AbnormalModelCall(
            String modelCallId,
            String agentTaskId,
            String traceId,
            String userId,
            String courseId,
            String agentType,
            String model,
            String status,
            Long latencyMs,
            double estimatedCost,
            String errorMessage,
            Instant createdAt,
            List<String> reasonCodes
    ) {
    }

    public record OpsAlertSummary(
            Instant windowStart,
            Instant windowEnd,
            OpsAlertThresholds thresholds,
            List<OpsAlertItem> alerts
    ) {
    }

    public record OpsAlertThresholds(
            long slowQueryMs,
            long slowModelMs,
            double noSourceRateThreshold,
            long noSourceMinCount,
            long reviewBacklogHours,
            long reviewBacklogCount
    ) {
    }

    public record OpsAlertItem(
            String type,
            String severity,
            boolean triggered,
            long count,
            String threshold,
            String summary,
            Map<String, Object> metrics,
            List<String> reasonCodes,
            String alertId,
            String alertStatus
    ) {
    }

    public record PersistedOpsAlertRecord(
            String alertId,
            String alertType,
            String severity,
            String summary,
            Map<String, Object> metrics,
            List<String> reasonCodes,
            Instant windowStart,
            Instant windowEnd,
            String status,
            String acknowledgedBy,
            Instant acknowledgedAt,
            String notificationStatus,
            Instant updatedAt
    ) {
    }

    private Optional<OpsAlertItem> slowRagQueryAlert(List<KbQueryLog> queryLogs, OpsAlertThresholds thresholds) {
        List<Long> slowLatencies = queryLogs.stream()
                .map(log -> numberField(log, "latencyMs"))
                .filter(latency -> latency >= thresholds.slowQueryMs())
                .toList();
        if (slowLatencies.isEmpty()) {
            return Optional.empty();
        }

        long maxLatencyMs = slowLatencies.stream().mapToLong(Long::longValue).max().orElse(0L);
        double averageLatencyMs = slowLatencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalCount", queryLogs.size());
        metrics.put("slowCount", slowLatencies.size());
        metrics.put("maxLatencyMs", maxLatencyMs);
        metrics.put("avgLatencyMs", averageLatencyMs);
        return Optional.of(new OpsAlertItem(
                "SLOW_RAG_QUERY",
                severity(maxLatencyMs, thresholds.slowQueryMs()),
                true,
                slowLatencies.size(),
                "slowQueryMs=" + thresholds.slowQueryMs(),
                "RAG query latency exceeded threshold",
                metrics,
                List.of("QUERY_HIGH_LATENCY"),
                null,
                null
        ));
    }

    private Optional<OpsAlertItem> slowModelCallAlert(List<ModelCallLog> modelCallLogs, OpsAlertThresholds thresholds) {
        List<Long> slowLatencies = modelCallLogs.stream()
                .map(ModelCallLog::getLatencyMs)
                .filter(latency -> latency != null && latency >= thresholds.slowModelMs())
                .toList();
        if (slowLatencies.isEmpty()) {
            return Optional.empty();
        }

        long maxLatencyMs = slowLatencies.stream().mapToLong(Long::longValue).max().orElse(0L);
        double averageLatencyMs = slowLatencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalCount", modelCallLogs.size());
        metrics.put("slowCount", slowLatencies.size());
        metrics.put("maxLatencyMs", maxLatencyMs);
        metrics.put("avgLatencyMs", averageLatencyMs);
        return Optional.of(new OpsAlertItem(
                "SLOW_MODEL_CALL",
                severity(maxLatencyMs, thresholds.slowModelMs()),
                true,
                slowLatencies.size(),
                "slowModelMs=" + thresholds.slowModelMs(),
                "Model call latency exceeded threshold",
                metrics,
                List.of("MODEL_CALL_HIGH_LATENCY"),
                null,
                null
        ));
    }

    private Optional<OpsAlertItem> ragNoSourceAlert(List<KbQueryLog> queryLogs, OpsAlertThresholds thresholds) {
        long totalRagCount = queryLogs.size();
        if (totalRagCount == 0) {
            return Optional.empty();
        }

        long noSourceCount = queryLogs.stream()
                .filter(log -> numberField(log, "retrievalCount") <= 0)
                .count();
        double noSourceRate = (double) noSourceCount / totalRagCount;
        if (noSourceCount < thresholds.noSourceMinCount()
                || noSourceRate < thresholds.noSourceRateThreshold()) {
            return Optional.empty();
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalRagCount", totalRagCount);
        metrics.put("noSourceCount", noSourceCount);
        metrics.put("noSourceRate", noSourceRate);
        return Optional.of(new OpsAlertItem(
                "RAG_NO_SOURCE",
                noSourceRate >= 0.5 ? "CRITICAL" : "WARNING",
                true,
                noSourceCount,
                "noSourceRateThreshold=" + thresholds.noSourceRateThreshold()
                        + ",noSourceMinCount=" + thresholds.noSourceMinCount(),
                "RAG no-source rate exceeded threshold",
                metrics,
                List.of("RAG_NO_SOURCE"),
                null,
                null
        ));
    }

    private Optional<OpsAlertItem> reviewBacklogAlert(
            List<ResourceReview> reviews,
            Instant windowEnd,
            OpsAlertThresholds thresholds
    ) {
        List<ResourceReview> backlogReviews = reviews.stream()
                .filter(review -> "PENDING_CRITIC".equals(review.getStatus())
                        || "REVISION_REQUESTED".equals(review.getStatus()))
                .filter(review -> reviewAgeHours(review, windowEnd) >= thresholds.reviewBacklogHours())
                .toList();
        if (backlogReviews.size() < thresholds.reviewBacklogCount()) {
            return Optional.empty();
        }

        long oldestAgeHours = backlogReviews.stream()
                .mapToLong(review -> reviewAgeHours(review, windowEnd))
                .max()
                .orElse(0L);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("backlogCount", backlogReviews.size());
        metrics.put("oldestAgeHours", oldestAgeHours);
        metrics.put("reviewBacklogHours", thresholds.reviewBacklogHours());
        metrics.put("reviewBacklogCount", thresholds.reviewBacklogCount());
        return Optional.of(new OpsAlertItem(
                "REVIEW_BACKLOG",
                backlogReviews.size() >= thresholds.reviewBacklogCount() * 2
                        || oldestAgeHours >= thresholds.reviewBacklogHours() * 2
                        ? "CRITICAL"
                        : "WARNING",
                true,
                backlogReviews.size(),
                "reviewBacklogHours=" + thresholds.reviewBacklogHours()
                        + ",reviewBacklogCount=" + thresholds.reviewBacklogCount(),
                "Resource review backlog exceeded threshold",
                metrics,
                List.of("REVIEW_BACKLOG_OVER_THRESHOLD"),
                null,
                null
        ));
    }

    private long reviewAgeHours(ResourceReview review, Instant now) {
        Instant createdAt = instantField(review, "createdAt");
        if (createdAt.isAfter(now)) {
            return 0L;
        }
        return Duration.between(createdAt, now).toHours();
    }

    private String severity(long observed, long threshold) {
        return observed >= threshold * 2 ? "CRITICAL" : "WARNING";
    }

    private long requirePositive(Long value, long defaultValue, String name) {
        if (value == null) {
            return defaultValue;
        }
        if (value <= 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, name + " must be positive");
        }
        return value;
    }

    private double requireRate(Double value, double defaultValue, String name) {
        if (value == null) {
            return defaultValue;
        }
        if (value < 0.0 || value > 1.0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, name + " must be between 0 and 1");
        }
        return value;
    }

    private List<ModelTokenUsageTotals> summarizeByModel(
            List<TokenUsageLog> tokenUsageLogs,
            Map<String, String> modelByAgentTaskId
    ) {
        Map<String, TokenAccumulator> byModel = new LinkedHashMap<>();
        for (TokenUsageLog log : tokenUsageLogs) {
            String model = modelByAgentTaskId.getOrDefault(log.getAgentTaskId(), "UNKNOWN_MODEL");
            byModel.computeIfAbsent(model, ignored -> new TokenAccumulator()).add(log);
        }
        return byModel.entrySet().stream()
                .map(entry -> new ModelTokenUsageTotals(
                        entry.getKey(),
                        entry.getValue().promptTokens,
                        entry.getValue().completionTokens,
                        entry.getValue().totalTokens,
                        entry.getValue().estimatedCost
                ))
                .toList();
    }

    private List<UserTokenUsageTotals> summarizeByUser(
            List<TokenUsageLog> tokenUsageLogs,
            Map<String, AgentTask> taskById
    ) {
        Map<String, TokenAccumulator> byUser = new LinkedHashMap<>();
        for (TokenUsageLog log : tokenUsageLogs) {
            AgentTask task = taskById.get(log.getAgentTaskId());
            String userId = task == null ? "UNKNOWN_USER" : task.getOwnerUserId();
            byUser.computeIfAbsent(userId, ignored -> new TokenAccumulator()).add(log);
        }
        return byUser.entrySet().stream()
                .map(entry -> new UserTokenUsageTotals(
                        entry.getKey(),
                        entry.getValue().promptTokens,
                        entry.getValue().completionTokens,
                        entry.getValue().totalTokens,
                        entry.getValue().estimatedCost
                ))
                .toList();
    }

    private List<AgentNameTokenUsageTotals> summarizeByAgentName(
            List<TokenUsageLog> tokenUsageLogs,
            Map<String, String> agentNameByTaskId
    ) {
        Map<String, TokenAccumulator> byAgentName = new LinkedHashMap<>();
        for (TokenUsageLog log : tokenUsageLogs) {
            String agentName = agentNameByTaskId.getOrDefault(log.getAgentTaskId(), "UNKNOWN_AGENT");
            byAgentName.computeIfAbsent(agentName, ignored -> new TokenAccumulator()).add(log);
        }
        return byAgentName.entrySet().stream()
                .map(entry -> new AgentNameTokenUsageTotals(
                        entry.getKey(),
                        entry.getValue().promptTokens,
                        entry.getValue().completionTokens,
                        entry.getValue().totalTokens,
                        entry.getValue().estimatedCost
                ))
                .toList();
    }

    private Map<String, String> firstAgentNameByTaskId(List<AgentTrace> traces) {
        Map<String, AgentTrace> firstTraceByTask = new LinkedHashMap<>();
        for (AgentTrace trace : traces) {
            AgentTrace current = firstTraceByTask.get(trace.getAgentTaskId());
            if (current == null || safeSequence(trace) < safeSequence(current)) {
                firstTraceByTask.put(trace.getAgentTaskId(), trace);
            }
        }
        Map<String, String> agentNameByTaskId = new LinkedHashMap<>();
        for (Map.Entry<String, AgentTrace> entry : firstTraceByTask.entrySet()) {
            agentNameByTaskId.put(entry.getKey(), entry.getValue().getAgentName());
        }
        return agentNameByTaskId;
    }

    private AgentSummary agentSummary(List<AgentTask> tasks, double tokenCost) {
        long totalTasks = tasks.size();
        long successCount = tasks.stream()
                .filter(task -> "DONE".equals(task.getStatus()))
                .count();
        long failureCount = tasks.stream()
                .filter(task -> "FAILED".equals(task.getStatus()))
                .count();
        double averageLatencyMs = tasks.stream()
                .map(AgentTask::getLatencyMs)
                .filter(latency -> latency != null)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        double successRate = totalTasks == 0 ? 0.0 : (double) successCount / totalTasks;
        double failureRate = totalTasks == 0 ? 0.0 : (double) failureCount / totalTasks;
        return new AgentSummary(
                totalTasks,
                successCount,
                failureCount,
                successRate,
                failureRate,
                averageLatencyMs,
                tokenCost,
                ragHitRate()
        );
    }

    private double ragHitRate() {
        List<KbQueryLog> logs = kbQueryLogRepository.findAll();
        if (logs.isEmpty()) {
            return 0.0;
        }
        long hits = logs.stream()
                .filter(log -> numberField(log, "retrievalCount") > 0)
                .count();
        return (double) hits / logs.size();
    }

    private long positiveLongOrDefault(Long value, long defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private double positiveDoubleOrDefault(Double value, double defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private boolean inWindow(Instant value, Instant from, Instant to) {
        Instant effectiveValue = value == null ? Instant.EPOCH : value;
        boolean afterFrom = from == null || !effectiveValue.isBefore(from);
        boolean beforeTo = to == null || !effectiveValue.isAfter(to);
        return afterFrom && beforeTo;
    }

    private Map<String, ResourceGenerationTask> resourceTaskByAgentTaskId() {
        return resourceGenerationTaskRepository.findAll().stream()
                .collect(Collectors.toMap(
                        ResourceGenerationTask::getAgentTaskId,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    private List<UserTokenUsageTotals> governanceByUser(
            List<TokenUsageLog> tokenUsageLogs,
            Map<String, AgentTask> taskById
    ) {
        return summarizeByUser(tokenUsageLogs, taskById);
    }

    private List<CourseTokenUsageTotals> governanceByCourse(
            List<TokenUsageLog> tokenUsageLogs,
            Map<String, ResourceGenerationTask> resourceTaskByAgentTaskId
    ) {
        Map<String, TokenAccumulator> byCourse = new LinkedHashMap<>();
        for (TokenUsageLog log : tokenUsageLogs) {
            ResourceGenerationTask resourceTask = resourceTaskByAgentTaskId.get(log.getAgentTaskId());
            String courseId = resourceTask == null ? "UNKNOWN_COURSE" : resourceTask.getGoalId();
            byCourse.computeIfAbsent(courseId, ignored -> new TokenAccumulator()).add(log);
        }
        return byCourse.entrySet().stream()
                .map(entry -> new CourseTokenUsageTotals(
                        entry.getKey(),
                        entry.getValue().promptTokens,
                        entry.getValue().completionTokens,
                        entry.getValue().totalTokens,
                        entry.getValue().estimatedCost
                ))
                .toList();
    }

    private List<AgentTypeTokenUsageTotals> governanceByAgentType(
            List<TokenUsageLog> tokenUsageLogs,
            Map<String, AgentTask> taskById
    ) {
        Map<String, TokenAccumulator> byAgentType = new LinkedHashMap<>();
        for (TokenUsageLog log : tokenUsageLogs) {
            AgentTask task = taskById.get(log.getAgentTaskId());
            String agentType = task == null ? "UNKNOWN_AGENT_TYPE" : task.getTaskType();
            byAgentType.computeIfAbsent(agentType, ignored -> new TokenAccumulator()).add(log);
        }
        return byAgentType.entrySet().stream()
                .map(entry -> new AgentTypeTokenUsageTotals(
                        entry.getKey(),
                        entry.getValue().promptTokens,
                        entry.getValue().completionTokens,
                        entry.getValue().totalTokens,
                        entry.getValue().estimatedCost
                ))
                .toList();
    }

    private BudgetDecision budgetDecision(long usedTokens, BudgetRule rule) {
        long remainingTokens = Math.max(0L, rule.tokenBudget() - usedTokens);
        double usageRatio = rule.tokenBudget() == 0 ? 0.0 : (double) usedTokens / rule.tokenBudget();
        if (usageRatio >= rule.manualConfirmationThreshold()) {
            return new BudgetDecision(
                    rule.tokenBudget(),
                    usedTokens,
                    remainingTokens,
                    usageRatio,
                    "OVER_BUDGET",
                    "REQUIRE_MANUAL_CONFIRMATION",
                    true,
                    "MANUAL_CONFIRMATION_REQUIRED",
                    List.of("MANUAL_CONFIRMATION_THRESHOLD_EXCEEDED")
            );
        }
        if (usageRatio >= rule.degradeThreshold()) {
            return new BudgetDecision(
                    rule.tokenBudget(),
                    usedTokens,
                    remainingTokens,
                    usageRatio,
                    "NEAR_BUDGET",
                    "DOWNGRADE_TO_DETERMINISTIC_OR_CACHED_RESPONSE",
                    false,
                    "DETERMINISTIC_OR_CACHED_RESPONSE",
                    List.of("DEGRADE_THRESHOLD_EXCEEDED")
            );
        }
        return new BudgetDecision(
                rule.tokenBudget(),
                usedTokens,
                remainingTokens,
                usageRatio,
                "WITHIN_BUDGET",
                "ALLOW_MODEL_GATEWAY",
                false,
                "ALLOW_MODEL_GATEWAY",
                List.of()
        );
    }

    private List<HighCostTaskWarning> highCostTaskWarnings(
            List<TokenUsageLog> tokenUsageLogs,
            Map<String, AgentTask> taskById,
            Map<String, ResourceGenerationTask> resourceTaskByAgentTaskId,
            BudgetRule rule
    ) {
        Map<String, TokenAccumulator> byTask = new LinkedHashMap<>();
        for (TokenUsageLog log : tokenUsageLogs) {
            byTask.computeIfAbsent(log.getAgentTaskId(), ignored -> new TokenAccumulator()).add(log);
        }
        return byTask.entrySet().stream()
                .map(entry -> highCostWarning(entry.getKey(), entry.getValue(), taskById, resourceTaskByAgentTaskId, rule))
                .filter(warning -> !warning.reasonCodes().isEmpty())
                .sorted(Comparator
                        .comparing(HighCostTaskWarning::warningLevel)
                        .thenComparing(HighCostTaskWarning::totalTokens, Comparator.reverseOrder())
                        .thenComparing(HighCostTaskWarning::agentTaskId))
                .toList();
    }

    private HighCostTaskWarning highCostWarning(
            String agentTaskId,
            TokenAccumulator accumulator,
            Map<String, AgentTask> taskById,
            Map<String, ResourceGenerationTask> resourceTaskByAgentTaskId,
            BudgetRule rule
    ) {
        List<String> reasonCodes = new ArrayList<>();
        if (accumulator.totalTokens >= rule.highCostTokenThreshold()) {
            reasonCodes.add("HIGH_TOKEN_USAGE");
        }
        if (accumulator.estimatedCost >= rule.highCostUsdThreshold()) {
            reasonCodes.add("HIGH_ESTIMATED_COST");
        }
        AgentTask task = taskById.get(agentTaskId);
        ResourceGenerationTask resourceTask = resourceTaskByAgentTaskId.get(agentTaskId);
        return new HighCostTaskWarning(
                agentTaskId,
                task == null ? null : task.getTraceId(),
                task == null ? "UNKNOWN_USER" : task.getOwnerUserId(),
                resourceTask == null ? "UNKNOWN_COURSE" : resourceTask.getGoalId(),
                task == null ? "UNKNOWN_AGENT_TYPE" : task.getTaskType(),
                accumulator.promptTokens,
                accumulator.completionTokens,
                accumulator.totalTokens,
                accumulator.estimatedCost,
                accumulator.totalTokens >= rule.tokenBudget() ? "CRITICAL" : "WARNING",
                reasonCodes
        );
    }

    private List<AbnormalModelCall> abnormalModelCalls(
            List<ModelCallLog> modelCallLogs,
            Map<String, AgentTask> taskById,
            Map<String, ResourceGenerationTask> resourceTaskByAgentTaskId,
            BudgetRule rule
    ) {
        return modelCallLogs.stream()
                .map(modelCall -> abnormalModelCall(modelCall, taskById, resourceTaskByAgentTaskId, rule))
                .filter(call -> !call.reasonCodes().isEmpty())
                .sorted(Comparator
                        .comparing((AbnormalModelCall call) -> call.reasonCodes().contains("MODEL_CALL_FAILED") ? 0 : 1)
                        .thenComparing(AbnormalModelCall::agentTaskId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private AbnormalModelCall abnormalModelCall(
            ModelCallLog modelCall,
            Map<String, AgentTask> taskById,
            Map<String, ResourceGenerationTask> resourceTaskByAgentTaskId,
            BudgetRule rule
    ) {
        List<String> reasonCodes = new ArrayList<>();
        if ("FAILED".equals(modelCall.getStatus())) {
            reasonCodes.add("MODEL_CALL_FAILED");
        }
        if (modelCall.getLatencyMs() != null && modelCall.getLatencyMs() >= rule.anomalyLatencyMsThreshold()) {
            reasonCodes.add("MODEL_CALL_HIGH_LATENCY");
        }
        if (modelCall.getEstimatedCost() != null && modelCall.getEstimatedCost() >= rule.highCostUsdThreshold()) {
            reasonCodes.add("MODEL_CALL_HIGH_COST");
        }
        AgentTask task = taskById.get(modelCall.getAgentTaskId());
        ResourceGenerationTask resourceTask = resourceTaskByAgentTaskId.get(modelCall.getAgentTaskId());
        return new AbnormalModelCall(
                modelCall.getId(),
                modelCall.getAgentTaskId(),
                modelCall.getTraceId(),
                task == null ? "UNKNOWN_USER" : task.getOwnerUserId(),
                resourceTask == null ? "UNKNOWN_COURSE" : resourceTask.getGoalId(),
                task == null ? "UNKNOWN_AGENT_TYPE" : task.getTaskType(),
                modelCall.getModel(),
                modelCall.getStatus(),
                modelCall.getLatencyMs(),
                modelCall.getEstimatedCost() == null ? 0.0 : modelCall.getEstimatedCost(),
                modelCall.getErrorMessage(),
                instantField(modelCall, "createdAt"),
                reasonCodes
        );
    }

    private TokenBudgetStatus budgetStatus(long totalTokens) {
        long remaining = Math.max(0L, DEFAULT_TOKEN_BUDGET - totalTokens);
        String status = totalTokens >= DEFAULT_TOKEN_BUDGET ? "OVER_BUDGET" : "WITHIN_BUDGET";
        String fallbackStrategy = totalTokens >= DEFAULT_TOKEN_BUDGET
                ? "DOWNGRADE_TO_DETERMINISTIC_OR_CACHED_RESPONSE"
                : "ALLOW_MODEL_GATEWAY";
        return new TokenBudgetStatus(DEFAULT_TOKEN_BUDGET, totalTokens, remaining, status, fallbackStrategy);
    }

    private StudentProgress progress(List<LearningPathNode> nodes) {
        long totalNodes = nodes.size();
        long doneNodes = nodes.stream().filter(node -> "DONE".equals(node.getStatus())).count();
        long activeNodes = nodes.stream().filter(node -> "ACTIVE".equals(node.getStatus()) || "READY".equals(node.getStatus())).count();
        long lockedNodes = nodes.stream().filter(node -> "LOCKED".equals(node.getStatus())).count();
        double completionRate = totalNodes == 0 ? 0.0 : (double) doneNodes / totalNodes;
        return new StudentProgress(totalNodes, doneNodes, activeNodes, lockedNodes, completionRate);
    }

    private List<CurrentMastery> currentMastery(List<LearningPathNode> nodes, List<MasteryRecord> records) {
        Map<String, MasteryRecord> latestRecordByKnowledgePoint = latestRecordByKnowledgePoint(records);
        List<CurrentMastery> currentMastery = new ArrayList<>();
        for (LearningPathNode node : nodes) {
            MasteryRecord record = latestRecordByKnowledgePoint.remove(node.getKnowledgePointId());
            if (record != null) {
                currentMastery.add(new CurrentMastery(
                        record.getKnowledgePointId(),
                        safeDouble(record.getMastery()),
                        record.getSourceType(),
                        record.getSourceId(),
                        record.getReasonSummary()
                ));
            } else {
                currentMastery.add(new CurrentMastery(
                        node.getKnowledgePointId(),
                        safeDouble(node.getMastery()),
                        "PATH_NODE",
                        node.getId(),
                        node.getReasonSummary()
                ));
            }
        }
        latestRecordByKnowledgePoint.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .map(record -> new CurrentMastery(
                        record.getKnowledgePointId(),
                        safeDouble(record.getMastery()),
                        record.getSourceType(),
                        record.getSourceId(),
                        record.getReasonSummary()
                ))
                .forEach(currentMastery::add);
        return currentMastery;
    }

    private Map<String, MasteryRecord> latestRecordByKnowledgePoint(List<MasteryRecord> records) {
        Map<String, MasteryRecord> latest = new LinkedHashMap<>();
        for (MasteryRecord record : records) {
            MasteryRecord current = latest.get(record.getKnowledgePointId());
            if (current == null || instantField(record, "updatedAt").isAfter(instantField(current, "updatedAt"))) {
                latest.put(record.getKnowledgePointId(), record);
            }
        }
        return latest;
    }

    private List<MasteryTrendPoint> masteryTrend(List<MasteryRecord> records, List<WrongQuestion> recentWrongCauses) {
        Map<String, Integer> wrongCausePriority = new LinkedHashMap<>();
        for (WrongQuestion wrongQuestion : recentWrongCauses) {
            wrongCausePriority.putIfAbsent(wrongQuestion.getKnowledgePointId(), wrongCausePriority.size());
        }
        return records.stream()
                .sorted(Comparator
                        .comparing((MasteryRecord record) -> wrongCausePriority.getOrDefault(
                                record.getKnowledgePointId(),
                                Integer.MAX_VALUE
                        ))
                        .thenComparing(record -> instantField(record, "updatedAt"))
                        .thenComparing(MasteryRecord::getId, Comparator.nullsLast(String::compareTo)))
                .map(record -> new MasteryTrendPoint(
                        record.getKnowledgePointId(),
                        safeDouble(record.getMastery()),
                        record.getSourceType(),
                        record.getSourceId(),
                        record.getReasonSummary()
                ))
                .toList();
    }

    private List<WrongQuestion> recentWrongCauses(String learnerId) {
        return recentWrongCauses(learnerId, null);
    }

    private List<WrongQuestion> recentWrongCauses(String learnerId, Set<String> allowedKnowledgePointIds) {
        return wrongQuestionRepository.findAll().stream()
                .filter(wrongQuestion -> learnerId.equals(wrongQuestion.getLearnerId()))
                .filter(wrongQuestion -> allowedKnowledgePointIds == null
                        || allowedKnowledgePointIds.contains(wrongQuestion.getKnowledgePointId()))
                .sorted(Comparator
                        .comparing((WrongQuestion wrongQuestion) -> instantField(wrongQuestion, "updatedAt"))
                        .reversed()
                        .thenComparing(WrongQuestion::getId, Comparator.nullsLast(String::compareTo)))
                .limit(MAX_RECENT_WRONG_CAUSES)
                .toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private RecentWrongCause toRecentWrongCause(WrongQuestion wrongQuestion) {
        return new RecentWrongCause(
                wrongQuestion.getKnowledgePointId(),
                wrongQuestion.getQuestionId(),
                safeDouble(wrongQuestion.getScore()),
                wrongQuestion.getCauseAnalysis(),
                wrongQuestion.getResourcePushStrategy(),
                wrongQuestion.getTraceId()
        );
    }

    private List<RecommendedNextStep> recommendedNextSteps(
            List<LearningPathNode> nodes,
            List<WrongQuestion> recentWrongCauses
    ) {
        List<RecommendedNextStep> steps = new ArrayList<>();
        nodes.stream()
                .filter(node -> !"DONE".equals(node.getStatus()))
                .limit(MAX_NEXT_STEPS)
                .map(node -> new RecommendedNextStep(
                        "PATH_NODE",
                        node.getKnowledgePointId(),
                        node.getTitle(),
                        node.getReasonSummary()
                ))
                .forEach(steps::add);

        if (steps.size() >= MAX_NEXT_STEPS) {
            return steps;
        }
        recentWrongCauses.stream()
                .filter(wrongQuestion -> wrongQuestion.getResourcePushStrategy() != null
                        && !wrongQuestion.getResourcePushStrategy().isBlank())
                .limit(MAX_NEXT_STEPS - steps.size())
                .map(wrongQuestion -> new RecommendedNextStep(
                        "WRONG_CAUSE_REMEDIATION",
                        wrongQuestion.getKnowledgePointId(),
                        wrongQuestion.getResourcePushStrategy(),
                        wrongQuestion.getCauseAnalysis()
                ))
                .forEach(steps::add);
        return steps;
    }

    private List<WeakKnowledgePoint> weakKnowledgePoints(
            List<LearningPathNode> classNodes,
            List<WrongQuestion> classWrongQuestions,
            Map<String, String> knowledgeTitleById
    ) {
        Map<String, List<LearningPathNode>> nodesByKnowledgePoint = classNodes.stream()
                .collect(Collectors.groupingBy(
                        LearningPathNode::getKnowledgePointId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<String, Long> wrongCountByKnowledgePoint = classWrongQuestions.stream()
                .collect(Collectors.groupingBy(
                        WrongQuestion::getKnowledgePointId,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        Map<String, String> topCauseByKnowledgePoint = topCauseByKnowledgePoint(classWrongQuestions);

        return nodesByKnowledgePoint.entrySet().stream()
                .map(entry -> {
                    String knowledgePointId = entry.getKey();
                    List<LearningPathNode> nodes = entry.getValue();
                    double averageMastery = nodes.stream()
                            .map(LearningPathNode::getMastery)
                            .mapToDouble(this::safeDouble)
                            .average()
                            .orElse(0.0);
                    boolean hasIncompleteNode = nodes.stream().anyMatch(node -> !"DONE".equals(node.getStatus()));
                    long affectedLearnerCount = nodes.stream()
                            .map(LearningPathNode::getLearnerId)
                            .distinct()
                            .count();
                    return new WeakKnowledgePoint(
                            knowledgePointId,
                            knowledgeTitleById.getOrDefault(knowledgePointId, knowledgePointId),
                            averageMastery,
                            wrongCountByKnowledgePoint.getOrDefault(knowledgePointId, 0L),
                            affectedLearnerCount,
                            topCauseByKnowledgePoint.get(knowledgePointId)
                    );
                })
                .filter(point -> point.averageMastery() < WEAK_MASTERY_THRESHOLD
                        || classNodes.stream().anyMatch(node -> point.knowledgePointId().equals(node.getKnowledgePointId())
                        && !"DONE".equals(node.getStatus())))
                .sorted(Comparator
                        .comparingDouble(WeakKnowledgePoint::averageMastery)
                        .thenComparing(WeakKnowledgePoint::wrongQuestionCount, Comparator.reverseOrder())
                        .thenComparing(WeakKnowledgePoint::knowledgePointId))
                .toList();
    }

    private void requireTeacherClassAccess(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            Course course
    ) {
        if (currentUserAdmin) {
            return;
        }
        if (currentUserTeacher && course != null && currentUserId.equals(course.getTeacherId())) {
            return;
        }
        throw new ApiException(ErrorCode.FORBIDDEN, "Teacher class analytics access denied");
    }

    private ApiException scopedClassCourseMissing(boolean currentUserAdmin) {
        if (currentUserAdmin) {
            return new ApiException(ErrorCode.NOT_FOUND, "Course not found");
        }
        return new ApiException(ErrorCode.FORBIDDEN, "Teacher class analytics access denied");
    }

    private Set<String> classLearnerIds(String courseId) {
        if (courseAccessService != null) {
            return Set.copyOf(courseAccessService.listActiveLearnerIds(courseId));
        }
        return Set.of();
    }

    private List<WrongCauseDistribution> wrongCauseDistribution(List<WrongQuestion> wrongQuestions) {
        Map<String, WrongCauseAccumulator> distribution = new LinkedHashMap<>();
        for (WrongQuestion wrongQuestion : wrongQuestions) {
            String cause = normalizeCause(wrongQuestion.getCauseAnalysis());
            String key = wrongQuestion.getKnowledgePointId() + "\u0000" + cause;
            distribution.computeIfAbsent(
                    key,
                    ignored -> new WrongCauseAccumulator(wrongQuestion.getKnowledgePointId(), cause)
            ).count++;
        }
        return distribution.values().stream()
                .map(accumulator -> new WrongCauseDistribution(
                        accumulator.knowledgePointId,
                        accumulator.causeAnalysis,
                        accumulator.count
                ))
                .sorted(Comparator
                        .comparingLong(WrongCauseDistribution::count)
                        .reversed()
                        .thenComparing(WrongCauseDistribution::knowledgePointId)
                        .thenComparing(WrongCauseDistribution::causeAnalysis))
                .toList();
    }

    private ResourceCompletion resourceCompletion(List<ResourceGenerationTask> tasks) {
        long totalTasks = tasks.size();
        long doneTasks = tasks.stream().filter(task -> "DONE".equals(task.getStatus())).count();
        long waitingReviewTasks = tasks.stream()
                .filter(task -> "WAITING_REVIEW".equals(task.getStatus()) || "PENDING_CRITIC".equals(task.getReviewStatus()))
                .count();
        long failedTasks = tasks.stream().filter(task -> "FAILED".equals(task.getStatus())).count();
        double averageProgress = tasks.stream()
                .map(ResourceGenerationTask::getProgressPercent)
                .mapToInt(progress -> progress == null ? 0 : progress)
                .average()
                .orElse(0.0);
        double completionRate = totalTasks == 0 ? 0.0 : (double) doneTasks / totalTasks;
        return new ResourceCompletion(totalTasks, doneTasks, waitingReviewTasks, failedTasks, averageProgress, completionRate);
    }

    private List<PendingReviewSummary> pendingReviews(List<ResourceGenerationTask> tasks) {
        Set<String> taskIds = tasks.stream()
                .map(ResourceGenerationTask::getId)
                .collect(Collectors.toSet());
        if (taskIds.isEmpty()) {
            return List.of();
        }
        Map<String, LearningResource> resourceById = learningResourceRepository.findAll().stream()
                .filter(resource -> taskIds.contains(resource.getGenerationTaskId()))
                .collect(Collectors.toMap(
                        LearningResource::getId,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
        return resourceReviewRepository.findAll().stream()
                .filter(review -> taskIds.contains(review.getGenerationTaskId()))
                .filter(review -> "PENDING_CRITIC".equals(review.getStatus()) || "REVISION_REQUESTED".equals(review.getStatus()))
                .sorted(Comparator
                        .comparing(ResourceReview::getGenerationTaskId)
                        .thenComparing(ResourceReview::getId, Comparator.nullsLast(String::compareTo)))
                .map(review -> {
                    LearningResource resource = resourceById.get(review.getResourceId());
                    return new PendingReviewSummary(
                            review.getId(),
                            review.getResourceId(),
                            review.getGenerationTaskId(),
                            review.getStatus(),
                            review.getReviewerType(),
                            resource == null ? null : resource.getTitle(),
                            resource == null ? null : resource.getResourceType()
                    );
                })
                .toList();
    }

    private Map<String, String> topCauseByKnowledgePoint(List<WrongQuestion> wrongQuestions) {
        Map<String, Map<String, Long>> causeCountsByKnowledgePoint = new LinkedHashMap<>();
        for (WrongQuestion wrongQuestion : wrongQuestions) {
            String cause = normalizeCause(wrongQuestion.getCauseAnalysis());
            causeCountsByKnowledgePoint
                    .computeIfAbsent(wrongQuestion.getKnowledgePointId(), ignored -> new LinkedHashMap<>())
                    .merge(cause, 1L, Long::sum);
        }
        Map<String, String> topCauseByKnowledgePoint = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Long>> entry : causeCountsByKnowledgePoint.entrySet()) {
            entry.getValue().entrySet().stream()
                    .sorted(Comparator
                            .<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
                            .reversed()
                            .thenComparing(Map.Entry::getKey))
                    .findFirst()
                    .ifPresent(cause -> topCauseByKnowledgePoint.put(entry.getKey(), cause.getKey()));
        }
        return topCauseByKnowledgePoint;
    }

    private String normalizeCause(String causeAnalysis) {
        return causeAnalysis == null || causeAnalysis.isBlank() ? "UNKNOWN_CAUSE" : causeAnalysis;
    }

    private int safeSequence(AgentTrace trace) {
        return trace.getSequenceNo() == null ? Integer.MAX_VALUE : trace.getSequenceNo();
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private long numberField(Object source, String fieldName) {
        Object value = new DirectFieldAccessor(source).getPropertyValue(fieldName);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Instant instantField(Object source, String fieldName) {
        Object value = new DirectFieldAccessor(source).getPropertyValue(fieldName);
        return value instanceof Instant instant ? instant : Instant.EPOCH;
    }

    private static final class TokenAccumulator {
        private long promptTokens;
        private long completionTokens;
        private long totalTokens;
        private double estimatedCost;

        private void add(TokenUsageLog log) {
            this.promptTokens += log.getPromptTokens() == null ? 0 : log.getPromptTokens();
            this.completionTokens += log.getCompletionTokens() == null ? 0 : log.getCompletionTokens();
            this.totalTokens += log.getTotalTokens() == null ? 0 : log.getTotalTokens();
            this.estimatedCost += log.getEstimatedCost() == null ? 0.0 : log.getEstimatedCost();
        }
    }

    private record BudgetRule(
            long tokenBudget,
            double degradeThreshold,
            double manualConfirmationThreshold,
            long highCostTokenThreshold,
            double highCostUsdThreshold,
            long anomalyLatencyMsThreshold
    ) {
    }

    private static final class WrongCauseAccumulator {
        private final String knowledgePointId;
        private final String causeAnalysis;
        private long count;

        private WrongCauseAccumulator(String knowledgePointId, String causeAnalysis) {
            this.knowledgePointId = knowledgePointId;
            this.causeAnalysis = causeAnalysis;
        }
    }
}
