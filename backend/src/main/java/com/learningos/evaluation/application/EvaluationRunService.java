package com.learningos.evaluation.application;

import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.evaluation.domain.EvaluationRun;
import com.learningos.evaluation.domain.EvaluationRunMetric;
import com.learningos.evaluation.domain.EvaluationSet;
import com.learningos.evaluation.dto.EvaluationMetricAggregateResponse;
import com.learningos.evaluation.dto.EvaluationMetricRequest;
import com.learningos.evaluation.dto.EvaluationMetricResponse;
import com.learningos.evaluation.dto.EvaluationRunRecordRequest;
import com.learningos.evaluation.dto.EvaluationRunResponse;
import com.learningos.evaluation.dto.PromptVersionComparisonRowResponse;
import com.learningos.evaluation.dto.PromptVersionQualityComparisonResponse;
import com.learningos.evaluation.repository.EvaluationRunMetricRepository;
import com.learningos.evaluation.repository.EvaluationRunRepository;
import com.learningos.evaluation.repository.EvaluationSetRepository;
import com.learningos.knowledge.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EvaluationRunService {

    private static final String STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String STATUS_FAILED = "FAILED";
    private static final Set<String> ALLOWED_STATUSES = Set.of(STATUS_SUCCEEDED, STATUS_FAILED);
    private static final Set<String> ALLOWED_METRICS = Set.of(
            "recallAtK",
            "citationAccuracy",
            "groundedness",
            "noSourceRefusalRate",
            "meanAbsoluteError",
            "agreementRate",
            "wrongCauseAgreementRate",
            "qualityScore",
            "citationCoverage",
            "reviewPassRate"
    );
    private static final Set<String> LOWER_IS_BETTER_METRICS = Set.of("meanAbsoluteError");

    private final EvaluationSetRepository evaluationSetRepository;
    private final EvaluationRunRepository evaluationRunRepository;
    private final EvaluationRunMetricRepository evaluationRunMetricRepository;
    private final CourseRepository courseRepository;

    public EvaluationRunService(
            EvaluationSetRepository evaluationSetRepository,
            EvaluationRunRepository evaluationRunRepository,
            EvaluationRunMetricRepository evaluationRunMetricRepository,
            CourseRepository courseRepository
    ) {
        this.evaluationSetRepository = evaluationSetRepository;
        this.evaluationRunRepository = evaluationRunRepository;
        this.evaluationRunMetricRepository = evaluationRunMetricRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public EvaluationRunResponse record(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            EvaluationRunRecordRequest request
    ) {
        EvaluationSet evaluationSet = loadReadableEvaluationSet(
                currentUserId,
                currentUserAdmin,
                currentUserTeacher,
                request.evaluationSetId()
        );
        String promptCode = requiredTrimmed(request.promptCode(), "Prompt code is required");
        assertPromptCodeMatchesSet(evaluationSet, promptCode);
        String promptVersion = requiredTrimmed(request.promptVersion(), "Prompt version is required");
        String status = normalizeStatus(request.status());
        int sampleCount = normalizeSampleCount(request.sampleCount());
        List<EvaluationMetricRequest> requestedMetrics = request.metrics();
        if (STATUS_SUCCEEDED.equals(status) && sampleCount <= 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Succeeded evaluation run sample count must be positive");
        }
        if (STATUS_SUCCEEDED.equals(status) && requestedMetrics.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Succeeded evaluation run requires metrics");
        }
        assertUniqueMetricNames(requestedMetrics);

        EvaluationRun run = new EvaluationRun();
        run.setEvaluationSetId(evaluationSet.getId());
        run.setSetType(evaluationSet.getType());
        run.setPromptCode(promptCode);
        run.setPromptVersion(promptVersion);
        run.setModelName(blankToNull(request.model()));
        run.setStatus(status);
        run.setSampleCount(sampleCount);
        run.setCreatedBy(currentUserId);
        run.setTraceId(blankToNull(request.traceId()));
        run.setFinishedAt(Instant.now());
        EvaluationRun savedRun = evaluationRunRepository.save(run);

        List<EvaluationRunMetric> metrics = requestedMetrics.stream()
                .map(metric -> toMetricEntity(savedRun.getId(), sampleCount, metric))
                .toList();
        evaluationRunMetricRepository.saveAll(metrics);

        return toRunResponse(savedRun, metrics);
    }

    private void assertUniqueMetricNames(List<EvaluationMetricRequest> metrics) {
        Set<String> names = new LinkedHashSet<>();
        for (EvaluationMetricRequest metric : metrics) {
            String metricName = requiredTrimmed(metric.metricName(), "Metric name is required");
            if (!names.add(metricName)) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Duplicate evaluation metric name is not allowed");
            }
        }
    }

    @Transactional(readOnly = true)
    public PromptVersionQualityComparisonResponse compare(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String evaluationSetId,
            String promptCode,
            List<String> promptVersions,
            String baselinePromptVersion
    ) {
        EvaluationSet evaluationSet = loadReadableEvaluationSet(
                currentUserId,
                currentUserAdmin,
                currentUserTeacher,
                evaluationSetId
        );
        String normalizedPromptCode = requiredTrimmed(promptCode, "Prompt code is required");
        assertPromptCodeMatchesSet(evaluationSet, normalizedPromptCode);
        List<String> normalizedVersions = normalizedVersions(promptVersions);
        if (normalizedVersions.size() < 2) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "At least two prompt versions are required");
        }

        List<EvaluationRun> runs = evaluationRunRepository
                .findByEvaluationSetIdAndPromptCodeAndPromptVersionInAndStatusOrderByCreatedAtAsc(
                        evaluationSet.getId(),
                        normalizedPromptCode,
                        normalizedVersions,
                        STATUS_SUCCEEDED
                );
        Map<String, List<EvaluationRun>> runsByVersion = runs.stream()
                .collect(Collectors.groupingBy(EvaluationRun::getPromptVersion, LinkedHashMap::new, Collectors.toList()));
        List<String> comparableVersions = normalizedVersions.stream()
                .filter(version -> runsByVersion.containsKey(version) && !runsByVersion.get(version).isEmpty())
                .toList();
        if (comparableVersions.size() < 2) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "At least two prompt versions must have succeeded evaluation runs");
        }

        String baseline = baselinePromptVersion == null || baselinePromptVersion.isBlank()
                ? comparableVersions.getFirst()
                : baselinePromptVersion.trim();
        if (!runsByVersion.containsKey(baseline) || runsByVersion.get(baseline).isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Baseline prompt version must have succeeded evaluation runs");
        }

        Map<String, List<EvaluationRunMetric>> metricsByRun = metricsByRun(runs);
        Map<String, Map<String, EvaluationMetricAggregateResponse>> aggregatesByVersion = new LinkedHashMap<>();
        for (String version : comparableVersions) {
            aggregatesByVersion.put(version, aggregateMetrics(runsByVersion.get(version), metricsByRun));
        }
        Map<String, EvaluationMetricAggregateResponse> baselineMetrics = aggregatesByVersion.get(baseline);

        List<PromptVersionComparisonRowResponse> rows = new ArrayList<>();
        for (String version : comparableVersions) {
            Map<String, EvaluationMetricAggregateResponse> metrics = aggregatesByVersion.get(version);
            rows.add(new PromptVersionComparisonRowResponse(
                    version,
                    runsByVersion.get(version).size(),
                    runsByVersion.get(version).stream().mapToInt(EvaluationRun::getSampleCount).sum(),
                    metrics,
                    deltas(metrics, baselineMetrics)
            ));
        }

        return new PromptVersionQualityComparisonResponse(
                evaluationSet.getId(),
                normalizedPromptCode,
                baseline,
                rows,
                winnerByMetric(aggregatesByVersion)
        );
    }

    private EvaluationRunMetric toMetricEntity(String runId, int runSampleCount, EvaluationMetricRequest request) {
        String metricName = requiredTrimmed(request.metricName(), "Metric name is required");
        if (!ALLOWED_METRICS.contains(metricName)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Evaluation metric is not allowed");
        }
        Double metricValue = request.metricValue();
        if (metricValue == null || metricValue.isNaN() || metricValue.isInfinite()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Metric value must be finite");
        }
        int metricSampleCount = request.sampleCount() == null ? runSampleCount : request.sampleCount();
        if (metricSampleCount <= 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Metric sample count must be positive");
        }
        EvaluationRunMetric metric = new EvaluationRunMetric();
        metric.setRunId(runId);
        metric.setMetricName(metricName);
        metric.setMetricValue(metricValue);
        metric.setMetricUnit(blankToNull(request.metricUnit()) == null ? "score" : request.metricUnit().trim());
        metric.setSampleCount(metricSampleCount);
        return metric;
    }

    private Map<String, List<EvaluationRunMetric>> metricsByRun(List<EvaluationRun> runs) {
        List<String> runIds = runs.stream().map(EvaluationRun::getId).toList();
        if (runIds.isEmpty()) {
            return Map.of();
        }
        return evaluationRunMetricRepository.findByRunIdIn(runIds).stream()
                .collect(Collectors.groupingBy(EvaluationRunMetric::getRunId));
    }

    private Map<String, EvaluationMetricAggregateResponse> aggregateMetrics(
            List<EvaluationRun> runs,
            Map<String, List<EvaluationRunMetric>> metricsByRun
    ) {
        Map<String, List<EvaluationRunMetric>> byMetric = new LinkedHashMap<>();
        runs.stream()
                .map(run -> metricsByRun.getOrDefault(run.getId(), List.of()))
                .flatMap(Collection::stream)
                .forEach(metric -> byMetric.computeIfAbsent(metric.getMetricName(), ignored -> new ArrayList<>()).add(metric));

        Map<String, EvaluationMetricAggregateResponse> result = new LinkedHashMap<>();
        byMetric.forEach((name, metrics) -> {
            int sampleCount = metrics.stream().mapToInt(EvaluationRunMetric::getSampleCount).sum();
            double weightedTotal = metrics.stream()
                    .mapToDouble(metric -> metric.getMetricValue() * metric.getSampleCount())
                    .sum();
            double average = sampleCount == 0 ? 0.0 : weightedTotal / sampleCount;
            long runCount = metrics.stream().map(EvaluationRunMetric::getRunId).distinct().count();
            result.put(name, new EvaluationMetricAggregateResponse(round(average), sampleCount, Math.toIntExact(runCount)));
        });
        return result;
    }

    private Map<String, Double> deltas(
            Map<String, EvaluationMetricAggregateResponse> metrics,
            Map<String, EvaluationMetricAggregateResponse> baselineMetrics
    ) {
        Map<String, Double> result = new LinkedHashMap<>();
        metrics.forEach((name, aggregate) -> {
            EvaluationMetricAggregateResponse baseline = baselineMetrics.get(name);
            if (baseline != null) {
                result.put(name, round(aggregate.average() - baseline.average()));
            }
        });
        return result;
    }

    private Map<String, String> winnerByMetric(Map<String, Map<String, EvaluationMetricAggregateResponse>> aggregatesByVersion) {
        Map<String, String> winner = new LinkedHashMap<>();
        aggregatesByVersion.forEach((version, metrics) -> metrics.forEach((metricName, aggregate) -> {
            String currentWinner = winner.get(metricName);
            if (currentWinner == null) {
                winner.put(metricName, version);
                return;
            }
            double currentWinnerValue = aggregatesByVersion.get(currentWinner).get(metricName).average();
            if (isBetter(metricName, aggregate.average(), currentWinnerValue)) {
                winner.put(metricName, version);
            }
        }));
        return winner;
    }

    private boolean isBetter(String metricName, double candidate, double incumbent) {
        if (LOWER_IS_BETTER_METRICS.contains(metricName)) {
            return candidate < incumbent;
        }
        return candidate > incumbent;
    }

    private EvaluationRunResponse toRunResponse(EvaluationRun run, List<EvaluationRunMetric> metrics) {
        return new EvaluationRunResponse(
                run.getId(),
                run.getEvaluationSetId(),
                run.getSetType(),
                run.getPromptCode(),
                run.getPromptVersion(),
                run.getModelName(),
                run.getStatus(),
                run.getSampleCount(),
                run.getTraceId(),
                metrics.stream()
                        .map(metric -> new EvaluationMetricResponse(
                                metric.getMetricName(),
                                metric.getMetricValue(),
                                metric.getMetricUnit(),
                                metric.getSampleCount()
                        ))
                        .toList(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getCreatedAt()
        );
    }

    private EvaluationSet loadReadableEvaluationSet(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String evaluationSetId
    ) {
        String setId = requiredTrimmed(evaluationSetId, "Evaluation set id is required");
        EvaluationSet evaluationSet = evaluationSetRepository.findById(setId)
                .filter(set -> set.getDeletedAt() == null)
                .orElseThrow(() -> evaluationSetNotVisible(currentUserAdmin));
        if (!canRead(currentUserId, currentUserAdmin, currentUserTeacher, evaluationSet)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Evaluation set access denied");
        }
        return evaluationSet;
    }

    private boolean canRead(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            EvaluationSet set
    ) {
        if (currentUserAdmin) {
            return true;
        }
        return currentUserTeacher
                && (currentUserId.equals(set.getCreatedBy()) || isCourseTeacher(currentUserId, set.getCourseId()));
    }

    private ApiException evaluationSetNotVisible(boolean currentUserAdmin) {
        if (currentUserAdmin) {
            return new ApiException(ErrorCode.NOT_FOUND, "Evaluation set not found");
        }
        return new ApiException(ErrorCode.FORBIDDEN, "Evaluation set access denied");
    }

    private boolean isCourseTeacher(String userId, String courseId) {
        if (userId == null || courseId == null || courseId.isBlank()) {
            return false;
        }
        return courseRepository.findById(courseId)
                .map(course -> userId.equals(course.getTeacherId()))
                .orElse(false);
    }

    private void assertPromptCodeMatchesSet(EvaluationSet evaluationSet, String promptCode) {
        String setPromptCode = evaluationSet.getPromptCode();
        if (setPromptCode != null && !setPromptCode.isBlank() && !setPromptCode.equals(promptCode)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Prompt code does not match evaluation set");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status == null || status.isBlank()
                ? STATUS_SUCCEEDED
                : status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Evaluation run status is invalid");
        }
        return normalized;
    }

    private int normalizeSampleCount(Integer sampleCount) {
        if (sampleCount == null || sampleCount < 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Sample count cannot be negative");
        }
        return sampleCount;
    }

    private List<String> normalizedVersions(List<String> versions) {
        if (versions == null) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        versions.stream()
                .filter(version -> version != null && !version.isBlank())
                .map(String::trim)
                .forEach(normalized::add);
        return List.copyOf(normalized);
    }

    private String requiredTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private double round(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }
}
